package io.github.abhik9.expirywatch.core.domain.shopping

import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.model.ShoppingList
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest

class CurrentShoppingListTest {
    private val shopping = FakeShoppingRepository()
    private val getShoppingList = GetShoppingListUseCase(shopping, "Shopping list", TestTime.clock)

    @Test
    fun makesOneListWhenThereIsNone() = runTest {
        val lists = List(3) { async { getShoppingList() } }.awaitAll()

        val made = ShoppingList(id = 1, name = "Shopping list", createdAt = TestTime.now)
        assertEquals(listOf(made, made, made), lists)
        assertEquals(listOf(made), shopping.currentLists)
    }

    @Test
    fun usesTheFirstList() = runTest {
        shopping.createList("Groceries", TestTime.now)
        shopping.createList("Pharmacy", TestTime.now)

        assertEquals("Groceries", getShoppingList().name)
        assertEquals(2, shopping.currentLists.size)
    }

    @Test
    fun followsTheListAndMakesAnotherWhenItIsDeleted() = runTest {
        ObserveShoppingListUseCase(shopping, getShoppingList)().test {
            val first = awaitItem()
            assertEquals("Shopping list", first.name)

            shopping.renameList(first.id, "Groceries")
            assertEquals("Groceries", awaitItem().name)

            shopping.deleteList(first.id)
            assertEquals(ShoppingList(id = 2, name = "Shopping list", createdAt = TestTime.now), awaitItem())
            assertEquals(1, shopping.currentLists.size)
        }
    }
}
