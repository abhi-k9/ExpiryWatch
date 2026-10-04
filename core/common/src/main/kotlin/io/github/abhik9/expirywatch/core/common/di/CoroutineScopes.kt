package io.github.abhik9.expirywatch.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** A scope that lives as long as the application process, for work that outlives a screen. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopesModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun providesApplicationScope(
        @Dispatcher(AppDispatcher.Default) dispatcher: CoroutineDispatcher,
        log: EventLog,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher + backgroundErrorHandler(log))
}

/**
 * Background work failing, such as refreshing widgets, shouldn't take the whole app down: the
 * error is recorded instead, and the rest of the work in the scope carries on.
 */
internal fun backgroundErrorHandler(log: EventLog) = CoroutineExceptionHandler { _, error ->
    error.printStackTrace()
    log.record { "background work failed: ${error.stackTraceToString()}" }
}
