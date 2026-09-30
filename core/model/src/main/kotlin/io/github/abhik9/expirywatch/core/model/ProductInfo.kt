package io.github.abhik9.expirywatch.core.model

/**
 * What is known about a product identified by its [barcode]. Used to pre-fill a new item after a
 * scan. The expiry date is never part of it, since it differs from one package to the next.
 */
data class ProductInfo(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val quantity: Double? = null,
    val unit: QuantityUnit? = null,
    val useWithinDaysAfterOpening: Int? = null,
    val source: ProductSource,
)

enum class ProductSource {
    /** Remembered from an item the user saved earlier. */
    HISTORY,

    /** Looked up in the Open Food Facts database. */
    OPEN_FOOD_FACTS,
}
