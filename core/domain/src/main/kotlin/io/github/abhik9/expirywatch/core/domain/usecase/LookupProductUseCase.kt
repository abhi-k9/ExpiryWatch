package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.repository.ProductCatalog
import io.github.abhik9.expirywatch.core.domain.repository.ProductHistoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ProductInfo
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

sealed interface ProductLookupResult {
    data class Found(val product: ProductInfo) : ProductLookupResult

    data object NotFound : ProductLookupResult

    /** The online catalog couldn't be reached, e.g. because the device is offline. */
    data object CatalogUnavailable : ProductLookupResult

    /** Not among the user's saved products, and they turned off looking up products online. */
    data object NotFoundOnlineLookupOff : ProductLookupResult
}

/**
 * Finds details for a scanned barcode: first among products the user saved before, then in the
 * online catalog unless the user turned that off. The user's own history wins, since it reflects
 * how they like to record items.
 */
class LookupProductUseCase @Inject constructor(
    private val history: ProductHistoryRepository,
    private val catalog: ProductCatalog,
    private val settingsRepository: UserSettingsRepository,
    private val log: EventLog,
) {
    suspend operator fun invoke(rawBarcode: String): ProductLookupResult {
        val barcode = rawBarcode.trim()
        if (barcode.isEmpty()) return ProductLookupResult.NotFound

        history.find(barcode)?.let { product ->
            log.record { "lookup ${barcode.forLog()}: found in history" }
            return ProductLookupResult.Found(product)
        }

        // Online catalogs only know retail product codes (EAN/UPC), not e.g. QR codes.
        if (!barcode.isRetailProductCode()) {
            log.record { "lookup ${barcode.forLog()}: not a retail product code" }
            return ProductLookupResult.NotFound
        }

        if (!settingsRepository.settings.first().onlineProductLookup) {
            log.record { "lookup $barcode: not in history, and online lookup is off" }
            return ProductLookupResult.NotFoundOnlineLookupOff
        }

        val product = try {
            catalog.find(barcode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.record { "lookup $barcode: catalog unavailable: $e" }
            return ProductLookupResult.CatalogUnavailable
        }
        log.record { "lookup $barcode: ${if (product == null) "not in" else "found in"} the catalog" }
        return product?.let(ProductLookupResult::Found) ?: ProductLookupResult.NotFound
    }
}

private val retailCodeLengths = setOf(8, 12, 13, 14)

/** EAN-8, UPC-A, EAN-13 or GTIN-14. */
internal fun String.isRetailProductCode(): Boolean = length in retailCodeLengths && all(Char::isDigit)

/** Other codes, such as QR codes, can contain personal data, so only their length is recorded. */
private fun String.forLog(): String = if (isRetailProductCode()) this else "of $length characters"
