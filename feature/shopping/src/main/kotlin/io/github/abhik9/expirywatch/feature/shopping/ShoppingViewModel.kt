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
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListResult
import io.github.abhik9.expirywatch.core.domain.shopping.AddToShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.CreateShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.DeleteShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.DismissRestockSuggestionUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.GetShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.KnownProduct
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveKnownProductsUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveRestockSuggestionsUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveShoppingListSummariesUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ObserveShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.RenameShoppingListUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.RestockSuggestion
import io.github.abhik9.expirywatch.core.domain.shopping.SaveShoppingItemUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingEntry
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingListOverview
import io.github.abhik9.expirywatch.core.domain.shopping.ShoppingListSummary
import io.github.abhik9.expirywatch.core.domain.shopping.aisleComparator
import io.github.abhik9.expirywatch.core.domain.shopping.buildShoppingOverview
import io.github.abhik9.expirywatch.core.domain.shopping.suggestProducts
import io.github.abhik9.expirywatch.core.domain.usecase.LookupProductUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ProductLookupResult
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
        /** Every list, to switch between them. */
        val lists: List<ShoppingListSummary>,
        /** Products to buy again, because what the user had expired or is gone. */
        val restock: List<RestockSuggestion>,
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
    observeShoppingListSummaries: ObserveShoppingListSummariesUseCase,
    observeKnownProducts: ObserveKnownProductsUseCase,
    observeRestockSuggestions: ObserveRestockSuggestionsUseCase,
    categoryRepository: CategoryRepository,
    private val getShoppingList: GetShoppingListUseCase,
    private val addToShoppingList: AddToShoppingListUseCase,
    private val saveShoppingItem: SaveShoppingItemUseCase,
    private val createShoppingList: CreateShoppingListUseCase,
    private val renameShoppingList: RenameShoppingListUseCase,
    private val deleteShoppingList: DeleteShoppingListUseCase,
    private val dismissRestockSuggestion: DismissRestockSuggestionUseCase,
    private val lookupProduct: LookupProductUseCase,
    private val shoppingRepository: ShoppingRepository,
    private val settingsRepository: UserSettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    /** What's typed into the add field, held as Compose state so it updates synchronously. */
    var input by mutableStateOf("")
        private set

    /** What happened to the last thing added, shown under the add field until the user types again. */
    var lastAdded by mutableStateOf<AddToShoppingListResult?>(null)
        private set

    /** Why the last barcode scanned couldn't be added, until the user types again. */
    var scanProblem by mutableStateOf<ProductLookupResult?>(null)
        private set

    /** The item being changed in the edit sheet, if any. */
    var editing by mutableStateOf<ShoppingItem?>(null)
        private set

    /** A barcode scanned for a product that wasn't found, for what's typed next. */
    private var unknownBarcode: String? = null

    private val aisleOrder: Flow<List<Long>> = settingsRepository.settings
        .map { it.shoppingAisleOrder }
        .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val overview: Flow<ShoppingListOverview> = observeShoppingList().flatMapLatest { list ->
        combine(shoppingRepository.observeItems(list.id), aisleOrder) { items, order ->
            buildShoppingOverview(list, items, order)
        }
    }

    /** The categories in the order of the shop. */
    private val categories: Flow<List<Category>> = combine(
        categoryRepository.observeCategories(),
        aisleOrder,
    ) { all, order -> all.sortedWith(aisleComparator(order)) }

    private val content: Flow<ListContent> = combine(
        overview,
        observeShoppingListSummaries(),
        categories,
        observeRestockSuggestions(),
    ) { currentOverview, lists, orderedCategories, restock ->
        ListContent(currentOverview, lists, orderedCategories, restock)
    }

    val uiState: StateFlow<ShoppingUiState> = combine(
        content,
        observeKnownProducts(),
        snapshotFlow { input },
    ) { currentContent, known, text ->
        val listOverview = currentContent.overview
        val onList = listOverview.aisles.flatMap { aisle -> aisle.items.map { productKey(it.name) } }.toSet()
        ShoppingUiState.Success(
            overview = listOverview,
            suggestions = suggestProducts(ShoppingEntry.parse(text)?.name.orEmpty(), known, exclude = onList),
            categories = currentContent.categories,
            lists = currentContent.lists,
            restock = currentContent.restock,
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
        scanProblem = null
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
        val withBarcode = unknownBarcode?.let { entry.copy(barcode = it) } ?: entry
        unknownBarcode = null
        viewModelScope.launch {
            lastAdded = addToShoppingList(getShoppingList().id, withBarcode)
        }
    }

    /**
     * Adds the product with the scanned [barcode]. When it isn't known, the user is asked to type
     * its name, and the barcode goes with what they add next, so it's known from then on.
     */
    fun onBarcodeScanned(barcode: String) {
        lastAdded = null
        scanProblem = null
        viewModelScope.launch {
            when (val result = lookupProduct(barcode)) {
                is ProductLookupResult.Found -> {
                    val product = result.product
                    val entry = ShoppingEntry(
                        name = product.name,
                        brand = product.brand,
                        barcode = product.barcode,
                        category = categories.first().firstOrNull { it.id == product.categoryId },
                    )
                    lastAdded = addToShoppingList(getShoppingList().id, entry)
                }

                else -> {
                    unknownBarcode = barcode.trim()
                    scanProblem = result
                }
            }
        }
    }

    /** Adds a product suggested to buy again, leaving what's typed as it is. */
    fun addRestock(suggestion: RestockSuggestion) {
        viewModelScope.launch {
            lastAdded = addToShoppingList(getShoppingList().id, suggestion.toEntry())
        }
    }

    fun addAllRestock(suggestions: List<RestockSuggestion>) {
        viewModelScope.launch {
            val listId = getShoppingList().id
            suggestions.forEach { addToShoppingList(listId, it.toEntry()) }
        }
    }

    fun dismissRestock(suggestion: RestockSuggestion) {
        viewModelScope.launch { dismissRestockSuggestion(suggestion) }
    }

    private fun RestockSuggestion.toEntry() = ShoppingEntry(name = name, category = category)

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

    fun switchToList(listId: Long) {
        lastAdded = null
        viewModelScope.launch { settingsRepository.setShoppingListId(listId) }
    }

    fun createList(name: String) {
        lastAdded = null
        viewModelScope.launch { createShoppingList(name) }
    }

    fun renameList(listId: Long, name: String) {
        viewModelScope.launch { renameShoppingList(listId, name) }
    }

    /** Puts the categories in the order the user walks through the shop. */
    fun arrangeAisles(categoryIds: List<Long>) {
        viewModelScope.launch { settingsRepository.setShoppingAisleOrder(categoryIds) }
    }

    fun deleteList(listId: Long) {
        lastAdded = null
        viewModelScope.launch { deleteShoppingList(listId) }
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

/** What the list shows, apart from the suggestions for what's typed. */
private data class ListContent(
    val overview: ShoppingListOverview,
    val lists: List<ShoppingListSummary>,
    val categories: List<Category>,
    val restock: List<RestockSuggestion>,
)
