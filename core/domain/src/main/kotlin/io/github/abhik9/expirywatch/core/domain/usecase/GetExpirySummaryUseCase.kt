package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** A snapshot of active items, soonest to expire first, for reminders and the widget. */
data class ExpirySummary(
    val items: List<ItemWithExpiry>,
    val today: LocalDate,
) {
    val expired: List<ItemWithExpiry> = items.filter { it.status == ExpiryStatus.EXPIRED }
    val expiringSoon: List<ItemWithExpiry> = items.filter { it.status == ExpiryStatus.EXPIRING_SOON }
}

class GetExpirySummaryUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val settingsRepository: UserSettingsRepository,
    private val observeToday: ObserveTodayUseCase,
) {
    suspend operator fun invoke(): ExpirySummary = summarize(
        items = itemRepository.getActiveItems(),
        expiringSoonDays = settingsRepository.settings.first().expiringSoonDays,
        today = observeToday().first(),
    )

    /** The summary, updated whenever items, the settings or the date change. */
    fun observe(): Flow<ExpirySummary> = combine(
        itemRepository.observeActiveItems(),
        settingsRepository.settings,
        observeToday(),
    ) { items, settings, today ->
        summarize(items, settings.expiringSoonDays, today)
    }

    private fun summarize(items: List<Item>, expiringSoonDays: Int, today: LocalDate): ExpirySummary {
        val overview = buildItemsOverview(
            items = items,
            query = ItemQuery(),
            sortOrder = ItemSortOrder.EXPIRY_SOONEST,
            today = today,
            expiringSoonDays = expiringSoonDays,
        )
        return ExpirySummary(items = overview.items, today = today)
    }
}
