package io.github.abhik9.expirywatch.feature.insights

import io.github.abhik9.expirywatch.core.domain.usecase.ObserveInsightsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.expirywatch.core.model.InsightsPeriod
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test

class InsightsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = FakeItemRepository(
        listOf(
            TestData.item(id = 1).copy(status = ItemStatus.CONSUMED, finishedDate = TestTime.today),
            TestData.item(id = 2).copy(status = ItemStatus.WASTED, finishedDate = TestTime.today.minusMonths(2)),
        ),
    )

    // Lazy, so the ViewModel is created after MainDispatcherRule has replaced the main dispatcher.
    private val viewModel by lazy {
        InsightsViewModel(ObserveInsightsUseCase(items, ObserveTodayUseCase(TestTime.clock)))
    }

    private suspend fun awaitCounts(consumed: Int, wasted: Int) = withTimeout(5.seconds) {
        viewModel.uiState.first { state ->
            state is InsightsUiState.Success &&
                state.insights.consumedCount == consumed &&
                state.insights.wastedCount == wasted
        }
    }

    @Test
    fun changingThePeriodRecalculates() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        awaitCounts(consumed = 1, wasted = 0)

        viewModel.onPeriodChange(InsightsPeriod.THREE_MONTHS)

        awaitCounts(consumed = 1, wasted = 1)
        assertEquals(InsightsPeriod.THREE_MONTHS, viewModel.period.value)
    }

    @Test
    fun axisMaximumIsRound() {
        assertEquals(1, niceAxisMax(0))
        assertEquals(2, niceAxisMax(2))
        assertEquals(5, niceAxisMax(3))
        assertEquals(10, niceAxisMax(7))
        assertEquals(20, niceAxisMax(11))
        assertEquals(100, niceAxisMax(51))
    }
}
