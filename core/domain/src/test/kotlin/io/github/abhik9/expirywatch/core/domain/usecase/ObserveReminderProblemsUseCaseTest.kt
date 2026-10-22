package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ReminderProblem
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeReminderHealthMonitor
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveReminderProblemsUseCaseTest {
    @Test
    fun readsTheProblemsAgainOnEachRecheckAndSettingsChange() = runTest {
        val monitor = FakeReminderHealthMonitor()
        val settings = FakeUserSettingsRepository()
        val rechecks = MutableSharedFlow<Unit>()
        val seen = mutableListOf<List<ReminderProblem>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveReminderProblemsUseCase(monitor, settings, TestTime.clock)(rechecks).toList(seen)
        }

        monitor.health = monitor.health.copy(notificationsAllowed = false)
        rechecks.emit(Unit)
        settings.setRemindersEnabled(false)

        assertEquals(listOf(emptyList(), listOf(ReminderProblem.NOTIFICATIONS_BLOCKED), emptyList()), seen)
    }
}
