package io.github.abhik9.expirywatch.core.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.common.di.ApplicationScope
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.usecase.SyncReminderScheduleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Runs the reminder check when the exact reminder alarm goes off. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runAsync(context) { reminderCheck()("the exact alarm") }
    }
}

/**
 * Sets the exact reminder alarm again when the system clears it or it no longer matches the clock:
 * after a restart or an app update, when the time or time zone changes, and when the user allows
 * the app to set alarms. The daily work needs no help; WorkManager handles these itself.
 */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        runAsync(context) {
            log().record { "reminders: rescheduling after ${intent.action}" }
            syncReminderSchedule().once()
        }
    }

    private companion object {
        // Only compared with, so the Android 12 action is fine on older versions.
        @SuppressLint("InlinedApi")
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReminderReceiverEntryPoint {
    @ApplicationScope
    fun applicationScope(): CoroutineScope

    fun reminderCheck(): ReminderCheck

    fun syncReminderSchedule(): SyncReminderScheduleUseCase

    fun log(): EventLog
}

/**
 * Runs [block] in the background and keeps the receiver, and with it the process and the alarm's
 * wake lock, alive until it's done. Failures are recorded by the application scope.
 */
private fun BroadcastReceiver.runAsync(context: Context, block: suspend ReminderReceiverEntryPoint.() -> Unit) {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        ReminderReceiverEntryPoint::class.java,
    )
    val pendingResult = goAsync()
    entryPoint.applicationScope().launch {
        try {
            entryPoint.block()
        } finally {
            pendingResult.finish()
        }
    }
}
