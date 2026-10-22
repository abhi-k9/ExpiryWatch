package io.github.abhik9.expirywatch.core.domain.repository

import io.github.abhik9.expirywatch.core.model.UserSettings
import java.time.Duration
import java.time.Instant

/** What the system allows that the daily reminder depends on, and when it was last shown. */
data class ReminderHealth(
    val notificationsAllowed: Boolean,
    val exactAlarmsAllowed: Boolean,
    /** Whether the user restricted the app's background activity, as One UI's sleeping apps do. */
    val backgroundRestricted: Boolean,
    /** When the last reminder shown was due, or `null` if none was shown yet. */
    val lastShown: Instant?,
)

/** Something that keeps reminders from arriving, or on time. */
enum class ReminderProblem {
    NOTIFICATIONS_BLOCKED,
    EXACT_ALARMS_BLOCKED,
    BACKGROUND_RESTRICTED,

    /** No reminder was shown for more than a day, so at least one went missing. */
    LATE,
}

/** Reads the [ReminderHealth] from the system as it is now. */
interface ReminderHealthMonitor {
    fun current(): ReminderHealth
}

/** How long without a reminder means one went missing: a day, plus room for moving its time. */
val MISSED_REMINDER_AFTER: Duration = Duration.ofHours(36)

/** What keeps reminders from working, most serious first; none when reminders are off. */
fun ReminderHealth.problems(settings: UserSettings, now: Instant): List<ReminderProblem> {
    if (!settings.remindersEnabled) return emptyList()
    return buildList {
        if (!notificationsAllowed) add(ReminderProblem.NOTIFICATIONS_BLOCKED)
        if (settings.exactReminders && !exactAlarmsAllowed) add(ReminderProblem.EXACT_ALARMS_BLOCKED)
        if (backgroundRestricted) add(ReminderProblem.BACKGROUND_RESTRICTED)
        val shown = lastShown
        if (shown != null && Duration.between(shown, now) > MISSED_REMINDER_AFTER) add(ReminderProblem.LATE)
    }
}
