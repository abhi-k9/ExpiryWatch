package io.github.abhik9.expirywatch.core.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.expirywatch.core.domain.repository.ReminderHealthMonitor
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Whether reminders can be shown, what keeps them from arriving, when the daily check is next due,
 * and when one was last shown.
 */
internal class RemindersDiagnosticsSection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthMonitor: ReminderHealthMonitor,
) : DiagnosticsSection {
    override val title = "Reminders"

    override suspend fun describe(): List<String> = buildList {
        val health = healthMonitor.current()
        add("Notifications enabled: ${health.notificationsAllowed}")
        val channel = NotificationManagerCompat.from(context).getNotificationChannelCompat(ExpiryNotifier.CHANNEL_ID)
        add("Reminder channel importance: ${channel?.importance ?: "not created yet"}")
        add("Exact alarms allowed: ${health.exactAlarmsAllowed}")
        add("Background use restricted: ${health.backgroundRestricted}")

        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(AndroidReminderScheduler.WORK_NAME)
            .first()
        if (work.isEmpty()) add("Daily check: not scheduled")
        work.forEach { add("Daily check: ${it.describe()}") }
        add("Last reminder shown was due: ${health.lastShown?.let(::localTime) ?: "none yet"}")
    }

    private fun WorkInfo.describe(): String {
        val nextRun = nextScheduleTimeMillis.takeIf { it != Long.MAX_VALUE }
            ?.let { localTime(Instant.ofEpochMilli(it)) }
        return "$state, next run $nextRun, attempts $runAttemptCount"
    }

    private fun localTime(instant: Instant): OffsetDateTime =
        OffsetDateTime.ofInstant(instant, ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS)
}
