package io.github.abhik9.expirywatch.feature.items

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.domain.repository.ReminderProblem
import io.github.abhik9.expirywatch.core.domain.usecase.DeleteItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeLength
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
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

    private val fixture = ItemsFixture()
    private val groups = fixture.groups
    private val items = fixture.items
    private val settings = fixture.settings
    private val shopping = fixture.shopping
    private val reminderHealth = fixture.reminderHealth
    private val recentlyDeleted = fixture.recentlyDeleted

    private fun viewModel(initialStatus: ExpiryStatus? = null) = fixture.viewModel(initialStatus)

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

    @Test
    fun warnsAboutReminderProblemsUntilDismissedOrNewOnesComeUp() = runTest {
        val viewModel = viewModel()
        viewModel.reminderProblems.test {
            assertEquals(emptyList(), awaitItem())

            reminderHealth.health = reminderHealth.health.copy(notificationsAllowed = false)
            viewModel.recheckReminders()
            assertEquals(listOf(ReminderProblem.NOTIFICATIONS_BLOCKED), awaitItem())

            viewModel.dismissReminderProblems()
            assertEquals(emptyList(), awaitItem())

            reminderHealth.health = reminderHealth.health.copy(backgroundRestricted = true)
            viewModel.recheckReminders()
            assertEquals(
                listOf(ReminderProblem.NOTIFICATIONS_BLOCKED, ReminderProblem.BACKGROUND_RESTRICTED),
                awaitItem(),
            )
        }
    }

    @Test
    fun offersUndoingADeletionInTheEditor() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            val milk = items.observeItem(1).first()!!
            DeleteItemUseCase(items, recentlyDeleted)(milk)

            assertEquals(ItemsEvent.ItemDeleted(milk), awaitItem())
            viewModel.undoDelete(milk)
            assertEquals(milk, items.observeItem(1).first())
        }
    }
}
