package io.github.abhik9.expirywatch.core.domain.repository

import java.time.LocalTime

/** Schedules the daily check that notifies the user about items that are about to expire. */
interface ReminderScheduler {
    /**
     * Runs the check every day at [time]. Scheduling the time that is already scheduled keeps the
     * existing schedule, so calling this on every app start doesn't delay a pending reminder.
     */
    suspend fun scheduleDaily(time: LocalTime)

    suspend fun cancel()
}
