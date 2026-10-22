package io.github.abhik9.expirywatch.core.notifications

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.abhik9.expirywatch.core.domain.repository.ReminderHealth
import io.github.abhik9.expirywatch.core.domain.repository.ReminderHealthMonitor
import javax.inject.Inject

internal class AndroidReminderHealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarm: ReminderAlarm,
    private val history: ReminderHistory,
) : ReminderHealthMonitor {
    override fun current() = ReminderHealth(
        // Covers both the Android 13+ permission and notifications turned off in the system settings.
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        exactAlarmsAllowed = alarm.isAllowed(),
        // Battery set to "Restricted", which includes One UI's sleeping and deep sleeping apps.
        backgroundRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true,
        lastShown = history.lastShown,
    )
}
