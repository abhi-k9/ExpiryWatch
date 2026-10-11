package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SaveShoppingItemUseCaseTest {
    private val shopping = FakeShoppingRepository()
    private val saveItem = SaveShoppingItemUseCase(shopping, TestTime.clock)

    private suspend fun addMilk() = TestData.shoppingItem(name = "Milk", category = null).let { milk ->
        milk.copy(id = shopping.upsertItem(milk)).also { shopping.rememberAdded(it.name, null, TestTime.now) }
    }

    @Test
    fun savesTheChangesTidiedUp() = runTest {
        val milk = addMilk()

        val saved =
            saveItem(milk, milk.copy(quantity = 2.0, unit = QuantityUnit.LITERS, note = "  ", brand = " Alpro "))

        assertTrue(saved)
        val item = shopping.currentItems.single()
        assertEquals(2.0 to QuantityUnit.LITERS, item.quantity to item.unit)
        assertNull(item.note)
        assertEquals("Alpro", item.brand)
    }

    @Test
    fun remembersANewCategoryOrNameWithoutCountingAnotherAddition() = runTest {
        val milk = addMilk()

        saveItem(milk, milk.copy(category = TestData.dairy))
        assertEquals(TestData.dairy, shopping.findInHistory("milk")?.category)
        assertEquals(1, shopping.findInHistory("milk")?.timesAdded)

        saveItem(milk, milk.copy(name = " Oat milk ", category = TestData.dairy))
        assertEquals("Oat milk", shopping.currentItems.single().name)
        assertEquals(TestData.dairy, shopping.findInHistory("oat milk")?.category)
    }

    @Test
    fun keepsTheItemWithoutANameOrWithAWrongAmount() = runTest {
        val milk = addMilk()

        assertFalse(saveItem(milk, milk.copy(name = " ")))
        assertFalse(saveItem(milk, milk.copy(quantity = 0.0)))
        assertFalse(saveItem(milk, milk.copy(quantity = Double.NaN)))

        assertEquals(listOf(milk), shopping.currentItems)
    }
}
