package io.github.abhik9.expirywatch.core.domain.repository

import java.time.LocalTime

/** Schedules the daily check that notifies the user about items that are about to expire. */
interface ReminderScheduler {
    /**
     * Runs the check every day at [time]. Scheduling the time that is already scheduled keeps the
     * existing schedule, so calling this on every app start doesn't delay a pending reminder.
     *
     * With [exact], the check also runs at exactly [time] when the system lets the app set exact
     * alarms; otherwise the system may run it later to save battery.
     */
    suspend fun scheduleDaily(time: LocalTime, exact: Boolean)

    suspend fun cancel()
}
