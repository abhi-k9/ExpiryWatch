package io.github.abhik9.expirywatch.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

/**
 * Provides the system clock. Everything that needs "now" or "today" takes a [Clock], so tests can
 * pin time with [Clock.fixed].
 */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    // Not a singleton: the default zone is read on each injection, so a time zone change is
    // picked up by newly created objects.
    @Provides
    fun providesClock(): Clock = Clock.systemDefaultZone()
}
