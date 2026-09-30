package io.github.abhik9.expirywatch.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * Something the user keeps track of, such as a carton of milk or a box of medicine.
 *
 * @property expiryDate the "best before" or "use by" date printed on the package.
 * @property openedDate when the package was opened, if it has been.
 * @property useWithinDaysAfterOpening how long the item keeps once opened (e.g. "use within
 * 3 days of opening"). Together with [openedDate] this can bring the effective expiry forward;
 * see [effectiveExpiryDate].
 * @property status whether the item is still in stock, or was used up or thrown away.
 * @property finishedDate the day the item left [ItemStatus.ACTIVE]; `null` while it is active.
 */
data class Item(
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: Category? = null,
    val location: StorageLocation? = null,
    val quantity: Double = 1.0,
    val unit: QuantityUnit = QuantityUnit.PIECES,
    val expiryDate: LocalDate,
    val openedDate: LocalDate? = null,
    val useWithinDaysAfterOpening: Int? = null,
    val notes: String? = null,
    val imageUrl: String? = null,
    val status: ItemStatus = ItemStatus.ACTIVE,
    val finishedDate: LocalDate? = null,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
)

enum class ItemStatus {
    /** Still in stock. */
    ACTIVE,

    /** Used up (eaten, taken, finished). */
    CONSUMED,

    /** Thrown away, typically because it went off. */
    WASTED,
}

enum class QuantityUnit {
    PIECES,
    PACKS,
    GRAMS,
    KILOGRAMS,
    MILLILITERS,
    LITERS,
}
