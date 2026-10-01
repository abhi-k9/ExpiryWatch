package io.github.abhik9.expirywatch.core.notifications.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.expirywatch.core.domain.repository.ReminderScheduler
import io.github.abhik9.expirywatch.core.notifications.RemindersDiagnosticsSection
import io.github.abhik9.expirywatch.core.notifications.WorkManagerReminderScheduler

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {
    @Binds
    abstract fun bindsReminderScheduler(scheduler: WorkManagerReminderScheduler): ReminderScheduler

    @Binds
    @IntoSet
    abstract fun bindsRemindersDiagnosticsSection(section: RemindersDiagnosticsSection): DiagnosticsSection
}
