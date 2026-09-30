package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.insights.InsightsCalculator
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.model.Insights
import io.github.abhik9.expirywatch.core.model.InsightsPeriod
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class ObserveInsightsUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val observeToday: ObserveTodayUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(period: Flow<InsightsPeriod>): Flow<Insights> = observeToday().flatMapLatest { today ->
        combine(
            itemRepository.observeFinishedItems(since = InsightsCalculator.earliestRelevantDate(today)),
            period,
        ) { finishedItems, currentPeriod ->
            InsightsCalculator.calculate(finishedItems, currentPeriod, today)
        }
    }
}
