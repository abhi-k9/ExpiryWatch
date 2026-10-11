package io.github.abhik9.expirywatch.core.model

import java.time.Instant

/** A list of things to buy, such as "Groceries" or "Pharmacy". */
data class ShoppingList(
    val id: Long = 0,
    val name: String,
    val createdAt: Instant = Instant.EPOCH,
)

/**
 * Something to buy. The brand, barcode and storage location aren't shown much on the list, but
 * carry over to the item that's created when the shopping is put away.
 *
 * @property note what to look out for, such as "the large pack" or "lactose-free".
 * @property checkedAt when it was put in the cart; `null` while it's still to buy.
 */
data class ShoppingItem(
    val id: Long = 0,
    val listId: Long,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: Category? = null,
    val location: StorageLocation? = null,
    val quantity: Double = 1.0,
    val unit: QuantityUnit = QuantityUnit.PIECES,
    val note: String? = null,
    val checkedAt: Instant? = null,
    val createdAt: Instant = Instant.EPOCH,
) {
    val isChecked: Boolean get() = checkedAt != null
}

/**
 * Something added to a shopping list before, remembered to suggest it again and to put it in the
 * same category.
 */
data class ShoppingHistoryEntry(
    val name: String,
    val category: Category? = null,
    val timesAdded: Int = 1,
    val lastAddedAt: Instant = Instant.EPOCH,
)
