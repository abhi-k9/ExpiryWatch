package io.github.abhik9.expirywatch.feature.items

import io.github.abhik9.expirywatch.core.domain.shopping.AddItemsToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.GetShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveKnownProductsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.DeleteItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.MoveItemsToGroupUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveItemsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveLabelsByUseUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveReminderProblemsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveTodayUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.RecentlyDeletedItems
import io.github.abhik9.expirywatch.core.domain.usecase.RestoreItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeRemindersUseCase
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeCategoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeCustomGroupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeReminderHealthMonitor
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeStorageLocationRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository

/** The fakes behind an [ItemsViewModel] with three items, for tests. */
internal class ItemsFixture {
    val groups = FakeCustomGroupRepository(listOf(CustomGroup(id = 1, name = "Baby", emoji = "🍼")))
    val items = FakeItemRepository(
        groups = groups,
        initialItems = listOf(
            TestData.item(id = 1, name = "Milk", expiresInDays = 1),
            TestData.item(id = 2, name = "Yogurt", expiresInDays = -1),
            TestData.item(id = 3, name = "Apples", expiresInDays = 12, category = TestData.produce),
        ),
    )
    val settings = FakeUserSettingsRepository()
    val shopping = FakeShoppingRepository()
    val reminderHealth = FakeReminderHealthMonitor()
    val recentlyDeleted = RecentlyDeletedItems()

    fun viewModel(initialStatus: ExpiryStatus? = null) = ItemsViewModel(
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
        observeReminderProblems = ObserveReminderProblemsUseCase(reminderHealth, settings, TestTime.clock),
        recentlyDeleted = recentlyDeleted,
        deleteItem = DeleteItemUseCase(items, recentlyDeleted),
    )
}
