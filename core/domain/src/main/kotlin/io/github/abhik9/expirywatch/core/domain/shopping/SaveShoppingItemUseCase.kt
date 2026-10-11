package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Saves changes made to an item on a shopping list. A product that's renamed or moved to another
 * category is remembered that way, so it's suggested and sorted the same way next time.
 */
class SaveShoppingItemUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val clock: Clock,
) {
    /** Returns false, without saving, when [changed] has no name or an amount out of range. */
    suspend operator fun invoke(original: ShoppingItem, changed: ShoppingItem): Boolean {
        val item = changed.copy(
            name = changed.name.trim(),
            brand = changed.brand?.trim()?.ifEmpty { null },
            note = changed.note?.trim()?.ifEmpty { null },
        )
        if (item.name.isEmpty() || !isValidShoppingQuantity(item.quantity)) return false
        shoppingRepository.upsertItem(item)
        if (item.name != original.name || item.category != original.category) {
            shoppingRepository.rememberProduct(item.name, item.category, Instant.now(clock))
        }
        return true
    }
}
