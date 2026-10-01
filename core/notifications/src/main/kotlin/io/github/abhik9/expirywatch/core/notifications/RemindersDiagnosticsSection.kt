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

/** Whether reminders can be shown, and when the daily check is next due. */
internal class RemindersDiagnosticsSection @Inject constructor(
    @ApplicationContext private val context: Context,
) : DiagnosticsSection {
    override val title = "Reminders"

    override suspend fun describe(): List<String> = buildList {
        val notifications = NotificationManagerCompat.from(context)
        add("Notifications enabled: ${notifications.areNotificationsEnabled()}")
        val channel = notifications.getNotificationChannelCompat(ExpiryNotifier.CHANNEL_ID)
        add("Reminder channel importance: ${channel?.importance ?: "not created yet"}")

        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(WorkManagerReminderScheduler.WORK_NAME)
            .first()
        if (work.isEmpty()) add("Daily check: not scheduled")
        work.forEach { add("Daily check: ${it.describe()}") }
    }

    private fun WorkInfo.describe(): String {
        val time = tags.firstOrNull { it.startsWith(WorkManagerReminderScheduler.TAG_PREFIX) }
            ?.removePrefix(WorkManagerReminderScheduler.TAG_PREFIX)
        val nextRun = nextScheduleTimeMillis.takeIf { it != Long.MAX_VALUE }?.let {
            OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)
        }
        return "$state, at $time, next run $nextRun, attempts $runAttemptCount"
    }
}
