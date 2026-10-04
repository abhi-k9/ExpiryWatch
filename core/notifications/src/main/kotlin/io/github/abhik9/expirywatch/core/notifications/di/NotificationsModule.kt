package io.github.abhik9.expirywatch.core.notifications.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.expirywatch.core.domain.repository.ReminderScheduler
import io.github.abhik9.expirywatch.core.notifications.AndroidReminderScheduler
import io.github.abhik9.expirywatch.core.notifications.RemindersDiagnosticsSection

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {
    @Binds
    abstract fun bindsReminderScheduler(scheduler: AndroidReminderScheduler): ReminderScheduler

    @Binds
    @IntoSet
    abstract fun bindsRemindersDiagnosticsSection(section: RemindersDiagnosticsSection): DiagnosticsSection
}
