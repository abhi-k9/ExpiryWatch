package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class AddItemsToShoppingListUseCaseTest {
    private val shopping = FakeShoppingRepository()
    private val items = FakeItemRepository()
    private val addItems = AddItemsToShoppingListUseCase(
        GetShoppingListUseCase(shopping, FakeUserSettingsRepository(), "Shopping list", TestTime.clock),
        AddToShoppingListUseCase(
            shopping,
            ObserveKnownProductsUseCase(shopping, items, TestTime.clock),
            TestTime.clock,
        ),
    )

    @Test
    fun putsEachProductOnTheListOnceWithItsDetails() = runTest {
        val milk = TestData.item(id = 1, name = "Milk").copy(
            brand = "Alpro",
            barcode = "5411188112709",
            quantity = 1.0,
            unit = QuantityUnit.LITERS,
        )

        val added = addItems(listOf(milk, TestData.item(id = 2, name = "milk "), TestData.item(id = 3, name = " ")))

        assertEquals("Shopping list", added.list.name)
        assertEquals(listOf("Milk"), added.names)
        val onList = shopping.currentItems.single()
        assertEquals(added.list.id, onList.listId)
        assertEquals(1.0 to QuantityUnit.LITERS, onList.quantity to onList.unit)
        assertEquals("Alpro", onList.brand)
        assertEquals("5411188112709", onList.barcode)
        assertEquals(TestData.dairy, onList.category)
        assertEquals(TestData.fridge, onList.location)
    }
}
