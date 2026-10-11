package io.github.abhik9.expirywatch.core.domain.shopping

import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class KnownProductsTest {
    @Test
    fun knowsWhatWasAddedBeforeThenWhatIsOrWasKept() = runTest {
        val shopping = FakeShoppingRepository()
        shopping.rememberAdded("Bread", null, TestTime.now)
        shopping.rememberAdded("Milk", TestData.dairy, TestTime.now)
        shopping.rememberAdded("Milk", null, TestTime.now)
        shopping.rememberAdded("Pears", null, TestTime.now)
        val items = FakeItemRepository(
            listOf(
                TestData.item(id = 1, name = "milk"),
                TestData.item(id = 2, name = "Yogurt"),
                TestData.item(id = 3, name = "Cheese").copy(
                    status = ItemStatus.CONSUMED,
                    finishedDate = TestTime.today.minusDays(30),
                ),
                TestData.item(id = 4, name = "Old jam").copy(
                    status = ItemStatus.WASTED,
                    finishedDate = TestTime.today.minusYears(2),
                ),
                TestData.item(id = 5, name = " "),
                TestData.item(id = 6, name = "pears", category = null),
                TestData.item(id = 7, name = "Pears", category = TestData.produce),
            ),
        )

        val known = ObserveKnownProductsUseCase(shopping, items, TestTime.clock)().first()

        assertEquals(
            listOf(
                KnownProduct("Milk", TestData.dairy, timesAdded = 2),
                KnownProduct("Bread", null, timesAdded = 1),
                KnownProduct("Pears", TestData.produce, timesAdded = 1),
                KnownProduct("Cheese", TestData.dairy),
                KnownProduct("Yogurt", TestData.dairy),
            ),
            known,
        )
    }

    private val known = listOf(
        KnownProduct("Milk", timesAdded = 5),
        KnownProduct("Oat milk", timesAdded = 2),
        KnownProduct("Buttermilk", timesAdded = 1),
        KnownProduct("Minced meat"),
        KnownProduct("Bread", timesAdded = 3),
    )

    private fun suggest(query: String, exclude: Set<String> = emptySet(), limit: Int = 8) =
        suggestProducts(query, known, exclude.map(::productKey).toSet(), limit).map { it.name }

    @Test
    fun suggestsNamesStartingWithWhatIsTypedFirst() {
        assertEquals(listOf("Milk", "Minced meat", "Oat milk", "Buttermilk"), suggest("mi"))
        assertEquals(listOf("Milk", "Oat milk", "Buttermilk"), suggest(" MILK"))
        assertEquals(listOf("Bread"), suggest("rea"))
        assertEquals(emptyList(), suggest("cheese"))
    }

    @Test
    fun suggestsWhatIsAddedMostOftenWithoutAQuery() {
        assertEquals(listOf("Milk", "Oat milk", "Buttermilk", "Bread"), suggest(""))
        assertEquals(listOf("Milk", "Oat milk"), suggest("", limit = 2))
    }

    @Test
    fun leavesOutWhatIsAlreadyOnTheList() {
        assertEquals(listOf("Oat milk", "Buttermilk"), suggest("milk", exclude = setOf("milk")))
        assertEquals(listOf("Oat milk", "Buttermilk", "Bread"), suggest("", exclude = setOf("MILK")))
    }
}
