package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.domain.usecase.ItemValidator
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

sealed interface AddToShoppingListResult {
    val item: ShoppingItem

    /** A new item on the list. */
    data class Added(override val item: ShoppingItem) : AddToShoppingListResult

    /** It was on the list already, and that item now includes what was added. */
    data class AlreadyOnList(override val item: ShoppingItem) : AddToShoppingListResult

    /** It was in the cart, and is back on the list. */
    data class BackOnList(override val item: ShoppingItem) : AddToShoppingListResult
}

/**
 * Adds something to a shopping list without making a duplicate: a product already on the list gets
 * the quantity added to it, and one already in the cart goes back on the list. A product the user
 * knows keeps its name and category, so "milk" goes in as "Milk", under Dairy; see
 * [ObserveKnownProductsUseCase]. Every product added is remembered, to suggest it again.
 */
class AddToShoppingListUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val observeKnownProducts: ObserveKnownProductsUseCase,
    private val clock: Clock,
) {
    suspend operator fun invoke(listId: Long, entry: ShoppingEntry): AddToShoppingListResult {
        val name = entry.name.trim()
        require(name.isNotEmpty()) { "Can't add an item without a name" }
        val now = Instant.now(clock)
        val key = productKey(name)
        val sameProduct = shoppingRepository.getItems(listId).filter { productKey(it.name) == key }
        val toBuy = sameProduct.firstOrNull { !it.isChecked }
        val inCart = sameProduct.firstOrNull { it.isChecked }

        val result = when {
            toBuy != null -> AddToShoppingListResult.AlreadyOnList(toBuy.plus(entry).withDetailsFrom(entry))

            inCart != null -> AddToShoppingListResult.BackOnList(
                inCart.copy(checkedAt = null, quantity = entry.quantity, unit = entry.unit).withDetailsFrom(entry),
            )

            else -> {
                val known = observeKnownProducts().first().firstOrNull { it.key == key }
                AddToShoppingListResult.Added(
                    ShoppingItem(
                        listId = listId,
                        name = known?.name ?: name,
                        brand = entry.brand,
                        barcode = entry.barcode,
                        category = entry.category ?: known?.category,
                        location = entry.location,
                        quantity = entry.quantity,
                        unit = entry.unit,
                        note = entry.note,
                        createdAt = now,
                    ),
                )
            }
        }
        val id = shoppingRepository.upsertItem(result.item)
        shoppingRepository.rememberAdded(result.item.name, result.item.category, now)
        return when (result) {
            is AddToShoppingListResult.Added -> result.copy(item = result.item.copy(id = id))
            else -> result
        }
    }

    /**
     * Adds the quantities when they're in the same unit. When they aren't, a plain "1" gives way to
     * the more precise amount, and otherwise the amount on the list stays.
     */
    private fun ShoppingItem.plus(entry: ShoppingEntry): ShoppingItem = when {
        unit == entry.unit -> copy(quantity = (quantity + entry.quantity).coerceAtMost(ItemValidator.MAX_QUANTITY))
        quantity == 1.0 && unit == QuantityUnit.PIECES -> copy(quantity = entry.quantity, unit = entry.unit)
        else -> this
    }

    /** Fills in details the item doesn't have yet. */
    private fun ShoppingItem.withDetailsFrom(entry: ShoppingEntry) = copy(
        brand = brand ?: entry.brand,
        barcode = barcode ?: entry.barcode,
        category = category ?: entry.category,
        location = location ?: entry.location,
        note = note ?: entry.note,
    )
}
