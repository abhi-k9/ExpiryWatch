package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.model.ShoppingList

/** What's still to buy in one category, found together in the shop. `null` holds the items without one. */
data class ShoppingAisle(
    val category: Category?,
    val items: List<ShoppingItem>,
)

/**
 * A shopping list as it's shown.
 *
 * @property aisles what's still to buy, by category in the order of the shop, with the items
 * without a category last.
 * @property inCart what's in the cart, the last thing put in first, so a mistap is easy to undo.
 */
data class ShoppingListOverview(
    val list: ShoppingList,
    val aisles: List<ShoppingAisle>,
    val inCart: List<ShoppingItem>,
) {
    val toBuyCount: Int get() = aisles.sumOf { it.items.size }

    val isEmpty: Boolean get() = aisles.isEmpty() && inCart.isEmpty()
}

/**
 * Groups what's still to buy into aisles: categories in [aisleOrder] first, in that order, then the
 * others by name, and the items without a category last.
 */
fun buildShoppingOverview(
    list: ShoppingList,
    items: List<ShoppingItem>,
    aisleOrder: List<Long> = emptyList(),
): ShoppingListOverview {
    val (inCart, toBuy) = items.partition { it.isChecked }
    val byName = compareBy<ShoppingItem> { productKey(it.name) }.thenBy { it.id }
    val aisles = toBuy
        .groupBy { it.category?.id }
        .map { (_, inCategory) -> ShoppingAisle(inCategory.first().category, inCategory.sortedWith(byName)) }
        .sortedWith(
            compareBy<ShoppingAisle> { it.category == null }
                .thenBy(aisleComparator(aisleOrder)) { it.category },
        )
    return ShoppingListOverview(
        list = list,
        aisles = aisles,
        inCart = inCart.sortedWith(compareByDescending<ShoppingItem> { it.checkedAt }.thenByDescending { it.id }),
    )
}

/** Puts categories in the order of the shop: those in [aisleOrder] first, in that order, then the rest by name. */
fun aisleComparator(aisleOrder: List<Long>): Comparator<Category?> {
    val positions = aisleOrder.withIndex().associate { (index, id) -> id to index }
    return compareBy<Category?> { category -> category?.let { positions[it.id] } ?: Int.MAX_VALUE }
        .thenBy { category -> category?.let { productKey(it.name) } }
        .thenBy { category -> category?.id }
}
