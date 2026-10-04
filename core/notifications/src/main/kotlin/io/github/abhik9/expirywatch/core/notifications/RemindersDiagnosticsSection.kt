package io.github.abhik9.expirywatch.core.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Whether reminders can be shown, when the daily check is next due, and when one was last shown. */
internal class RemindersDiagnosticsSection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarm: ReminderAlarm,
    private val history: ReminderHistory,
) : DiagnosticsSection {
    override val title = "Reminders"

    override suspend fun describe(): List<String> = buildList {
        val notifications = NotificationManagerCompat.from(context)
        add("Notifications enabled: ${notifications.areNotificationsEnabled()}")
        val channel = notifications.getNotificationChannelCompat(ExpiryNotifier.CHANNEL_ID)
        add("Reminder channel importance: ${channel?.importance ?: "not created yet"}")
        add("Exact alarms allowed: ${alarm.isAllowed()}")

        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(AndroidReminderScheduler.WORK_NAME)
            .first()
        if (work.isEmpty()) add("Daily check: not scheduled")
        work.forEach { add("Daily check: ${it.describe()}") }
        add("Last reminder shown was due: ${history.lastShown?.let(::localTime) ?: "none yet"}")
    }

    private fun WorkInfo.describe(): String {
        val nextRun = nextScheduleTimeMillis.takeIf { it != Long.MAX_VALUE }?.let {
            localTime(
                Instant.ofEpochMilli(it),
            )
        }
        return "$state, next run $nextRun, attempts $runAttemptCount"
    }

    private fun localTime(instant: Instant): OffsetDateTime =
        OffsetDateTime.ofInstant(instant, ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)
}
