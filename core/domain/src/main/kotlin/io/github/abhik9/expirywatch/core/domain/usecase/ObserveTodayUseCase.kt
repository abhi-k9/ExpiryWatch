package io.github.abhik9.expirywatch.core.domain.usecase

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * Emits today's date, and again just after each midnight, so screens that stay open overnight
 * update "expires today" and "expired" labels on their own.
 */
class ObserveTodayUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(): Flow<LocalDate> = flow {
        while (true) {
            val now = LocalDateTime.now(clock)
            emit(now.toLocalDate())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            delay(Duration.between(now, nextMidnight).toMillis() + MIDNIGHT_GRACE_MILLIS)
        }
    }.distinctUntilChanged()

    private companion object {
        const val MIDNIGHT_GRACE_MILLIS = 1_000L
    }
}
