package io.github.abhik9.expirywatch.core.notifications

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers the last reminder shown, so the alarm and the daily work don't both show it. */
@Singleton
internal class ReminderHistory @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** When the last reminder shown was due. */
    val lastShown: Instant?
        get() = preferences.getLong(KEY_LAST_SHOWN, NONE).takeIf { it != NONE }?.let(Instant::ofEpochMilli)

    /** Records that the reminder due at [occurrence] is shown. Returns false if it already was. */
    @Synchronized
    fun markShown(occurrence: ZonedDateTime): Boolean {
        val millis = occurrence.toInstant().toEpochMilli()
        if (preferences.getLong(KEY_LAST_SHOWN, NONE) == millis) return false
        // Written right away, in case the process ends before another check runs.
        preferences.edit(commit = true) { putLong(KEY_LAST_SHOWN, millis) }
        return true
    }

    private companion object {
        const val PREFERENCES_NAME = "reminders"
        const val KEY_LAST_SHOWN = "last_shown"
        const val NONE = Long.MIN_VALUE
    }
}
