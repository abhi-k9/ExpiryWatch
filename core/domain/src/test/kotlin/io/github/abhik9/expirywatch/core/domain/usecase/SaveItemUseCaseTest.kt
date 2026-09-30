package io.github.abhik9.expirywatch.core.domain.usecase

import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeProductHistoryRepository
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class SaveItemUseCaseTest {
    private val items = FakeItemRepository()
    private val history = FakeProductHistoryRepository()
    private val saveItem = SaveItemUseCase(items, history, TestTime.clock)

    @Test
    fun rejectsInvalidItems() = runTest {
        val result = saveItem(TestData.item(name = "  ").copy(quantity = 0.0, useWithinDaysAfterOpening = 0))

        assertEquals(
            SaveItemResult.Invalid(
                setOf(
                    ItemValidationError.NAME_REQUIRED,
                    ItemValidationError.QUANTITY_INVALID,
                    ItemValidationError.OPENED_WINDOW_INVALID,
                ),
            ),
            result,
        )
        assertEquals(emptyList(), items.currentItems)
    }

    @Test
    fun trimsTextAndStampsNewItems() = runTest {
        val result = saveItem(
            TestData.item(name = "  Milk ").copy(brand = " ", notes = " fresh ", createdAt = Instant.EPOCH),
        )

        val id = assertIs<SaveItemResult.Saved>(result).itemId
        val saved = items.currentItems.single { it.id == id }
        assertEquals("Milk", saved.name)
        assertNull(saved.brand)
        assertEquals("fresh", saved.notes)
        assertEquals(TestTime.now, saved.createdAt)
        assertEquals(TestTime.now, saved.updatedAt)
    }

    @Test
    fun keepsCreationTimeOfExistingItems() = runTest {
        val created = Instant.parse("2025-01-01T00:00:00Z")
        saveItem(TestData.item(id = 7).copy(createdAt = created))

        val saved = items.currentItems.single()
        assertEquals(created, saved.createdAt)
        assertEquals(TestTime.now, saved.updatedAt)
    }

    @Test
    fun remembersProductDetailsForBarcodes() = runTest {
        saveItem(TestData.item(name = "Oat milk").copy(barcode = " 4001234567890 ", useWithinDaysAfterOpening = 5))

        val product = history.products.getValue("4001234567890")
        assertEquals("Oat milk", product.name)
        assertEquals(TestData.dairy.id, product.categoryId)
        assertEquals(TestData.fridge.id, product.locationId)
        assertEquals(5, product.useWithinDaysAfterOpening)
        assertEquals(ProductSource.HISTORY, product.source)
    }
}
