package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ShoppingListUseCasesTest {
    private val shopping = FakeShoppingRepository()
    private val settings = FakeUserSettingsRepository()

    @Test
    fun aNewListIsOpened() = runTest {
        val createList = CreateShoppingListUseCase(shopping, settings, TestTime.clock)

        val id = createList("  Pharmacy ")

        assertEquals("Pharmacy", shopping.currentLists.single { it.id == id }.name)
        assertEquals(id, settings.current.shoppingListId)
        assertNull(createList(" "))
        assertEquals(1, shopping.currentLists.size)
    }

    @Test
    fun listsAreRenamedButNotToNothing() = runTest {
        val renameList = RenameShoppingListUseCase(shopping)
        val id = shopping.createList("Groceries", TestTime.now)

        assertTrue(renameList(id, " Weekly shop "))
        assertFalse(renameList(id, ""))

        assertEquals("Weekly shop", shopping.currentLists.single().name)
    }

    @Test
    fun deletingTheOpenListOpensTheFirstOneLeft() = runTest {
        val deleteList = DeleteShoppingListUseCase(shopping, settings)
        val groceries = shopping.createList("Groceries", TestTime.now)
        val pharmacy = shopping.createList("Pharmacy", TestTime.now)
        val hardware = shopping.createList("Hardware", TestTime.now)
        shopping.upsertItem(TestData.shoppingItem(listId = pharmacy, name = "Plasters"))
        settings.setShoppingListId(pharmacy)

        deleteList(pharmacy)
        assertEquals(listOf(groceries, hardware), shopping.currentLists.map { it.id })
        assertEquals(emptyList(), shopping.currentItems)
        assertEquals(groceries, settings.current.shoppingListId)

        // Deleting another list leaves the open one open.
        deleteList(hardware)
        assertEquals(groceries, settings.current.shoppingListId)
    }
}
