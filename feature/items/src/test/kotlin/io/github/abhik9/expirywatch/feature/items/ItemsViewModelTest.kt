package io.github.abhik9.expirywatch.feature.items

import androidx.compose.runtime.snapshots.Snapshot
import app.cash.turbine.test
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveItemsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.RestoreItemUseCase
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.testing.MainDispatcherRule
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeCategoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeStorageLocationRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

    private val items = FakeItemRepository(
        listOf(
            TestData.item(id = 1, name = "Milk", expiresInDays = 1),
            TestData.item(id = 2, name = "Yogurt", expiresInDays = -1),
            TestData.item(id = 3, name = "Apples", expiresInDays = 12, category = TestData.produce),
        ),
    )
    private val settings = FakeUserSettingsRepository()

    private fun viewModel(initialStatus: ExpiryStatus? = null) = ItemsViewModel(
        initialStatus = initialStatus,
        observeItems = ObserveItemsUseCase(items, settings, ObserveTodayUseCase(TestTime.clock)),
        categoryRepository = FakeCategoryRepository(listOf(TestData.dairy, TestData.produce)),
        locationRepository = FakeStorageLocationRepository(listOf(TestData.fridge)),
        settingsRepository = settings,
        finishItem = FinishItemUseCase(items, TestTime.clock),
        restoreItem = RestoreItemUseCase(items),
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
}
