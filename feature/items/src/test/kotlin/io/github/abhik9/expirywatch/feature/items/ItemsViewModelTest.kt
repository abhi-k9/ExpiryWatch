package io.github.abhik9.expirywatch.feature.items

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.domain.shopping.AddItemsToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.GetShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveKnownProductsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.MoveItemsToGroupUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveItemsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveLabelsByUseUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.RestoreItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeLength
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeRemindersUseCase
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeCategoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeCustomGroupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeStorageLocationRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test

class ItemsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val groups = FakeCustomGroupRepository(listOf(CustomGroup(id = 1, name = "Baby", emoji = "🍼")))
    private val items = FakeItemRepository(
        groups = groups,
        initialItems = listOf(
            TestData.item(id = 1, name = "Milk", expiresInDays = 1),
            TestData.item(id = 2, name = "Yogurt", expiresInDays = -1),
            TestData.item(id = 3, name = "Apples", expiresInDays = 12, category = TestData.produce),
        ),
    )
    private val settings = FakeUserSettingsRepository()
    private val shopping = FakeShoppingRepository()

    private fun viewModel(initialStatus: ExpiryStatus? = null) = ItemsViewModel(
        initialStatus = initialStatus,
        observeItems = ObserveItemsUseCase(items, settings, ObserveTodayUseCase(TestTime.clock)),
        categoryRepository = FakeCategoryRepository(listOf(TestData.dairy, TestData.produce)),
        locationRepository = FakeStorageLocationRepository(listOf(TestData.fridge)),
        settingsRepository = settings,
        finishItem = FinishItemUseCase(items, TestTime.clock),
        restoreItem = RestoreItemUseCase(items),
        addItemsToShoppingList = AddItemsToShoppingListUseCase(
            GetShoppingListUseCase(shopping, settings, "Shopping list", TestTime.clock),
            AddToShoppingListUseCase(
                shopping,
                ObserveKnownProductsUseCase(shopping, items, TestTime.clock),
                TestTime.clock,
            ),
        ),
        snoozeReminders = SnoozeRemindersUseCase(items),
        moveItemsToGroup = MoveItemsToGroupUseCase(items),
        groupRepository = groups,
        observeLabelsByUse = ObserveLabelsByUseUseCase(
            FakeCategoryRepository(),
            FakeStorageLocationRepository(),
            groups,
            items,
            TestTime.clock,
        ),
    )

    private suspend fun ItemsViewModel.successState(): ItemsUiState.Success =
        uiState.filterIsInstance<ItemsUiState.Success>().first()

    /** Waits (in virtual time) until the list shows exactly [names]. */
    private suspend fun ItemsViewModel.awaitItemNames(vararg names: String) {
        withTimeout(5.seconds) {
            uiState.first { state ->
                state is ItemsUiState.Success && state.overview.items.map { it.item.name } == names.toList()
            }
        }
    }

    /** Selects or unselects the items with [ids] together, as a product group does. */
    private fun ItemsViewModel.toggle(vararg ids: Long) {
        toggleSelection(items.currentItems.filter { it.id in ids })
        Snapshot.sendApplyNotifications()
    }

    private suspend fun ItemsViewModel.awaitSelection(vararg ids: Long): ItemsUiState.Success = withTimeout(5.seconds) {
        uiState.filterIsInstance<ItemsUiState.Success>().first { it.selection == ids.toSet() }
    }

    private fun snoozes(): Map<Long, LocalDate?> = items.currentItems.associate { it.id to it.remindersSnoozedUntil }

    @Test
    fun showsActiveItemsSoonestFirst() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.awaitItemNames("Yogurt", "Milk", "Apples")
        assertEquals(2, viewModel.successState().categories.size)
    }

    @Test
    fun startsWithTheStatusFilterItWasOpenedWith() = runTest {
        val viewModel = viewModel(initialStatus = ExpiryStatus.EXPIRED)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.awaitItemNames("Yogurt")
    }

    @Test
    fun filtersBySearchAndCategory() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.onCategoryFilterChange(TestData.produce.id)
        viewModel.awaitItemNames("Apples")

        viewModel.clearFilters()
        viewModel.onSearchTextChange("yog")
        Snapshot.sendApplyNotifications()
        viewModel.awaitItemNames("Yogurt")
    }

    @Test
    fun sortOrderIsSavedToSettings() = runTest {
        viewModel().onSortOrderChange(ItemSortOrder.NAME)
        assertEquals(ItemSortOrder.NAME, settings.current.sortOrder)
    }

    @Test
    fun finishingAnItemCanBeUndone() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.awaitItemNames("Yogurt", "Milk", "Apples")
        val milk = viewModel.successState().overview.items.first { it.item.name == "Milk" }.item

        viewModel.events.test {
            viewModel.finish(milk, ItemStatus.CONSUMED)

            val event = assertIs<ItemsEvent.ItemFinished>(awaitItem())
            assertEquals(ItemStatus.CONSUMED, event.outcome)
            assertEquals(ItemStatus.CONSUMED, items.currentItems.first { it.id == milk.id }.status)
        }

        viewModel.undoFinish(milk)
        assertEquals(ItemStatus.ACTIVE, items.currentItems.first { it.id == milk.id }.status)
    }

    @Test
    fun productGroupsOpenAndClose() = runTest {
        items.upsert(TestData.item(name = "Milk", expiresInDays = 3))
        val viewModel = viewModel()
        val milk = viewModel.successState().overview.sections.flatMap { it.groups }.single { it.entries.size > 1 }
        assertEquals(listOf(1L, 4L), milk.entries.map { it.item.id })

        viewModel.onGroupClick(milk.key)
        assertEquals(setOf(milk.key), viewModel.expandedGroups)

        viewModel.onGroupClick(milk.key)
        assertEquals(emptySet(), viewModel.expandedGroups)
    }

    @Test
    fun putsItemsOnTheShoppingList() = runTest {
        val viewModel = viewModel()
        val (milk, yogurt) = listOf(1L, 2L).map { id -> items.currentItems.single { it.id == id } }

        viewModel.events.test {
            viewModel.addToShoppingList(listOf(yogurt, milk))

            val added = assertIs<ItemsEvent.PutOnShoppingList>(awaitItem()).added
            assertEquals("Shopping list", added.list.name)
            assertEquals(listOf("Yogurt", "Milk"), added.names)
        }
        assertEquals(setOf("Yogurt", "Milk"), shopping.currentItems.map { it.name }.toSet())
    }

    @Test
    fun selectsItemsAndWholeProductGroups() = runTest {
        items.upsert(TestData.item(name = "Milk", expiresInDays = 3))
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.awaitItemNames("Yogurt", "Milk", "Milk", "Apples")
        assertFalse(viewModel.successState().isSelecting)

        // A long press on the Milk group selects both of its items; one of them is then unselected.
        viewModel.toggle(1, 4)
        assertTrue(viewModel.awaitSelection(1, 4).isSelecting)
        viewModel.toggle(1)
        viewModel.awaitSelection(4)

        viewModel.selectAll()
        Snapshot.sendApplyNotifications()
        viewModel.awaitSelection(1, 2, 3, 4)

        // Listing something else starts over.
        viewModel.onStatusFilterChange(ExpiryStatus.EXPIRED)
        Snapshot.sendApplyNotifications()
        viewModel.awaitSelection()
    }

    @Test
    fun snoozesTheSelectedItemsRemindersWithUndo() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.awaitItemNames("Yogurt", "Milk", "Apples")
        viewModel.toggle(1, 2)
        viewModel.awaitSelection(1, 2)

        val weekLater = TestTime.today.plusWeeks(1)
        viewModel.events.test {
            viewModel.snoozeSelected(SnoozeLength.ONE_WEEK)

            val event = assertIs<ItemsEvent.RemindersSnoozed>(awaitItem())
            assertEquals(weekLater, event.until)
            assertEquals(setOf("Yogurt", "Milk"), event.before.map { it.name }.toSet())
            assertEquals(mapOf(1L to weekLater, 2L to weekLater, 3L to null), snoozes())
            Snapshot.sendApplyNotifications()
            viewModel.awaitSelection()

            viewModel.undoSnooze(event.before)
            assertEquals(mapOf(1L to null, 2L to null, 3L to null), snoozes())
        }
    }

    @Test
    fun snoozesUntilADateAndTurnsRemindersBackOn() = runTest {
        items.snoozeReminders(listOf(2L), Item.SNOOZED_INDEFINITELY)
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.awaitItemNames("Yogurt", "Milk", "Apples")

        viewModel.toggle(3)
        viewModel.awaitSelection(3)
        viewModel.snoozeSelectedUntil(TestTime.today.plusDays(10))
        assertEquals(TestTime.today.plusDays(10), snoozes()[3L])

        viewModel.toggle(2)
        viewModel.awaitSelection(2)
        viewModel.resumeSelectedReminders()
        assertEquals(null, snoozes()[2L])
    }

    @Test
    fun putsTheSelectedItemsOnTheShoppingList() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.awaitItemNames("Yogurt", "Milk", "Apples")
        viewModel.toggle(2, 3)
        viewModel.awaitSelection(2, 3)

        viewModel.addSelectedToShoppingList()
        Snapshot.sendApplyNotifications()

        assertEquals(setOf("Yogurt", "Apples"), shopping.currentItems.map { it.name }.toSet())
        viewModel.awaitSelection()
    }

    @Test
    fun movesTheSelectedItemsToAGroupWithUndo() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.awaitItemNames("Yogurt", "Milk", "Apples")
        val baby = groups.currentGroups.single()
        viewModel.toggle(1, 2)
        viewModel.awaitSelection(1, 2)

        viewModel.events.test {
            viewModel.moveSelectedToGroup(baby)

            val moved = assertIs<ItemsEvent.MovedToGroup>(awaitItem())
            assertEquals(baby, moved.group)
            assertEquals(mapOf(1L to "Baby", 2L to "Baby", 3L to null), groupNames())

            viewModel.undoMove(moved.before)
            assertEquals(mapOf(1L to null, 2L to null, 3L to null), groupNames())
        }
    }

    @Test
    fun movesTheSelectedItemsToANewGroup() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.awaitItemNames("Yogurt", "Milk", "Apples")
        viewModel.toggle(3)
        viewModel.awaitSelection(3)

        viewModel.moveSelectedToNewGroup(" Fruit bowl ", "🍎")

        val fruitBowl = groups.currentGroups.single { it.name == "Fruit bowl" }
        assertEquals(fruitBowl, items.currentItems.single { it.id == 3L }.customGroup)
        Snapshot.sendApplyNotifications()
        viewModel.awaitSelection()
    }

    private fun groupNames(): Map<Long, String?> = items.currentItems.associate { it.id to it.customGroup?.name }
}
