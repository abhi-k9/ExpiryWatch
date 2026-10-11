package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.usecase.SaveItemUseCase
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeProductHistoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class PutAwayTest {
    private val items = FakeItemRepository()
    private val shopping = FakeShoppingRepository()
    private val getDrafts = GetPutAwayDraftsUseCase(shopping, items, TestTime.clock)
    private val putAway =
        PutAwayUseCase(SaveItemUseCase(items, FakeProductHistoryRepository(), TestTime.clock), shopping)
    private val today = TestTime.today

    private suspend fun buy(name: String, quantity: Double = 1.0, checked: Boolean = true) = shopping.upsertItem(
        TestData.shoppingItem(
            name = name,
            category = null,
            quantity = quantity,
            checkedAt = TestTime.now.takeIf { checked },
        ),
    )

    @Test
    fun guessesTheExpiryDateFromHowLongTheProductKeptLastTime() = runTest {
        // Milk added 10 days ago kept for a week; yogurt was used up last month, after keeping 3 weeks.
        items.setItems(
            listOf(
                TestData.item(id = 1, name = "Milk", expiresInDays = -3).copy(
                    createdAt = TestTime.now.minus(Duration.ofDays(10)),
                    useWithinDaysAfterOpening = 4,
                    imageUrl = "https://example.org/milk.jpg",
                ),
                TestData.item(id = 2, name = "Yogurt", expiresInDays = -9).copy(
                    createdAt = TestTime.now.minus(Duration.ofDays(30)),
                    status = ItemStatus.CONSUMED,
                    finishedDate = today.minusDays(20),
                ),
            ),
        )
        buy("milk")
        buy("Yogurt")
        buy("Bread")
        buy("Eggs", checked = false)

        val drafts = getDrafts(listId = 1)

        assertEquals(listOf("Bread", "milk", "Yogurt"), drafts.map { it.bought.name })
        assertNull(drafts[0].expiryDate)
        assertNull(drafts[0].location)
        assertEquals(today.plusDays(7), drafts[1].expiryDate)
        assertEquals(TestData.fridge, drafts[1].location)
        assertEquals(today.plusDays(21), drafts[2].expiryDate)
    }

    @Test
    fun putsAwayWhatHasAnExpiryDateAndTakesItOffTheList() = runTest {
        items.setItems(
            listOf(
                TestData.item(id = 1, name = "Milk", expiresInDays = 4).copy(
                    useWithinDaysAfterOpening = 4,
                    imageUrl = "https://example.org/milk.jpg",
                ),
            ),
        )
        buy("Milk", quantity = 2.0)
        buy("Bread")
        val (bread, milk) = getDrafts(listId = 1)

        val count = putAway(
            listOf(
                bread,
                milk.copy(expiryDate = today.plusDays(6), location = TestData.pantry),
            ),
        )

        assertEquals(1, count)
        val newMilk = items.currentItems.single { it.id != 1L }
        assertEquals("Milk", newMilk.name)
        assertEquals(today.plusDays(6), newMilk.expiryDate)
        assertEquals(2.0 to QuantityUnit.PIECES, newMilk.quantity to newMilk.unit)
        assertEquals(TestData.pantry, newMilk.location)
        assertEquals(TestData.dairy, newMilk.category)
        assertEquals(4, newMilk.useWithinDaysAfterOpening)
        assertEquals("https://example.org/milk.jpg", newMilk.imageUrl)
        assertEquals(TestTime.now, newMilk.createdAt)
        assertEquals(listOf("Bread"), shopping.currentItems.map { it.name })
    }
}
