package io.github.abhik9.expirywatch.core.domain.shopping

import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.model.ShoppingList
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class CurrentShoppingListTest {
    private val shopping = FakeShoppingRepository()
    private val settings = FakeUserSettingsRepository()
    private val getShoppingList = GetShoppingListUseCase(shopping, settings, "Shopping list", TestTime.clock)
    private val observeShoppingList = ObserveShoppingListUseCase(shopping, settings, getShoppingList)

    @Test
    fun makesOneListWhenThereIsNone() = runTest {
        val lists = List(3) { async { getShoppingList() } }.awaitAll()

        val made = ShoppingList(id = 1, name = "Shopping list", createdAt = TestTime.now)
        assertEquals(listOf(made, made, made), lists)
        assertEquals(listOf(made), shopping.currentLists)
    }

    @Test
    fun usesTheListOpenedLastOrElseTheFirst() = runTest {
        shopping.createList("Groceries", TestTime.now)
        val pharmacy = shopping.createList("Pharmacy", TestTime.now)
        assertEquals("Groceries", getShoppingList().name)

        settings.setShoppingListId(pharmacy)
        assertEquals("Pharmacy", getShoppingList().name)

        settings.setShoppingListId(42)
        assertEquals("Groceries", getShoppingList().name)
        assertEquals(2, shopping.currentLists.size)
    }

    @Test
    fun followsTheListOpenedAndMakesAnotherWhenTheLastIsDeleted() = runTest {
        observeShoppingList().test {
            val first = awaitItem()
            assertEquals("Shopping list", first.name)

            val pharmacy = shopping.createList("Pharmacy", TestTime.now)
            settings.setShoppingListId(pharmacy)
            assertEquals("Pharmacy", awaitItem().name)

            shopping.renameList(pharmacy, "Chemist")
            assertEquals("Chemist", awaitItem().name)

            shopping.deleteList(pharmacy)
            assertEquals(first, awaitItem())

            shopping.deleteList(first.id)
            assertEquals(ShoppingList(id = 3, name = "Shopping list", createdAt = TestTime.now), awaitItem())
        }
    }

    @Test
    fun summarizesWhatIsLeftToBuyOnEachList() = runTest {
        val groceries = shopping.createList("Groceries", TestTime.now)
        val pharmacy = shopping.createList("Pharmacy", TestTime.now)
        shopping.upsertItem(TestData.shoppingItem(listId = groceries, name = "Milk"))
        shopping.upsertItem(TestData.shoppingItem(listId = groceries, name = "Bread", checkedAt = TestTime.now))

        val summaries = ObserveShoppingListSummariesUseCase(shopping)().first()

        assertEquals(listOf("Groceries" to 1, "Pharmacy" to 0), summaries.map { it.list.name to it.toBuyCount })
        assertEquals(pharmacy, summaries.last().list.id)
    }
}
