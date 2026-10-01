package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.testing.diagnostics.RecordingEventLog
import io.github.abhik9.expirywatch.core.testing.repository.FakeProductCatalog
import io.github.abhik9.expirywatch.core.testing.repository.FakeProductHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class LookupProductUseCaseTest {
    private val history = FakeProductHistoryRepository()
    private val catalog = FakeProductCatalog()
    private val log = RecordingEventLog()
    private val lookup = LookupProductUseCase(history, catalog, log)

    private val ean = "3017620422003"

    @Test
    fun prefersTheUsersOwnHistory() = runTest {
        val remembered = ProductInfo(barcode = ean, name = "My spread", source = ProductSource.HISTORY)
        history.remember(remembered)
        catalog.products[ean] = ProductInfo(barcode = ean, name = "Nutella", source = ProductSource.OPEN_FOOD_FACTS)

        assertEquals(ProductLookupResult.Found(remembered), lookup(ean))
        assertEquals(emptyList(), catalog.requestedBarcodes)
    }

    @Test
    fun fallsBackToTheCatalog() = runTest {
        val product = ProductInfo(barcode = ean, name = "Nutella", source = ProductSource.OPEN_FOOD_FACTS)
        catalog.products[ean] = product

        assertEquals(ProductLookupResult.Found(product), lookup(" $ean "))
    }

    @Test
    fun reportsUnknownProducts() = runTest {
        assertEquals(ProductLookupResult.NotFound, lookup(ean))
    }

    @Test
    fun reportsAnUnreachableCatalog() = runTest {
        catalog.isOffline = true
        assertEquals(ProductLookupResult.CatalogUnavailable, lookup(ean))
        assertTrue(log.messages.single().startsWith("lookup $ean: catalog unavailable: "), log.messages.single())
    }

    @Test
    fun doesNotQueryTheCatalogForNonRetailCodes() = runTest {
        assertEquals(ProductLookupResult.NotFound, lookup("https://example.com/qr"))
        assertEquals(ProductLookupResult.NotFound, lookup("12345"))
        assertEquals(ProductLookupResult.NotFound, lookup(""))
        assertEquals(emptyList(), catalog.requestedBarcodes)
    }

    @Test
    fun recordsOutcomesButNotTheContentOfOtherCodes() = runTest {
        catalog.products[ean] = ProductInfo(barcode = ean, name = "Nutella", source = ProductSource.OPEN_FOOD_FACTS)

        lookup(ean)
        lookup("4006381333931")
        lookup("WIFI:S:home;P:secret;;")

        assertEquals(
            listOf(
                "lookup $ean: found in the catalog",
                "lookup 4006381333931: not in the catalog",
                "lookup of 22 characters: not a retail product code",
            ),
            log.messages,
        )
    }
}
