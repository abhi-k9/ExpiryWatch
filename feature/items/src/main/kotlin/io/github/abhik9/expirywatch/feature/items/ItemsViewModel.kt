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
import io.github.abhik9.expirywatch.core.domain.repository.StorageLocationRepository
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ItemQuery
import io.github.abhik9.expirywatch.core.domain.usecase.ItemsOverview
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveItemsUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.RestoreItemUseCase
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ExpiryStatus
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemSortOrder
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.StorageLocation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    ) : ItemsUiState
}

/** One-off messages for the screen, such as the undo snackbar. */
sealed interface ItemsEvent {
    data class ItemFinished(val item: Item, val outcome: ItemStatus) : ItemsEvent
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
) : ViewModel() {
    /** Held as Compose state so the search field updates synchronously while typing. */
    var searchText by mutableStateOf("")
        private set

    /** Keys of the product groups opened to show each of their items. */
    var expandedGroups by mutableStateOf(emptySet<String>())
        private set

    private val filters = MutableStateFlow(ItemQuery(status = initialStatus))

    private val query: Flow<ItemQuery> = combine(filters, snapshotFlow { searchText }) { currentFilters, text ->
        currentFilters.copy(searchText = text)
    }

    val uiState: StateFlow<ItemsUiState> = combine(
        observeItems(query),
        query,
        categoryRepository.observeCategories(),
        locationRepository.observeLocations(),
    ) { overview, currentQuery, categories, locations ->
        ItemsUiState.Success(overview, currentQuery, categories, locations)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ItemsUiState.Loading,
    )

    private val _events = Channel<ItemsEvent>(Channel.BUFFERED)
    val events: Flow<ItemsEvent> = _events.receiveAsFlow()

    fun onSearchTextChange(text: String) {
        searchText = text
    }

    fun onStatusFilterChange(status: ExpiryStatus?) = filters.update { it.copy(status = status) }

    fun onCategoryFilterChange(categoryId: Long?) = filters.update { it.copy(categoryId = categoryId) }

    fun onLocationFilterChange(locationId: Long?) = filters.update { it.copy(locationId = locationId) }

    fun clearFilters() {
        searchText = ""
        filters.value = ItemQuery()
    }

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

    @AssistedFactory
    interface Factory {
        fun create(initialStatus: ExpiryStatus?): ItemsViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
