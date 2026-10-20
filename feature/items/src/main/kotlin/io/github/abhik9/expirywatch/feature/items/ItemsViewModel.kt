package io.github.abhik9.expirywatch.feature.items

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.domain.repository.CategoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.CustomGroupRepository
import io.github.abhik9.expirywatch.core.domain.repository.StorageLocationRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.shopping.AddItemsToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.AddedToShoppingList
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ItemQuery
import io.github.abhik9.expirywatch.core.domain.usecase.ItemsOverview
import io.github.abhik9.expirywatch.core.domain.usecase.MoveItemsToGroupUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveItemsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveLabelsByUseUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.RestoreItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeLength
import io.github.abhik9.expirywatch.core.domain.usecase.SnoozeRemindersUseCase
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.CustomGroup
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.StorageLocation
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ItemsUiState {
    data object Loading : ItemsUiState

    data class Success(
        val overview: ItemsOverview,
        val query: ItemQuery,
        val categories: List<Category>,
        val locations: List<StorageLocation>,
        /** The ids of the listed items picked to act on together; empty when not selecting. */
        val selection: Set<Long> = emptySet(),
    ) : ItemsUiState {
        val isSelecting: Boolean get() = selection.isNotEmpty()
    }
}

/** One-off messages for the screen, such as the undo snackbar. */
sealed interface ItemsEvent {
    data class ItemFinished(val item: Item, val outcome: ItemStatus) : ItemsEvent

    data class PutOnShoppingList(val added: AddedToShoppingList) : ItemsEvent

    /**
     * Reminders about [before] were snoozed until [until], or turned back on when it's `null`.
     * [before] holds the items as they were, to undo it.
     */
    data class RemindersSnoozed(val before: List<Item>, val until: LocalDate?) : ItemsEvent

    /**
     * [before] was moved to [group], or out of any group when it's `null`. [before] holds the items
     * as they were, to undo it.
     */
    data class MovedToGroup(val before: List<Item>, val group: CustomGroup?) : ItemsEvent
}

