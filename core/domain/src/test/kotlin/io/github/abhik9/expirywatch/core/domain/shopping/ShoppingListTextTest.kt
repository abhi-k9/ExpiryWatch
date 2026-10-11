package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.ShoppingList
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ShoppingListTextTest {
    private val list = ShoppingList(id = 1, name = "Groceries")

    @Test
    fun writesWhatIsLeftToBuyByAisle() {
        val overview = buildShoppingOverview(
            list,
            listOf(
                TestData.shoppingItem(id = 1, name = "Milk", quantity = 2.0, unit = QuantityUnit.LITERS),
                TestData.shoppingItem(id = 2, name = "Butter", quantity = 250.0, unit = QuantityUnit.GRAMS)
                    .copy(note = "unsalted"),
                TestData.shoppingItem(id = 3, name = "Apples", category = TestData.produce, quantity = 6.0),
                TestData.shoppingItem(
                    id = 4,
                    name = "Pasta",
                    category = null,
                    quantity = 1.5,
                    unit = QuantityUnit.KILOGRAMS,
                ),
                TestData.shoppingItem(id = 5, name = "Bread", checkedAt = TestTime.now),
            ),
        )

        assertEquals(
            """
            Groceries

            🥛 Dairy
            - 250 g Butter (unsalted)
            - 2 l Milk

            🥦 Produce
            - 6 Apples

            Other
            - 1.5 kg Pasta
            """.trimIndent(),
            ShoppingListText.format(overview, noCategory = "Other"),
        )
    }

    @Test
    fun leavesOutTheHeadingWhenNothingHasACategory() {
        val overview = buildShoppingOverview(list, listOf(TestData.shoppingItem(name = "Pasta", category = null)))

        assertEquals("Groceries\n- Pasta", ShoppingListText.format(overview, noCategory = "Other"))
    }

    @Test
    fun readsBackWhatItWrites() {
        val overview = buildShoppingOverview(
            list,
            listOf(
                TestData.shoppingItem(id = 1, name = "Milk", quantity = 2.0, unit = QuantityUnit.LITERS),
                TestData.shoppingItem(
                    id = 2,
                    name = "Pasta",
                    category = null,
                    quantity = 3.0,
                    unit = QuantityUnit.PACKS,
                )
                    .copy(note = "spaghetti"),
            ),
        )

        val entries = ShoppingListText.parse(ShoppingListText.format(overview, "Other"), listOf(TestData.dairy))

        assertEquals(
            listOf(
                ShoppingEntry("Milk", 2.0, QuantityUnit.LITERS, category = TestData.dairy),
                ShoppingEntry("Pasta", 3.0, QuantityUnit.PACKS, note = "spaghetti"),
            ),
            entries,
        )
    }

    @Test
    fun readsPlainAndMarkedUpLists() {
        assertEquals(
            listOf(ShoppingEntry("milk"), ShoppingEntry("eggs", 12.0), ShoppingEntry("bread")),
            ShoppingListText.parse("milk\n\n  eggs x12\nbread  ", emptyList()),
        )
        assertEquals(
            listOf("Milk", "Eggs", "Bread", "Jam", "Tea"),
            ShoppingListText.parse(
                "Shopping\n* Milk\n• Eggs\n[ ] Bread\n☐ Jam\n1. Tea",
                emptyList(),
            ).map { it.name },
        )
    }
}
