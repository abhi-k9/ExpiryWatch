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
    fun savesEachAdditionalDateAsAnUnopenedEntryOfItsOwn() = runTest {
        val opened = TestData.item(name = "Yogurt", expiresInDays = 2).copy(
            quantity = 2.0,
            openedDate = TestTime.today,
            useWithinDaysAfterOpening = 4,
            notes = "Greek",
        )

        val result = saveItem(
            opened,
            listOf(
                AdditionalDate(TestTime.today.plusDays(6), quantity = 1.0),
                AdditionalDate(TestTime.today.plusDays(13), quantity = 3.0),
            ),
        )

        val mainId = assertIs<SaveItemResult.Saved>(result).itemId
        val (main, others) = items.currentItems.partition { it.id == mainId }
        assertEquals(TestTime.today, main.single().openedDate)
        assertEquals(
            listOf(TestTime.today.plusDays(6) to 1.0, TestTime.today.plusDays(13) to 3.0),
            others.map { it.expiryDate to it.quantity },
        )
        others.forEach {
            assertNull(it.openedDate)
            assertEquals("Yogurt", it.name)
            assertEquals(TestData.dairy, it.category)
            assertEquals(TestData.fridge, it.location)
            assertEquals(4, it.useWithinDaysAfterOpening)
            assertEquals("Greek", it.notes)
            assertEquals(TestTime.now, it.createdAt)
        }
    }

    @Test
    fun additionalDatesOfAnExistingItemAreNewEntries() = runTest {
        val created = Instant.parse("2025-01-01T00:00:00Z")
        saveItem(TestData.item(id = 7).copy(createdAt = created), listOf(AdditionalDate(TestTime.today, 1.0)))

        val (existing, added) = items.currentItems.partition { it.id == 7L }
        assertEquals(created, existing.single().createdAt)
        assertEquals(TestTime.now, added.single().createdAt)
    }

    @Test
    fun nothingIsSavedWhenAnAdditionalDateIsInvalid() = runTest {
        val result = saveItem(TestData.item(), listOf(AdditionalDate(TestTime.today, quantity = 0.0)))

        assertEquals(SaveItemResult.Invalid(setOf(ItemValidationError.QUANTITY_INVALID)), result)
        assertEquals(emptyList(), items.currentItems)
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
