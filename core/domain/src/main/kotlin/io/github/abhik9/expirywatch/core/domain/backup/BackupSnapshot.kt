package io.github.abhik9.expirywatch.core.domain.backup

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ShoppingHistoryEntry
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import io.github.abhik9.expirywatch.core.model.ShoppingList
import io.github.abhik9.expirywatch.core.model.StorageLocation

/** Everything the user has entered, as written to and read from a backup file. */
data class BackupSnapshot(
    val categories: List<Category>,
    val locations: List<StorageLocation>,
    val items: List<Item>,
    val products: List<ProductInfo>,
    /** The shopping lists; `null` when read from a backup made before the app had them. */
    val shopping: ShoppingBackup? = null,
)

/** The shopping lists with what's on them, and the products added to them before. */
data class ShoppingBackup(
    val lists: List<ShoppingList> = emptyList(),
    val items: List<ShoppingItem> = emptyList(),
    val history: List<ShoppingHistoryEntry> = emptyList(),
    /** The ids of the categories in the order of the shop's aisles, which the settings keep. */
    val aisleOrder: List<Long> = emptyList(),
) {
    /**
     * The same lists, with every category and location swapped for the one of the same name
     * among [categories] and [locations], or none when there isn't one. The aisle order, which
     * refers to categories by id, is left out.
     */
    fun relinkedTo(categories: List<Category>, locations: List<StorageLocation>): ShoppingBackup {
        val categoriesByName = categories.associateBy { productKey(it.name) }
        val locationsByName = locations.associateBy { productKey(it.name) }
        fun Category.relinked() = categoriesByName[productKey(name)]
        fun StorageLocation.relinked() = locationsByName[productKey(name)]
        return copy(
            items = items.map { it.copy(category = it.category?.relinked(), location = it.location?.relinked()) },
            history = history.map { it.copy(category = it.category?.relinked()) },
            aisleOrder = emptyList(),
        )
    }
}

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)
