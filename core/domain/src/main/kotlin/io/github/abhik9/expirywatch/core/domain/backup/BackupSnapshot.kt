package io.github.abhik9.expirywatch.core.domain.backup

import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.StorageLocation

/** Everything the user has entered, as written to and read from a backup file. */
data class BackupSnapshot(
    val categories: List<Category>,
    val locations: List<StorageLocation>,
    val items: List<Item>,
    val products: List<ProductInfo>,
)

class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)
