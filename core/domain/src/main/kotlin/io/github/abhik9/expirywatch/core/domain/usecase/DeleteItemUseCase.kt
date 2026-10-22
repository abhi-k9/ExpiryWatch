package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.model.Item
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * The item deleted last, kept until a screen takes it to offer undoing the deletion. The editor
 * closes once it deletes, so the item list offers it instead.
 */
@Singleton
class RecentlyDeletedItems @Inject constructor() {
    private val last = MutableStateFlow<Item?>(null)

    /** The item deleted last that no screen took yet. */
    val deleted: StateFlow<Item?> = last.asStateFlow()

    fun record(item: Item) {
        last.value = item
    }

    /** Returns the item deleted last, unless another screen took it already, and forgets it. */
    fun take(): Item? = last.getAndUpdate { null }
}

/** Deletes an item, which can be undone; see [RecentlyDeletedItems]. */
class DeleteItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val recentlyDeleted: RecentlyDeletedItems,
) {
    suspend operator fun invoke(item: Item) {
        itemRepository.delete(item.id)
        recentlyDeleted.record(item)
    }

    /** Puts [item] back as it was before it was deleted. */
    suspend fun undo(item: Item) {
        itemRepository.restore(item)
    }
}
