package io.github.abhik9.expirywatch.feature.shopping.putaway

import io.github.abhik9.expirywatch.core.domain.shopping.GetPutAwayDraftsUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.PutAwayUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.SaveItemUseCase
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeProductHistoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeStorageLocationRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PutAwayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = FakeItemRepository(listOf(TestData.item(id = 1, name = "Milk", expiresInDays = 4)))
    private val shopping = FakeShoppingRepository()

    private fun viewModel(listId: Long) = PutAwayViewModel(
        listId = listId,
        getPutAwayDrafts = GetPutAwayDraftsUseCase(shopping, items, TestTime.clock),
        locationRepository = FakeStorageLocationRepository(listOf(TestData.fridge, TestData.pantry)),
        putAway = PutAwayUseCase(SaveItemUseCase(items, FakeProductHistoryRepository(), TestTime.clock), shopping),
    )

    private suspend fun buy(listId: Long, name: String) =
        shopping.upsertItem(TestData.shoppingItem(listId = listId, name = name, checkedAt = TestTime.now))

    @Test
    fun putsAwayWhatIsIncludedOnceEachHasADate() = runTest {
        val listId = shopping.createList("Groceries", TestTime.now)
        val milk = buy(listId, "Milk")
        val bread = buy(listId, "Bread")
        val eggs = buy(listId, "Eggs")
        val viewModel = viewModel(listId)

        val editing = assertIs<PutAwayState.Editing>(viewModel.state)
        assertEquals(listOf("Bread", "Eggs", "Milk"), editing.rows.map { it.draft.bought.name })
        assertEquals(TestTime.today.plusDays(4), editing.rows.last().draft.expiryDate)

        // The bread has no date yet, so nothing is put away.
        viewModel.save()
        assertTrue(assertIs<PutAwayState.Editing>(viewModel.state).showErrors)

        viewModel.setExpiryDate(bread, TestTime.today.plusDays(3))
        viewModel.setIncluded(eggs, false)
        viewModel.setLocation(milk, TestData.pantry)
        viewModel.save()

        assertEquals(PutAwayState.Done(2), viewModel.state)
        assertEquals(listOf("Eggs"), shopping.currentItems.map { it.name })
        assertEquals(TestData.pantry, items.currentItems.single { it.name == "Milk" && it.id != 1L }.location)
    }

    @Test
    fun anEmptyCartHasNothingToPutAway() = runTest {
        val listId = shopping.createList("Groceries", TestTime.now)
        shopping.upsertItem(TestData.shoppingItem(listId = listId, name = "Milk"))

        val editing = assertIs<PutAwayState.Editing>(viewModel(listId).state)

        assertEquals(emptyList(), editing.rows)
        assertEquals(false, editing.canSave)
    }
}
