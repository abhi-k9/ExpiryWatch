package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ShoppingList
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShoppingListOverviewTest {
    private val list = ShoppingList(id = 1, name = "Groceries")

    @Test
    fun groupsWhatIsLeftToBuyByCategory() {
        val overview = buildShoppingOverview(
            list,
            listOf(
                TestData.shoppingItem(id = 1, name = "yogurt"),
                TestData.shoppingItem(id = 2, name = "Pasta", category = null),
                TestData.shoppingItem(id = 3, name = "Éclairs"),
                TestData.shoppingItem(id = 4, name = "Apples", category = TestData.produce),
                TestData.shoppingItem(id = 5, name = "Butter"),
            ),
        )

        assertEquals(
            listOf(
                TestData.dairy to listOf("Butter", "Éclairs", "yogurt"),
                TestData.produce to listOf("Apples"),
                null to listOf("Pasta"),
            ),
            overview.aisles.map { aisle -> aisle.category to aisle.items.map { it.name } },
        )
        assertEquals(5, overview.toBuyCount)
        assertEquals(emptyList(), overview.inCart)
    }

    @Test
    fun followsTheOrderOfTheShop() {
        val bakery = Category(id = 3, name = "Bakery", emoji = "🍞")
        val items = listOf(
            TestData.shoppingItem(id = 1, name = "Milk"),
            TestData.shoppingItem(id = 2, name = "Bread", category = bakery),
            TestData.shoppingItem(id = 3, name = "Apples", category = TestData.produce),
            TestData.shoppingItem(id = 4, name = "Pasta", category = null),
        )

        // Produce first, then dairy; bakery isn't placed, so it comes after them.
        val overview =
            buildShoppingOverview(list, items, aisleOrder = listOf(TestData.produce.id, 99, TestData.dairy.id))

        assertEquals(
            listOf(TestData.produce, TestData.dairy, bakery, null),
            overview.aisles.map { it.category },
        )
    }

    @Test
    fun putsTheCartLastPutInFirst() {
        val overview = buildShoppingOverview(
            list,
            listOf(
                TestData.shoppingItem(id = 1, name = "Milk", checkedAt = TestTime.now),
                TestData.shoppingItem(id = 2, name = "Bread", checkedAt = TestTime.now.plusSeconds(60)),
                TestData.shoppingItem(id = 3, name = "Eggs"),
            ),
        )

        assertEquals(listOf("Bread", "Milk"), overview.inCart.map { it.name })
        assertEquals(1, overview.toBuyCount)
    }

    @Test
    fun anEmptyListHasNothing() {
        val overview = buildShoppingOverview(list, emptyList())

        assertTrue(overview.isEmpty)
        assertEquals(0, overview.toBuyCount)
    }
}
