package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class AddToShoppingListUseCaseTest {
    private val shopping = FakeShoppingRepository()
    private val items = FakeItemRepository()
    private val addToList = AddToShoppingListUseCase(
        shopping,
        ObserveKnownProductsUseCase(shopping, items, TestTime.clock),
        TestTime.clock,
    )

    private suspend fun add(text: String, listId: Long = 1) = addToList(listId, checkNotNull(ShoppingEntry.parse(text)))

    @Test
    fun addsANewItemAndRemembersIt() = runTest {
        val result = add("2 milk")

        val added = assertIs<AddToShoppingListResult.Added>(result).item
        assertEquals(shopping.currentItems.single(), added)
        assertEquals("milk", added.name)
        assertEquals(2.0, added.quantity)
        assertEquals(TestTime.now, added.createdAt)
        assertEquals(1, shopping.findInHistory("Milk")?.timesAdded)
    }

    @Test
    fun aProductAddedBeforeKeepsItsNameAndCategory() = runTest {
        shopping.rememberAdded("Crème fraîche", TestData.dairy, TestTime.now)

        val added = add("creme  FRAICHE").item

        assertEquals("Crème fraîche", added.name)
        assertEquals(TestData.dairy, added.category)
        assertEquals(2, shopping.findInHistory("crème fraîche")?.timesAdded)
    }

    @Test
    fun aProductKeptAsAnItemGoesInTheItemsCategory() = runTest {
        items.upsert(TestData.item(name = "Apples", category = TestData.produce))

        val added = add("3 apples").item

        assertEquals("Apples", added.name)
        assertEquals(TestData.produce, added.category)
        assertEquals(TestData.produce, shopping.findInHistory("apples")?.category)
    }

    @Test
    fun addsToTheQuantityOfAnItemAlreadyOnTheList() = runTest {
        add("2 eggs")

        val result = add("Eggs x6")

        assertEquals(8.0, assertIs<AddToShoppingListResult.AlreadyOnList>(result).item.quantity)
        assertEquals("eggs", shopping.currentItems.single().name)
        assertEquals(8.0, shopping.currentItems.single().quantity)
    }

    @Test
    fun aPlainOneGivesWayToAnAmountInAnotherUnit() = runTest {
        add("flour")

        add("1 kg flour")
        assertEquals(1.0 to QuantityUnit.KILOGRAMS, shopping.currentItems.single().let { it.quantity to it.unit })

        // Another unit doesn't add up, so the amount on the list stays.
        add("500 g flour")
        assertEquals(1.0 to QuantityUnit.KILOGRAMS, shopping.currentItems.single().let { it.quantity to it.unit })
    }

    @Test
    fun putsAnItemFromTheCartBackOnTheList() = runTest {
        val milk = add("Milk").item
        shopping.setChecked(listOf(milk.id), TestTime.now)

        val result = add("2 milk")

        val back = assertIs<AddToShoppingListResult.BackOnList>(result).item
        assertNull(back.checkedAt)
        assertEquals(2.0, back.quantity)
        assertEquals(listOf(back), shopping.currentItems)
    }

    @Test
    fun fillsInDetailsTheItemDoesNotHaveYet() = runTest {
        add("Apples")

        addToList(1, ShoppingEntry("apples", category = TestData.produce, note = "Granny Smith"))

        val apples = shopping.currentItems.single()
        assertEquals(TestData.produce, apples.category)
        assertEquals("Granny Smith", apples.note)
        assertEquals(TestData.produce, shopping.findInHistory("apples")?.category)
    }

    @Test
    fun keepsListsApart() = runTest {
        add("Milk", listId = 1)

        val result = add("Milk", listId = 2)

        assertIs<AddToShoppingListResult.Added>(result)
        assertEquals(2, shopping.currentItems.size)
    }

    @Test
    fun rejectsABlankName() = runTest {
        assertFailsWith<IllegalArgumentException> { addToList(1, ShoppingEntry(" ")) }
        assertEquals(emptyList(), shopping.currentItems)
    }
}
