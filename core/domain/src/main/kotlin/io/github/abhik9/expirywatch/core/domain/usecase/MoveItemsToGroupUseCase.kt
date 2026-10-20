package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.model.Item
import javax.inject.Inject

/** Puts items in one of the user's groups, or takes them out of theirs. */
class MoveItemsToGroupUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
) {
    /** Moves [items] to the group with [groupId], or out of any group when it's `null`. */
    suspend operator fun invoke(items: List<Item>, groupId: Long?) {
        itemRepository.moveToGroup(items.map { it.id }, groupId)
    }

    /** Puts each of [items] back in the group it was in, to undo a move. */
    suspend fun restore(items: List<Item>) {
        items.groupBy({ it.customGroup?.id }, { it.id })
            .forEach { (groupId, ids) -> itemRepository.moveToGroup(ids, groupId) }
    }
}