@HiltViewModel(assistedFactory = ItemsViewModel.Factory::class)
class ItemsViewModel @AssistedInject constructor(
    @Assisted initialStatus: ExpiryStatus?,
    observeItems: ObserveItemsUseCase,
    categoryRepository: CategoryRepository,
    locationRepository: StorageLocationRepository,
    private val settingsRepository: UserSettingsRepository,
    private val finishItem: FinishItemUseCase,
    private val restoreItem: RestoreItemUseCase,
    private val addItemsToShoppingList: AddItemsToShoppingListUseCase,
    private val snoozeReminders: SnoozeRemindersUseCase,
    private val moveItemsToGroup: MoveItemsToGroupUseCase,
    private val groupRepository: CustomGroupRepository,
    observeLabelsByUse: ObserveLabelsByUseUseCase,
) : ViewModel() {
    /** Held as Compose state so the search field updates synchronously while typing. */
    var searchText by mutableStateOf("")
        private set

    /** Keys of the product groups opened to show each of their items. */
    var expandedGroups by mutableStateOf(emptySet<String>())
        private set

    /** The ids of the items picked to act on together; see [ItemsUiState.Success.selection]. */
    private var selectedIds by mutableStateOf(emptySet<Long>())

    private val filters = MutableStateFlow(ItemQuery(status = initialStatus))

    private val query: Flow<ItemQuery> = combine(filters, snapshotFlow { searchText }) { currentFilters, text ->
        currentFilters.copy(searchText = text)
    }

    val uiState: StateFlow<ItemsUiState> = combine(
        observeItems(query),
        query,
        categoryRepository.observeCategories(),
        locationRepository.observeLocations(),
        snapshotFlow { selectedIds },
    ) { overview, currentQuery, categories, locations, selected ->
        // Items no longer listed, such as ones used up meanwhile, aren't selected any more.
        val selection = overview.items.mapNotNullTo(mutableSetOf()) { entry -> entry.item.id.takeIf { it in selected } }
        ItemsUiState.Success(overview, currentQuery, categories, locations, selection)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ItemsUiState.Loading,
    )

    /** The user's groups to move items to, the ones used most and lately first. */
    val groups: StateFlow<List<CustomGroup>> = observeLabelsByUse()
        .map { it.groups }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _events = Channel<ItemsEvent>(Channel.BUFFERED)
    val events: Flow<ItemsEvent> = _events.receiveAsFlow()

    fun onSearchTextChange(text: String) {
        searchText = text
    }

    // Changing what's listed starts the selection over.
    fun onStatusFilterChange(status: ExpiryStatus?) {
        clearSelection()
        filters.update { it.copy(status = status) }
    }

    fun onCategoryFilterChange(categoryId: Long?) {
        clearSelection()
        filters.update { it.copy(categoryId = categoryId) }
    }

    fun onLocationFilterChange(locationId: Long?) {
        clearSelection()
        filters.update { it.copy(locationId = locationId) }
    }

    fun clearFilters() {
        clearSelection()
        searchText = ""
        filters.value = ItemQuery()
    }

    /** Selects [items], such as all of a product group's, or unselects them when they all are. */
    fun toggleSelection(items: List<Item>) {
        val ids = items.map { it.id }
        selectedIds = if (selectedIds.containsAll(ids)) selectedIds - ids.toSet() else selectedIds + ids
    }

    fun selectAll() {
        selectedIds = listedItems().mapTo(mutableSetOf()) { it.id }
    }

    fun clearSelection() {
        selectedIds = emptySet()
    }

    /** Leaves the selected items out of reminders for [length]. */
    fun snoozeSelected(length: SnoozeLength) {
        val today = (uiState.value as? ItemsUiState.Success)?.overview?.today ?: return
        snoozeSelectedUntil(length.resumeDate(today))
    }

    /** Leaves the selected items out of reminders until [date], when they start again. */
    fun snoozeSelectedUntil(date: LocalDate) = changeSelectedReminders(date)

    /** Includes the selected items in reminders again. */
    fun resumeSelectedReminders() = changeSelectedReminders(null)

    private fun changeSelectedReminders(until: LocalDate?) {
        val items = selectedItems()
        clearSelection()
        if (items.isEmpty()) return
        viewModelScope.launch {
            snoozeReminders(items, until)
            _events.send(ItemsEvent.RemindersSnoozed(items, until))
        }
    }

    fun undoSnooze(before: List<Item>) {
        viewModelScope.launch { snoozeReminders.restore(before) }
    }

    /** Moves the selected items to [group], or out of any group when it's `null`. */
    fun moveSelectedToGroup(group: CustomGroup?) {
        val items = selectedItems()
        clearSelection()
        if (items.isEmpty()) return
        viewModelScope.launch {
            moveItemsToGroup(items, group?.id)
            _events.send(ItemsEvent.MovedToGroup(items, group))
        }
    }

    /** Makes a group and moves the selected items to it. */
    fun moveSelectedToNewGroup(name: String, emoji: String) {
        val trimmed = name.trim()
        val items = selectedItems()
        clearSelection()
        if (trimmed.isEmpty() || items.isEmpty()) return
        viewModelScope.launch {
            val group = CustomGroup(name = trimmed, emoji = emoji.ifBlank { DEFAULT_GROUP_EMOJI })
            val id = groupRepository.upsert(group)
            moveItemsToGroup(items, id)
            _events.send(ItemsEvent.MovedToGroup(items, group.copy(id = id)))
        }
    }

    fun undoMove(before: List<Item>) {
        viewModelScope.launch { moveItemsToGroup.restore(before) }
    }

    /** Puts the products of the selected items on the shopping list in use. */
    fun addSelectedToShoppingList() {
        val items = selectedItems()
        clearSelection()
        if (items.isNotEmpty()) addToShoppingList(items)
    }

    private fun listedItems(): List<Item> =
        (uiState.value as? ItemsUiState.Success)?.overview?.items?.map { it.item }.orEmpty()

    private fun selectedItems(): List<Item> = listedItems().filter { it.id in selectedIds }

    fun onGroupClick(key: String) {
        expandedGroups = if (key in expandedGroups) expandedGroups - key else expandedGroups + key
    }

    fun onSortOrderChange(sortOrder: ItemSortOrder) {
        viewModelScope.launch { settingsRepository.setSortOrder(sortOrder) }
    }

    fun finish(item: Item, outcome: ItemStatus) {
        viewModelScope.launch {
            finishItem(item.id, outcome)
            _events.send(ItemsEvent.ItemFinished(item, outcome))
        }
    }

    fun undoFinish(item: Item) {
        viewModelScope.launch { restoreItem(item.id) }
    }

    /** Puts the products of [items] on the shopping list in use. */
    fun addToShoppingList(items: List<Item>) {
        viewModelScope.launch {
            val added = addItemsToShoppingList(items)
            if (added.names.isNotEmpty()) _events.send(ItemsEvent.PutOnShoppingList(added))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialStatus: ExpiryStatus?): ItemsViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DEFAULT_GROUP_EMOJI = "🗂️"
    }
}
