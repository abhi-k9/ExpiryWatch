package io.github.abhik9.expirywatch.core.notifications

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.common.navigation.DeepLink
import io.github.abhik9.expirywatch.core.domain.usecase.ExpirySummary
import io.github.abhik9.expirywatch.core.domain.usecase.ItemWithExpiry
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.ui.ExpiryText
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts the daily "items need attention" notification, with a button to snooze it all, and one
 * notification for each of the first few expired items, with buttons to finish or snooze it.
 */
@Singleton
class ExpiryNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val log: EventLog,
) {
    /** Creates the reminders channel, so it can be set up in the system settings before the first reminder. */
    fun createChannel() = with(context) { ensureChannel(NotificationManagerCompat.from(this)) }

    /**
     * Posts a summary of expired and soon-to-expire items, or removes it when there are none. Its
     * "Snooze all" button snoozes the items about to expire too when [snoozeAllIncludesExpiringSoon].
     */
    fun showExpiryReminder(summary: ExpirySummary, snoozeAllIncludesExpiringSoon: Boolean) =
        post(summary, snoozeAllIncludesExpiringSoon, update = false)

    /**
     * Brings the reminder up to date after the user acted on it: quietly, and without bringing
     * back notifications they dismissed.
     */
    fun updateExpiryReminder(summary: ExpirySummary, snoozeAllIncludesExpiringSoon: Boolean) =
        post(summary, snoozeAllIncludesExpiringSoon, update = true)

    /** Removes the notification about the item with [itemId]. */
    fun cancelItemReminder(itemId: Long) = NotificationManagerCompat.from(context).cancel(ITEM_TAG, itemId.toInt())

    private fun post(summary: ExpirySummary, snoozeAllIncludesExpiringSoon: Boolean, update: Boolean) {
        val notificationManager = NotificationManagerCompat.from(context)
        val needingAttention = summary.expired + summary.expiringSoon
        val itemsToShow = summary.expired.take(MAX_ITEM_NOTIFICATIONS)
        // Items finished, snoozed or no longer expired since lose their notification.
        val shown = notificationManager.activeNotifications
        shown.filter { it.tag == ITEM_TAG && itemsToShow.none { entry -> entry.item.id.toInt() == it.id } }
            .forEach { notificationManager.cancel(ITEM_TAG, it.id) }
        if (needingAttention.isEmpty()) {
            notificationManager.cancel(REMINDER_NOTIFICATION_ID)
            log.record { "reminder notification: removed, nothing needs attention" }
            return
        }
        if (update && shown.none { it.tag == null && it.id == REMINDER_NOTIFICATION_ID }) return
        // ContextCompat treats POST_NOTIFICATIONS as granted before Android 13 when notifications
        // are enabled for the app.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            log.record { "reminder notification: not shown, notifications are off" }
            return
        }
        context.ensureChannel(notificationManager)

        notificationManager.notify(
            REMINDER_NOTIFICATION_ID,
            context.summaryNotification(summary, snoozeAllIncludesExpiringSoon, silent = update),
        )
        if (update) {
            log.record { "reminder notification: updated for ${needingAttention.size} items" }
            return
        }
        log.record { "reminder notification: shown for ${needingAttention.size} items" }
        itemsToShow.forEach { notificationManager.notify(ITEM_TAG, it.item.id.toInt(), context.itemNotification(it)) }
    }

    /** The summary of what needs attention, opening the list filtered to it. */
    private fun Context.summaryNotification(
        summary: ExpirySummary,
        snoozeAllIncludesExpiringSoon: Boolean,
        silent: Boolean,
    ): Notification {
        val needingAttention = summary.expired + summary.expiringSoon
        val title = resources.getQuantityString(
            R.plurals.core_notifications_title,
            needingAttention.size,
            needingAttention.size,
        )
        val statusLine = listOfNotNull(
            summary.expired.size.takeIf { it > 0 }?.let {
                resources.getQuantityString(R.plurals.core_notifications_expired_count, it, it)
            },
            summary.expiringSoon.size.takeIf { it > 0 }?.let {
                resources.getQuantityString(R.plurals.core_notifications_expiring_soon_count, it, it)
            },
        ).joinToString(separator = " · ")
        val filter = when {
            summary.expiringSoon.isEmpty() -> ExpiryStatus.EXPIRED
            summary.expired.isEmpty() -> ExpiryStatus.EXPIRING_SOON
            else -> null
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.core_notifications_ic_stat_expiry)
            .setContentTitle(title)
            .setContentText(statusLine)
            .setStyle(inboxStyle(title, needingAttention))
            .setNumber(needingAttention.size)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(deepLinkPendingIntent(DeepLink.Items(status = filter)))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(silent)
        // Nothing to snooze when only expired items are snoozed and none are.
        if (summary.toSnoozeAll(snoozeAllIncludesExpiringSoon).isNotEmpty()) {
            val label = if (snoozeAllIncludesExpiringSoon) {
                R.string.core_notifications_snooze_all
            } else {
                R.string.core_notifications_snooze_expired
            }
            builder.addAction(
                R.drawable.core_notifications_ic_stat_expiry,
                getString(label),
                actionPendingIntent(ReminderActions.ACTION_SNOOZE_ALL, itemId = null),
            )
        }
        return builder.build()
    }

    /** A line for each of the first [MAX_LINES] items, and how many more there are. */
    private fun Context.inboxStyle(title: String, items: List<ItemWithExpiry>): NotificationCompat.InboxStyle {
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        items.take(MAX_LINES).forEach {
            style.addLine(
                getString(
                    R.string.core_notifications_item_line,
                    it.item.name,
                    ExpiryText.sentence(resources, it.daysUntilExpiry),
                ),
            )
        }
        if (items.size > MAX_LINES) {
            val more = items.size - MAX_LINES
            style.setSummaryText(resources.getQuantityString(R.plurals.core_notifications_more, more, more))
        }
        return style
    }

    private fun Context.itemNotification(entry: ItemWithExpiry): Notification {
        val id = entry.item.id
        val action = { titleRes: Int, action: String ->
            NotificationCompat.Action(
                R.drawable.core_notifications_ic_stat_expiry,
                getString(titleRes),
                actionPendingIntent(action, id),
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.core_notifications_ic_stat_expiry)
            .setContentTitle(entry.item.name)
            .setContentText(ExpiryText.sentence(resources, entry.daysUntilExpiry))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(deepLinkPendingIntent(DeepLink.EditItem(id)))
            .addAction(action(R.string.core_notifications_used_up, ReminderActions.ACTION_USED_UP))
            .addAction(action(R.string.core_notifications_thrown_away, ReminderActions.ACTION_THROWN_AWAY))
            .addAction(action(R.string.core_notifications_snooze_item, ReminderActions.ACTION_SNOOZE))
            .setAutoCancel(true)
            // Shown along with the summary, which alerts for all of them.
            .setSilent(true)
            .build()
    }

    /** A broadcast to [ReminderActionReceiver]; the item's id goes in the data, so each item gets its own. */
    private fun Context.actionPendingIntent(action: String, itemId: Long?): PendingIntent {
        val intent = Intent(this, ReminderActionReceiver::class.java)
            .setAction(action)
            .setData(ReminderActions.dataUri(itemId).toUri())
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun Context.ensureChannel(notificationManager: NotificationManagerCompat) {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(getString(R.string.core_notifications_channel_name))
                .setDescription(getString(R.string.core_notifications_channel_description))
                .build(),
        )
    }

    private fun Context.deepLinkPendingIntent(deepLink: DeepLink): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, deepLink.toUri().toUri()).setPackage(packageName)
        return PendingIntent.getActivity(
            this,
            REMINDER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        internal const val CHANNEL_ID = "expiry_reminders"
        private const val REMINDER_NOTIFICATION_ID = 1
        private const val MAX_LINES = 5
        private const val ITEM_TAG = "item"

        /** At most this many expired items get a notification of their own, the rest only the summary. */
        private const val MAX_ITEM_NOTIFICATIONS = 3
    }
}
