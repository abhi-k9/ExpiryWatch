package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ShoppingList
import javax.inject.Inject

/** What was put on which shopping list. */
data class AddedToShoppingList(
    val list: ShoppingList,
    /** The names of the products put on it, as they're called on the list. */
    val names: List<String>,
)

/**
 * Puts the products of tracked items on the shopping list in use, each product once, in the
 * amount and with the details its item has.
 */
class AddItemsToShoppingListUseCase @Inject constructor(
    private val getShoppingList: GetShoppingListUseCase,
    private val addToShoppingList: AddToShoppingListUseCase,
) {
    suspend operator fun invoke(items: List<Item>): AddedToShoppingList {
        val list = getShoppingList()
        val names = items
            .filter { productKey(it.name).isNotEmpty() }
            .distinctBy { productKey(it.name) }
            .map { item -> addToShoppingList(list.id, item.toShoppingEntry()).item.name }
        return AddedToShoppingList(list, names)
    }
}

/** What to put on a shopping list to buy [this] item again. */
fun Item.toShoppingEntry() = ShoppingEntry(
    name = name,
    quantity = quantity,
    unit = unit,
    category = category,
    brand = brand,
    barcode = barcode,
    location = location,
)
