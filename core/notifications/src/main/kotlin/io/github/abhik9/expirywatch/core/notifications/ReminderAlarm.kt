package io.github.abhik9.expirywatch.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * An exact alarm that runs the reminder check at the reminder time, even when the device is idle.
 * Since Android 12, the app may only set one when the user allows it in the system settings,
 * under "Alarms & reminders".
 */
internal class ReminderAlarm @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager get() = checkNotNull(context.getSystemService())

    /** Whether the app may set exact alarms: always before Android 12, then only if the user allows it. */
    fun isAllowed(): Boolean = AlarmManagerCompat.canScheduleExactAlarms(alarmManager)

    /** Sets the alarm to go off at [time], replacing an earlier one. Returns false if exact alarms aren't allowed. */
    fun set(time: ZonedDateTime): Boolean {
        if (!isAllowed()) return false
        return try {
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                time.toInstant().toEpochMilli(),
                checkNotNull(pendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)),
            )
            true
        } catch (e: SecurityException) {
            // The user took the permission back in the meantime.
            false
        }
    }

    fun cancel() {
        val alarm = pendingIntent(PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(alarm)
        alarm.cancel()
    }

    private fun pendingIntent(flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ReminderAlarmReceiver::class.java),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 1
    }
}
