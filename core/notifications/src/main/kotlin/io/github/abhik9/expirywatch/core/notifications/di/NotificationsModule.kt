package io.github.abhik9.expirywatch.core.notifications.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.domain.repository.ReminderScheduler
import io.github.abhik9.expirywatch.core.notifications.WorkManagerReminderScheduler

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {
    @Binds
    abstract fun bindsReminderScheduler(scheduler: WorkManagerReminderScheduler): ReminderScheduler
}
