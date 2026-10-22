package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.model.UserSettings
import io.github.abhik9.expirywatch.core.testing.TestTime
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderHealthTest {
    private val healthy = ReminderHealth(
        notificationsAllowed = true,
        exactAlarmsAllowed = true,
        backgroundRestricted = false,
        lastShown = TestTime.now.minus(Duration.ofHours(20)),
    )

    @Test
    fun aHealthyReminderHasNoProblems() {
        assertEquals(emptyList(), healthy.problems(UserSettings(exactReminders = true), TestTime.now))
    }

    @Test
    fun listsWhatKeepsRemindersFromArrivingMostSeriousFirst() {
        val broken = ReminderHealth(
            notificationsAllowed = false,
            exactAlarmsAllowed = false,
            backgroundRestricted = true,
            lastShown = TestTime.now.minus(Duration.ofDays(3)),
        )

        assertEquals(
            listOf(
                ReminderProblem.NOTIFICATIONS_BLOCKED,
                ReminderProblem.EXACT_ALARMS_BLOCKED,
                ReminderProblem.BACKGROUND_RESTRICTED,
                ReminderProblem.LATE,
            ),
            broken.problems(UserSettings(exactReminders = true), TestTime.now),
        )
        // Blocked alarms only matter for exact reminders, and nothing matters with reminders off.
        assertEquals(
            listOf(ReminderProblem.NOTIFICATIONS_BLOCKED, ReminderProblem.BACKGROUND_RESTRICTED, ReminderProblem.LATE),
            broken.problems(UserSettings(exactReminders = false), TestTime.now),
        )
        assertEquals(emptyList(), broken.problems(UserSettings(remindersEnabled = false), TestTime.now))
    }

    @Test
    fun aReminderIsOnlyLateOnceOneWasShownAndADayWentByWithoutAnother() {
        assertEquals(emptyList(), healthy.copy(lastShown = null).problems(UserSettings(), TestTime.now))
        assertEquals(
            emptyList(),
            healthy.copy(lastShown = TestTime.now.minus(Duration.ofHours(35))).problems(UserSettings(), TestTime.now),
        )
        assertEquals(
            listOf(ReminderProblem.LATE),
            healthy.copy(lastShown = TestTime.now.minus(Duration.ofHours(37))).problems(UserSettings(), TestTime.now),
        )
    }
}
