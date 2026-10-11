package io.github.abhik9.expirywatch.feature.shopping.text

import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.GetShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveKnownProductsUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveShoppingListSummariesUseCase
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeCategoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test

class AddFromTextViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val shopping = FakeShoppingRepository()
    private val settings = FakeUserSettingsRepository()
    private val items = FakeItemRepository()

    private fun viewModel(text: String) = AddFromTextViewModel(
        initialText = text,
        observeShoppingListSummaries = ObserveShoppingListSummariesUseCase(shopping),
        categoryRepository = FakeCategoryRepository(listOf(TestData.dairy, TestData.produce)),
        getShoppingList = GetShoppingListUseCase(shopping, settings, "Shopping list", TestTime.clock),
        addToShoppingList = AddToShoppingListUseCase(
            shopping,
            ObserveKnownProductsUseCase(shopping, items, TestTime.clock),
            TestTime.clock,
        ),
        settingsRepository = settings,
    )

    @Test
    fun addsWhatTheTextSaysToTheChosenList() = runTest {
        val groceries = shopping.createList("Groceries", TestTime.now)
        val pharmacy = shopping.createList("Pharmacy", TestTime.now)
        val viewModel = viewModel("Weekend\n\n🥛 Dairy\n- 2 l milk\n- butter (unsalted)")
        backgroundScope.launch { viewModel.entries.collect {} }
        assertEquals(groceries, viewModel.listId)

        val entries = withTimeout(5.seconds) { viewModel.entries.first { it.size == 2 } }
        assertEquals(listOf("milk", "butter"), entries.map { it.name })

        viewModel.onListChange(pharmacy)
        viewModel.events.test {
            viewModel.add()
            assertEquals(AddFromTextEvent.Added, awaitItem())
        }

        val milk = shopping.currentItems.single { it.name == "milk" }
        assertEquals(pharmacy, milk.listId)
        assertEquals(2.0 to QuantityUnit.LITERS, milk.quantity to milk.unit)
        assertEquals(TestData.dairy, milk.category)
        assertEquals("unsalted", shopping.currentItems.single { it.name == "butter" }.note)
        assertEquals(pharmacy, settings.current.shoppingListId)
    }

    @Test
    fun hasNothingToAddFromBlankText() = runTest {
        val viewModel = viewModel("")
        backgroundScope.launch { viewModel.entries.collect {} }

        viewModel.add()

        assertEquals(emptyList(), viewModel.entries.value)
        assertEquals(emptyList(), shopping.currentItems)
    }
}
