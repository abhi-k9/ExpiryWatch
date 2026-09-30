package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.model.ItemStatus
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Marks an item as used up or thrown away today. Undo with [RestoreItemUseCase]. */
class FinishItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(itemId: Long, outcome: ItemStatus) {
        require(outcome != ItemStatus.ACTIVE) { "Use RestoreItemUseCase to make an item active again" }
        itemRepository.updateStatus(itemId, outcome, finishedDate = LocalDate.now(clock))
    }
}

/** Puts a finished item back into stock. */
class RestoreItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
) {
    suspend operator fun invoke(itemId: Long) {
        itemRepository.updateStatus(itemId, ItemStatus.ACTIVE, finishedDate = null)
    }
}
