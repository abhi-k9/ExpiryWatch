package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ReminderHealthMonitor
import io.github.abhik9.expirywatch.core.domain.repository.ReminderProblem
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.repository.problems
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

/** What keeps reminders from arriving, or on time; see [problems]. */
class ObserveReminderProblemsUseCase @Inject constructor(
    private val monitor: ReminderHealthMonitor,
    private val settingsRepository: UserSettingsRepository,
    private val clock: Clock,
) {
    /**
     * The problems now, and again whenever the settings change or [rechecks] emits. The system
     * doesn't tell when the user changes what it allows, so screens recheck when they're shown.
     */
    operator fun invoke(rechecks: Flow<Unit>): Flow<List<ReminderProblem>> = combine(
        settingsRepository.settings,
        rechecks.onStart { emit(Unit) },
    ) { settings, _ -> monitor.current().problems(settings, clock.instant()) }
        .distinctUntilChanged()
}
