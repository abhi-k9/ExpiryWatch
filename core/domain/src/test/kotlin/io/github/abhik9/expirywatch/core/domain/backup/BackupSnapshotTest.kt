package io.github.abhik9.expirywatch.core.domain.backup

import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ShoppingHistoryEntry
import io.github.abhik9.expirywatch.core.model.ShoppingList
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.testing.TestData
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupSnapshotTest {
    @Test
    fun shoppingListsMoveToTheCategoriesAndLocationsOfTheSameName() {
        val oldDairy = Category(id = 7, name = "DAIRY", emoji = "🥛")
        val oldSnacks = Category(id = 8, name = "Snacks", emoji = "🍫")
        val oldFridge = StorageLocation(id = 5, name = "Fridge", emoji = "🧊")
        val shopping = ShoppingBackup(
            lists = listOf(ShoppingList(id = 1, name = "Groceries")),
            items = listOf(
                TestData.shoppingItem(id = 1, name = "Milk", category = oldDairy).copy(location = oldFridge),
                TestData.shoppingItem(id = 2, name = "Crisps", category = oldSnacks),
            ),
            history = listOf(ShoppingHistoryEntry(name = "Milk", category = oldDairy)),
            aisleOrder = listOf(8L, 7L),
        )

        val relinked = shopping.relinkedTo(listOf(TestData.dairy, TestData.produce), listOf(TestData.fridge))

        assertEquals(shopping.lists, relinked.lists)
        assertEquals(listOf("Milk", "Crisps"), relinked.items.map { it.name })
        assertEquals(listOf(TestData.dairy, null), relinked.items.map { it.category })
        assertEquals(listOf(TestData.fridge, null), relinked.items.map { it.location })
        assertEquals(TestData.dairy, relinked.history.single().category)
        // It named the old categories' ids.
        assertEquals(emptyList(), relinked.aisleOrder)
    }
}
