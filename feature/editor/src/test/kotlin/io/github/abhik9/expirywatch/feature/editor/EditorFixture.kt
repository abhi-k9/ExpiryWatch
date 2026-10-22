package io.github.abhik9.expirywatch.feature.editor

import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.domain.shopping.AddItemsToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.GetShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveKnownProductsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.DeleteItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.LookupProductUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveLabelsByUseUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.RecentlyDeletedItems
import io.github.abhik9.expirywatch.core.domain.usecase.SaveItemUseCase
import io.github.abhik9.expirywatch.core.navigation.EditorNavKey
import io.github.abhik9.expirywatch.core.testing.TestData
import io.github.abhik9.expirywatch.core.testing.TestTime
import io.github.abhik9.expirywatch.core.testing.repository.FakeCategoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeCustomGroupRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeItemRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeProductCatalog
import io.github.abhik9.expirywatch.core.testing.repository.FakeProductHistoryRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeShoppingRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeStorageLocationRepository
import io.github.abhik9.expirywatch.core.testing.repository.FakeUserSettingsRepository

/** The fakes behind an [EditorViewModel], for tests. */
internal class EditorFixture {
    val items = FakeItemRepository()
    val history = FakeProductHistoryRepository()
    val catalog = FakeProductCatalog()
    val settings = FakeUserSettingsRepository()
    val shopping = FakeShoppingRepository()
    val categories = FakeCategoryRepository(listOf(TestData.dairy, TestData.produce))
    val locations = FakeStorageLocationRepository(listOf(TestData.fridge))
    val groups = FakeCustomGroupRepository()

    val recentlyDeleted = RecentlyDeletedItems()

    fun viewModel(key: EditorNavKey = EditorNavKey()) = EditorViewModel(
        key = key,
        categoryRepository = categories,
        locationRepository = locations,
        groupRepository = groups,
        observeLabelsByUse = ObserveLabelsByUseUseCase(categories, locations, groups, items, TestTime.clock),
        itemRepository = items,
        saveItem = SaveItemUseCase(items, history, TestTime.clock),
        finishItem = FinishItemUseCase(items, TestTime.clock),
        deleteItem = DeleteItemUseCase(items, recentlyDeleted),
        lookupProduct = LookupProductUseCase(history, catalog, settings, EventLog.NONE),
        addItemsToShoppingList = AddItemsToShoppingListUseCase(
            GetShoppingListUseCase(shopping, settings, "Shopping list", TestTime.clock),
            AddToShoppingListUseCase(
                shopping,
                ObserveKnownProductsUseCase(shopping, items, TestTime.clock),
                TestTime.clock,
            ),
        ),
        clock = TestTime.clock,
    )
}
