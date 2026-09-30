package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.ProductHistoryRepository
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

enum class ItemValidationError {
    NAME_REQUIRED,
    QUANTITY_INVALID,
    OPENED_WINDOW_INVALID,
}

sealed interface SaveItemResult {
    data class Saved(val itemId: Long) : SaveItemResult

    data class Invalid(val errors: Set<ItemValidationError>) : SaveItemResult
}

object ItemValidator {
    const val MAX_QUANTITY = 1_000_000.0
    val OPENED_WINDOW_DAYS_RANGE = 1..3650

    fun validate(item: Item): Set<ItemValidationError> = buildSet {
        if (item.name.isBlank()) add(ItemValidationError.NAME_REQUIRED)
        if (!item.quantity.isFinite() || item.quantity <= 0.0 || item.quantity > MAX_QUANTITY) {
            add(ItemValidationError.QUANTITY_INVALID)
        }
        val window = item.useWithinDaysAfterOpening
        if (window != null && window !in OPENED_WINDOW_DAYS_RANGE) {
            add(ItemValidationError.OPENED_WINDOW_INVALID)
        }
    }
}

/**
 * Validates and saves an item. When the item has a barcode, its details are remembered so the
 * next scan of the same product can pre-fill them.
 */
class SaveItemUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val productHistoryRepository: ProductHistoryRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(item: Item): SaveItemResult {
        val cleaned = item.cleaned()
        val errors = ItemValidator.validate(cleaned)
        if (errors.isNotEmpty()) return SaveItemResult.Invalid(errors)

        val now = Instant.now(clock)
        val toSave = cleaned.copy(
            createdAt = if (cleaned.id == 0L) now else cleaned.createdAt,
            updatedAt = now,
        )
        val id = itemRepository.upsert(toSave)

        toSave.barcode?.let { barcode ->
            productHistoryRepository.remember(toSave.toProductInfo(barcode))
        }
        return SaveItemResult.Saved(id)
    }

    private fun Item.cleaned(): Item = copy(
        name = name.trim(),
        brand = brand.trimToNull(),
        barcode = barcode.trimToNull(),
        notes = notes.trimToNull(),
        imageUrl = imageUrl.trimToNull(),
    )

    private fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun Item.toProductInfo(barcode: String) = ProductInfo(
        barcode = barcode,
        name = name,
        brand = brand,
        imageUrl = imageUrl,
        categoryId = category?.id,
        locationId = location?.id,
        quantity = quantity,
        unit = unit,
        useWithinDaysAfterOpening = useWithinDaysAfterOpening,
        source = ProductSource.HISTORY,
    )
}
