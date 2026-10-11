package io.github.abhik9.expirywatch.feature.shopping

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.domain.productKey
import io.github.abhik9.expirywatch.core.domain.repository.CategoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.ShoppingRepository
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListResult
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.GetShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.KnownProduct
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveKnownProductsUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.SaveShoppingItemUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingEntry
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingListOverview
import io.github.abhik9.expirywatch.core.domain.shopping.buildShoppingOverview
import io.github.abhik9.expirywatch.core.domain.shopping.suggestProducts
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.ShoppingItem
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ShoppingUiState {
    data object Loading : ShoppingUiState

    data class Success(
        val overview: ShoppingListOverview,
        /** Products to offer under the add field for what's typed so far. */
        val suggestions: List<KnownProduct>,
        val categories: List<Category>,
    ) : ShoppingUiState
}

/** One-off messages for the screen, such as the undo snackbars. */
sealed interface ShoppingEvent {
    data class ItemRemoved(val item: ShoppingItem) : ShoppingEvent

    data class CartCleared(val items: List<ShoppingItem>) : ShoppingEvent
}

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    observeShoppingList: ObserveShoppingListUseCase,
    observeKnownProducts: ObserveKnownProductsUseCase,
    categoryRepository: CategoryRepository,
    private val getShoppingList: GetShoppingListUseCase,
    private val addToShoppingList: AddToShoppingListUseCase,
    private val saveShoppingItem: SaveShoppingItemUseCase,
    private val shoppingRepository: ShoppingRepository,
    private val clock: Clock,
) : ViewModel() {
    /** What's typed into the add field, held as Compose state so it updates synchronously. */
    var input by mutableStateOf("")
        private set

    /** What happened to the last thing added, shown under the add field until the user types again. */
    var lastAdded by mutableStateOf<AddToShoppingListResult?>(null)
        private set

    /** The item being changed in the edit sheet, if any. */
    var editing by mutableStateOf<ShoppingItem?>(null)
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    private val overview: Flow<ShoppingListOverview> = observeShoppingList().flatMapLatest { list ->
        shoppingRepository.observeItems(list.id).map { items -> buildShoppingOverview(list, items) }
    }

    val uiState: StateFlow<ShoppingUiState> = combine(
        overview,
        observeKnownProducts(),
        snapshotFlow { input },
        categoryRepository.observeCategories(),
    ) { currentOverview, known, text, categories ->
        val onList = currentOverview.aisles.flatMap { aisle -> aisle.items.map { productKey(it.name) } }.toSet()
        ShoppingUiState.Success(
            overview = currentOverview,
            suggestions = suggestProducts(ShoppingEntry.parse(text)?.name.orEmpty(), known, exclude = onList),
            categories = categories,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ShoppingUiState.Loading,
    )

    private val _events = Channel<ShoppingEvent>(Channel.BUFFERED)
    val events: Flow<ShoppingEvent> = _events.receiveAsFlow()

    fun onInputChange(text: String) {
        input = text
        lastAdded = null
    }

    /** Adds what's typed, such as "2 milk". */
    fun add() {
        ShoppingEntry.parse(input)?.let(::add)
    }

    /** Adds a suggested product, in the quantity typed so far. */
    fun addSuggestion(product: KnownProduct) {
        val typed = ShoppingEntry.parse(input)
        val entry = ShoppingEntry(name = product.name, category = product.category)
        add(if (typed == null) entry else entry.copy(quantity = typed.quantity, unit = typed.unit))
    }

    private fun add(entry: ShoppingEntry) {
        // Cleared right away, so the next item can be typed while this one is saved.
        input = ""
        viewModelScope.launch {
            lastAdded = addToShoppingList(getShoppingList().id, entry)
        }
    }

    fun onCheckedChange(item: ShoppingItem, checked: Boolean) {
        viewModelScope.launch {
            shoppingRepository.setChecked(listOf(item.id), checkedAt = if (checked) Instant.now(clock) else null)
        }
    }

    fun onEdit(item: ShoppingItem) {
        editing = item
    }

    fun onEditDismiss() {
        editing = null
    }

    /** Saves the changes made in the edit sheet. */
    fun save(changed: ShoppingItem) {
        val original = editing ?: return
        editing = null
        viewModelScope.launch { saveShoppingItem(original, changed) }
    }

    fun remove(item: ShoppingItem) {
        if (editing?.id == item.id) editing = null
        viewModelScope.launch {
            shoppingRepository.deleteItems(listOf(item.id))
            _events.send(ShoppingEvent.ItemRemoved(item))
        }
    }

    fun undoRemove(item: ShoppingItem) {
        viewModelScope.launch { shoppingRepository.restoreItems(listOf(item)) }
    }

    /** Removes everything in the cart from the list. */
    fun clearCart() {
        viewModelScope.launch {
            val inCart = shoppingRepository.getItems(getShoppingList().id).filter { it.isChecked }
            if (inCart.isEmpty()) return@launch
            shoppingRepository.deleteItems(inCart.map { it.id })
            _events.send(ShoppingEvent.CartCleared(inCart))
        }
    }

    fun undoClearCart(items: List<ShoppingItem>) {
        viewModelScope.launch { shoppingRepository.restoreItems(items) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
