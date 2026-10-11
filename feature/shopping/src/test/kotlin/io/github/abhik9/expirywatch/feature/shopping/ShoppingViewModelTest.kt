package io.github.abhik9.expirywatch.feature.shopping

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListResult
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.CreateShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.DeleteShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.DismissRestockSuggestionUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.GetShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveKnownProductsUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveRestockSuggestionsUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveShoppingListSummariesUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.RenameShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.RestockReason
import io.github.abhik9.expirywatch.core.domain.shopping.SaveShoppingItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeCategoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test

class ShoppingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val shopping = FakeShoppingRepository()
    private val items = FakeItemRepository(
        listOf(TestData.item(id = 1, name = "Milk"), TestData.item(id = 2, name = "Mint tea", category = null)),
    )
    private val settings = FakeUserSettingsRepository()
    private val getShoppingList = GetShoppingListUseCase(shopping, settings, "Shopping list", TestTime.clock)
    private val observeKnownProducts = ObserveKnownProductsUseCase(shopping, items, TestTime.clock)

    // Made on first use in a test, once the rule has set the main dispatcher its scope runs on.
    private val viewModel by lazy {
        ShoppingViewModel(
            observeShoppingList = ObserveShoppingListUseCase(shopping, settings, getShoppingList),
            observeShoppingListSummaries = ObserveShoppingListSummariesUseCase(shopping),
            observeKnownProducts = observeKnownProducts,
            observeRestockSuggestions = ObserveRestockSuggestionsUseCase(
                items,
                shopping,
                settings,
                ObserveTodayUseCase(TestTime.clock),
                TestTime.clock,
            ),
            categoryRepository = FakeCategoryRepository(listOf(TestData.dairy, TestData.produce)),
            getShoppingList = getShoppingList,
            addToShoppingList = AddToShoppingListUseCase(shopping, observeKnownProducts, TestTime.clock),
            saveShoppingItem = SaveShoppingItemUseCase(shopping, TestTime.clock),
            createShoppingList = CreateShoppingListUseCase(shopping, settings, TestTime.clock),
            renameShoppingList = RenameShoppingListUseCase(shopping),
            deleteShoppingList = DeleteShoppingListUseCase(shopping, settings),
            dismissRestockSuggestion = DismissRestockSuggestionUseCase(settings, TestTime.clock),
            shoppingRepository = shopping,
            settingsRepository = settings,
            clock = TestTime.clock,
        )
    }

    /** Waits (in virtual time) for a state matching [predicate]. */
    private suspend fun awaitState(predicate: (ShoppingUiState.Success) -> Boolean): ShoppingUiState.Success =
        withTimeout(5.seconds) { viewModel.uiState.filterIsInstance<ShoppingUiState.Success>().first(predicate) }

    private suspend fun awaitLastAdded(): AddToShoppingListResult = withTimeout(5.seconds) {
        while (viewModel.lastAdded == null) delay(10.milliseconds)
        checkNotNull(viewModel.lastAdded)
    }

    private fun type(text: String) {
        viewModel.onInputChange(text)
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun startsWithAnEmptyListOfItsOwn() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = awaitState { true }

        assertEquals("Shopping list", state.overview.list.name)
        assertTrue(state.overview.isEmpty)
        assertEquals(1, shopping.currentLists.size)
    }

    @Test
    fun addsWhatIsTypedToItsCategoryAndClearsTheField() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        type("2 milk")
        viewModel.add()

        assertEquals("", viewModel.input)
        val aisle = awaitState { it.overview.toBuyCount == 1 }.overview.aisles.single()
        assertEquals(TestData.dairy, aisle.category)
        assertEquals("Milk", aisle.items.single().name)
        assertEquals(2.0, aisle.items.single().quantity)
        assertIs<AddToShoppingListResult.Added>(awaitLastAdded())

        type("milk")
        viewModel.add()
        assertEquals(3.0, assertIs<AddToShoppingListResult.AlreadyOnList>(awaitLastAdded()).item.quantity)

        type("b")
        assertEquals(null, viewModel.lastAdded)
    }

    @Test
    fun suggestsKnownProductsThatAreNotOnTheList() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        type("m")
        val suggestions = awaitState { it.suggestions.size == 2 }.suggestions
        assertEquals(listOf("Milk", "Mint tea"), suggestions.map { it.name })

        viewModel.addSuggestion(suggestions.first())
        type("m")
        assertEquals(listOf("Mint tea"), awaitState { it.suggestions.size == 1 }.suggestions.map { it.name })
    }

    @Test
    fun addsASuggestionInTheAmountTyped() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }

        type("6 mi")
        val mint = awaitState { it.suggestions.size == 2 }.suggestions.single { it.name == "Mint tea" }
        viewModel.addSuggestion(mint)

        val item = awaitState { it.overview.toBuyCount == 1 }.overview.aisles.single().items.single()
        assertEquals("Mint tea", item.name)
        assertEquals(6.0 to QuantityUnit.PIECES, item.quantity to item.unit)
        assertEquals("", viewModel.input)
    }

    @Test
    fun movesItemsToTheCartAndClearsItWithUndo() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        type("Bread")
        viewModel.add()
        val bread = awaitState { it.overview.toBuyCount == 1 }.overview.aisles.single().items.single()

        viewModel.onCheckedChange(bread, checked = true)
        assertEquals(TestTime.now, awaitState { it.overview.inCart.size == 1 }.overview.inCart.single().checkedAt)

        viewModel.events.test {
            viewModel.clearCart()
            val cleared = assertIs<ShoppingEvent.CartCleared>(awaitItem())
            assertEquals(listOf("Bread"), cleared.items.map { it.name })
            awaitState { it.overview.isEmpty }

            viewModel.undoClearCart(cleared.items)
            awaitState { it.overview.inCart.size == 1 }
        }

        viewModel.onCheckedChange(bread.copy(checkedAt = TestTime.now), checked = false)
        awaitState { it.overview.toBuyCount == 1 }
    }

    @Test
    fun editsAnItemAndRemembersItsNewCategory() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        type("Bread")
        viewModel.add()
        val bread = awaitState { it.overview.toBuyCount == 1 }.overview.aisles.single().items.single()
        assertEquals(listOf(TestData.dairy, TestData.produce), awaitState { true }.categories)

        viewModel.onEdit(bread)
        assertEquals(bread, viewModel.editing)
        viewModel.save(bread.copy(name = "Rye bread ", category = TestData.produce, note = "sliced"))

        assertEquals(null, viewModel.editing)
        val moved = awaitState { it.overview.aisles.singleOrNull()?.category == TestData.produce }
        val aisle = moved.overview.aisles.single()
        assertEquals("Rye bread", aisle.items.single().name)
        assertEquals("sliced", aisle.items.single().note)
        assertEquals(TestData.produce, shopping.findInHistory("rye bread")?.category)
    }

    @Test
    fun removesAnItemWithUndo() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        type("Bread")
        viewModel.add()
        val bread = awaitState { it.overview.toBuyCount == 1 }.overview.aisles.single().items.single()
        viewModel.onEdit(bread)

        viewModel.events.test {
            viewModel.remove(bread)
            assertEquals(ShoppingEvent.ItemRemoved(bread), awaitItem())
            assertEquals(null, viewModel.editing)
            awaitState { it.overview.isEmpty }

            viewModel.undoRemove(bread)
            assertEquals(listOf(bread), awaitState { it.overview.toBuyCount == 1 }.overview.aisles.single().items)
        }
    }

    @Test
    fun switchesBetweenListsAndManagesThem() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        type("Milk")
        viewModel.add()
        awaitState { it.overview.toBuyCount == 1 }

        viewModel.createList("Pharmacy")
        val pharmacy = awaitState { it.overview.list.name == "Pharmacy" }
        assertTrue(pharmacy.overview.isEmpty)
        assertEquals(
            listOf("Shopping list" to 1, "Pharmacy" to 0),
            pharmacy.lists.map { it.list.name to it.toBuyCount },
        )
        type("Plasters")
        viewModel.add()
        awaitState { it.overview.list.name == "Pharmacy" && it.overview.toBuyCount == 1 }

        viewModel.renameList(pharmacy.overview.list.id, "Chemist")
        awaitState { it.overview.list.name == "Chemist" }

        viewModel.switchToList(pharmacy.lists.first().list.id)
        val groceries = awaitState { it.overview.list.name == "Shopping list" }
        assertEquals("Milk", groceries.overview.aisles.single().items.single().name)

        viewModel.deleteList(pharmacy.overview.list.id)
        assertEquals(listOf("Shopping list"), awaitState { it.lists.size == 1 }.lists.map { it.list.name })
    }

    @Test
    fun suggestsExpiredItemsToBuyAgain() = runTest {
        items.upsert(TestData.item(id = 3, name = "Yogurt", expiresInDays = -1))
        items.upsert(TestData.item(id = 4, name = "Cream", expiresInDays = -3))
        backgroundScope.launch { viewModel.uiState.collect {} }

        val restock = awaitState { it.restock.size == 2 }.restock
        assertEquals(listOf("Yogurt", "Cream"), restock.map { it.name })
        assertEquals(RestockReason.EXPIRED, restock.first().reason)

        type("2 eg")
        viewModel.addRestock(restock.first())
        val added = awaitState { it.restock.size == 1 }
        assertEquals(listOf("Yogurt"), added.overview.aisles.single().items.map { it.name })
        // What's typed is left as it is.
        assertEquals("2 eg", viewModel.input)

        viewModel.dismissRestock(restock.last())
        awaitState { it.restock.isEmpty() }
    }

    @Test
    fun addsEverythingSuggestedAtOnce() = runTest {
        items.upsert(TestData.item(id = 3, name = "Yogurt", expiresInDays = -1))
        items.upsert(TestData.item(id = 4, name = "Cream", expiresInDays = -3))
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.addAllRestock(awaitState { it.restock.size == 2 }.restock)

        val state = awaitState { it.restock.isEmpty() }
        assertEquals(setOf("Yogurt", "Cream"), state.overview.aisles.flatMap { it.items }.map { it.name }.toSet())
    }
}
