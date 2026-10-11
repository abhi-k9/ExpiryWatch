package io.github.abhik9.expirywatch.feature.shopping.text

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
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.GetShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveShoppingListSummariesUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingEntry
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingListSummary
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingListText
import io.github.abhik9.expirywatch.core.model.Category
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One-off messages for the screen. */
sealed interface AddFromTextEvent {
    /** The items were added: the screen should close. */
    data object Added : AddFromTextEvent
}

@HiltViewModel(assistedFactory = AddFromTextViewModel.Factory::class)
class AddFromTextViewModel @AssistedInject constructor(
    @Assisted initialText: String,
    observeShoppingListSummaries: ObserveShoppingListSummariesUseCase,
    categoryRepository: CategoryRepository,
    private val getShoppingList: GetShoppingListUseCase,
    private val addToShoppingList: AddToShoppingListUseCase,
    private val settingsRepository: UserSettingsRepository,
) : ViewModel() {
    /** The text to read items from, held as Compose state so it updates synchronously while typing. */
    var text by mutableStateOf(initialText)
        private set

    /** The list to add to; the one in use until another is picked. */
    var listId by mutableStateOf<Long?>(null)
        private set

    var isAdding by mutableStateOf(false)
        private set

    val lists: StateFlow<List<ShoppingListSummary>> = observeShoppingListSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // Kept up to date from the start, so adding reads the same items as the screen shows.
    private val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** What the text would add, as it's typed. */
    val entries: StateFlow<List<ShoppingEntry>> = combine(snapshotFlow { text }, categories) { current, known ->
        ShoppingListText.parse(current, known)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _events = Channel<AddFromTextEvent>(Channel.BUFFERED)
    val events: Flow<AddFromTextEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (listId == null) listId = getShoppingList().id
        }
    }

    fun onTextChange(newText: String) {
        text = newText
    }

    fun onListChange(id: Long) {
        listId = id
    }

    /** Adds everything in the text to the chosen list, which opens next. */
    fun add() {
        val target = listId ?: return
        val toAdd = ShoppingListText.parse(text, categories.value)
        if (toAdd.isEmpty() || isAdding) return
        isAdding = true
        viewModelScope.launch {
            toAdd.forEach { addToShoppingList(target, it) }
            settingsRepository.setShoppingListId(target)
            _events.send(AddFromTextEvent.Added)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialText: String): AddFromTextViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
