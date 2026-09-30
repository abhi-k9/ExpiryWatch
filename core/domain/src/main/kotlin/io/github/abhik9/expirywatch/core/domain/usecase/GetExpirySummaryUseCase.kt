package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
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
    private val clock: Clock,
) {
    suspend operator fun invoke(): ExpirySummary {
        val today = LocalDate.now(clock)
        val settings = settingsRepository.settings.first()
        val overview = buildItemsOverview(
            items = itemRepository.getActiveItems(),
            query = ItemQuery(),
            sortOrder = ItemSortOrder.EXPIRY_SOONEST,
            today = today,
            expiringSoonDays = settings.expiringSoonDays,
        )
        return ExpirySummary(items = overview.items, today = today)
    }
}
