package io.github.abhik9.expirywatch.core.notifications

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ReminderTimingTest {
    private val zone = ZoneId.of("America/Denver")
    private val nine = LocalTime.of(9, 0)

    private fun at(text: String): ZonedDateTime = ZonedDateTime.of(LocalDateTime.parse(text), zone)

    @Test
    fun nextOccurrenceIsLaterTodayOrTomorrow() {
        assertEquals(at("2026-10-02T09:00"), nextOccurrence(nine, at("2026-10-02T00:14")))
        assertEquals(at("2026-10-03T09:00"), nextOccurrence(nine, at("2026-10-02T09:00")))
        assertEquals(at("2026-10-03T09:00"), nextOccurrence(nine, at("2026-10-02T21:30")))
    }

    @Test
    fun nextOccurrenceSkipsPastAClockChangeGap() {
        // Clocks jump from 02:00 to 03:00 on 2026-03-08 in Denver.
        assertEquals(at("2026-03-08T03:30"), nextOccurrence(LocalTime.of(2, 30), at("2026-03-07T22:00")))
    }

    @Test
    fun theNextReminderIsOnSchedule() {
        assertTrue(isOnSchedule(at("2026-10-02T09:00"), nine, now = at("2026-10-02T00:14")))
        // WorkManager counts from when the previous run ended, a few seconds after the hour.
        assertTrue(isOnSchedule(at("2026-10-03T09:00:04"), nine, now = at("2026-10-02T09:00:04")))
    }

    @Test
    fun anOverdueCheckIsLeftToRun() {
        assertTrue(isOnSchedule(at("2026-10-01T09:00"), nine, now = at("2026-10-02T00:14")))
    }

    @Test
    fun aDriftedOrChangedScheduleIsNot() {
        // After a late run at 00:14, the next one would be 00:14 the day after.
        assertFalse(isOnSchedule(at("2026-10-03T00:14"), nine, now = at("2026-10-02T00:20")))
        // The reminder time changed from 09:00 to 18:30.
        assertFalse(isOnSchedule(at("2026-10-03T09:00"), LocalTime.of(18, 30), now = at("2026-10-02T12:00")))
        // A day too late.
        assertFalse(isOnSchedule(at("2026-10-04T09:00"), nine, now = at("2026-10-02T12:00")))
    }
}
