package io.github.abhik9.expirywatch.feature.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.domain.repository.CategoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.ItemRepository
import io.github.abhik9.expirywatch.core.domain.repository.StorageLocationRepository
import io.github.abhik9.expirywatch.core.domain.usecase.FinishItemUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ItemValidationError
import io.github.abhik9.expirywatch.core.domain.usecase.LookupProductUseCase
import io.github.abhik9.expirywatch.core.domain.usecase.ProductLookupResult
import io.github.abhik9.expirywatch.core.domain.usecase.SaveItemResult
import io.github.abhik9.expirywatch.core.domain.usecase.SaveItemUseCase
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.Item
import io.github.abhik9.expirywatch.core.model.ItemStatus
import io.github.abhik9.expirywatch.core.model.ProductInfo
import io.github.abhik9.expirywatch.core.model.ProductSource
import io.github.abhik9.expirywatch.core.model.QuantityUnit
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.navigation.EditorNavKey
import io.github.abhik9.expirywatch.core.ui.formatNumber
import java.text.DecimalFormatSymbols
import java.time.Clock
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The editable fields, kept as entered (e.g. quantity as text) until the item is saved. */
data class EditorForm(
    val name: String = "",
    val brand: String = "",
    val barcode: String = "",
    val categoryId: Long? = null,
    val locationId: Long? = null,
    val quantityText: String = "1",
    val unit: QuantityUnit = QuantityUnit.PIECES,
    val expiryDate: LocalDate? = null,
    val isOpened: Boolean = false,
    val openedDate: LocalDate? = null,
    val useWithinDaysText: String = "",
    val notes: String = "",
    val imageUrl: String? = null,
)

enum class EditorError {
    NAME_REQUIRED,
    EXPIRY_REQUIRED,
    QUANTITY_INVALID,
    OPENED_WINDOW_INVALID,
}

sealed interface LookupState {
    data object Idle : LookupState

    data object Loading : LookupState

    data class Found(val source: ProductSource) : LookupState

    data object NotFound : LookupState

    data object Unavailable : LookupState
}

sealed interface EditorEvent {
    /** The item was saved, deleted or finished: the editor should close. */
    data object Done : EditorEvent
}

@HiltViewModel(assistedFactory = EditorViewModel.Factory::class)
class EditorViewModel @AssistedInject constructor(
    @Assisted private val key: EditorNavKey,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: StorageLocationRepository,
    private val itemRepository: ItemRepository,
    private val saveItem: SaveItemUseCase,
    private val finishItem: FinishItemUseCase,
    private val lookupProduct: LookupProductUseCase,
    private val clock: Clock,
) : ViewModel() {
    // Form state is Compose state, so text fields update synchronously as the user types.
    var form by mutableStateOf(EditorForm())
        private set
    var errors by mutableStateOf(emptySet<EditorError>())
        private set
    var lookup by mutableStateOf<LookupState>(LookupState.Idle)
        private set
    var isLoading by mutableStateOf(key.itemId != null)
        private set
    var isSaving by mutableStateOf(false)
        private set

    val isNewItem: Boolean = key.itemId == null
    val today: LocalDate get() = LocalDate.now(clock)

    val hasUnsavedChanges: Boolean get() = !isLoading && form != initialForm

    val categories: StateFlow<List<Category>> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    val locations: StateFlow<List<StorageLocation>> = locationRepository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    private var existingItem: Item? = null
    private var initialForm = EditorForm()
    private var lookupJob: Job? = null

    init {
        key.itemId?.let { id ->
            viewModelScope.launch {
                val item = itemRepository.observeItem(id).first()
                if (item == null) {
                    // Deleted elsewhere, e.g. from another screen or a backup restore.
                    _events.send(EditorEvent.Done)
                    return@launch
                }
                existingItem = item
                form = item.toForm()
                initialForm = form
                isLoading = false
            }
        }
    }

    fun updateForm(transform: (EditorForm) -> EditorForm) {
        form = transform(form)
        if (errors.isNotEmpty()) errors = validate(form)
    }

    fun onBarcodeScanned(barcode: String) {
        form = form.copy(barcode = barcode)
        lookUpBarcode()
    }

    /** Fills in empty fields with what's known about the barcode's product. */
    fun lookUpBarcode() {
        val barcode = form.barcode.trim()
        if (barcode.isEmpty()) return
        lookupJob?.cancel()
        lookup = LookupState.Loading
        lookupJob = viewModelScope.launch {
            lookup = when (val result = lookupProduct(barcode)) {
                is ProductLookupResult.Found -> {
                    form = form.withProduct(result.product)
                    LookupState.Found(result.product.source)
                }

                ProductLookupResult.NotFound -> LookupState.NotFound

                ProductLookupResult.CatalogUnavailable -> LookupState.Unavailable
            }
        }
    }

    fun save() {
        val currentErrors = validate(form)
        errors = currentErrors
        if (currentErrors.isNotEmpty() || isSaving) return
        val expiryDate = form.expiryDate ?: return

        isSaving = true
        val submitted = form
        viewModelScope.launch {
            val item = buildItem(submitted, expiryDate)
            when (val result = saveItem(item)) {
                is SaveItemResult.Saved -> _events.send(EditorEvent.Done)
                is SaveItemResult.Invalid -> errors = result.errors.map(::toEditorError).toSet()
            }
            isSaving = false
        }
    }

    fun finish(outcome: ItemStatus) {
        val id = existingItem?.id ?: return
        viewModelScope.launch {
            finishItem(id, outcome)
            _events.send(EditorEvent.Done)
        }
    }

    fun delete() {
        val id = existingItem?.id ?: return
        viewModelScope.launch {
            itemRepository.delete(id)
            _events.send(EditorEvent.Done)
        }
    }

    // Looks the labels up in the repositories rather than in [categories] and [locations], which are
    // only kept up to date while the UI collects them.
    private suspend fun buildItem(form: EditorForm, expiryDate: LocalDate): Item {
        val base = existingItem ?: Item(name = "", expiryDate = expiryDate)
        val category = categoryRepository.observeCategories().first().find { it.id == form.categoryId }
        val location = locationRepository.observeLocations().first().find { it.id == form.locationId }
        return base.copy(
            name = form.name,
            brand = form.brand,
            barcode = form.barcode,
            category = category,
            location = location,
            quantity = form.quantityText.parseQuantity() ?: base.quantity,
            unit = form.unit,
            expiryDate = expiryDate,
            openedDate = if (form.isOpened) form.openedDate ?: today else null,
            useWithinDaysAfterOpening = form.useWithinDaysText.trim().toIntOrNull(),
            notes = form.notes,
            imageUrl = form.imageUrl,
        )
    }

    private fun validate(form: EditorForm): Set<EditorError> = buildSet {
        if (form.name.isBlank()) add(EditorError.NAME_REQUIRED)
        if (form.expiryDate == null) add(EditorError.EXPIRY_REQUIRED)
        val quantity = form.quantityText.parseQuantity()
        if (quantity == null || quantity <= 0.0) add(EditorError.QUANTITY_INVALID)
        val window = form.useWithinDaysText.trim()
        if (window.isNotEmpty() && (window.toIntOrNull() ?: 0) <= 0) add(EditorError.OPENED_WINDOW_INVALID)
    }

    private fun toEditorError(error: ItemValidationError): EditorError = when (error) {
        ItemValidationError.NAME_REQUIRED -> EditorError.NAME_REQUIRED
        ItemValidationError.QUANTITY_INVALID -> EditorError.QUANTITY_INVALID
        ItemValidationError.OPENED_WINDOW_INVALID -> EditorError.OPENED_WINDOW_INVALID
    }

    @AssistedFactory
    interface Factory {
        fun create(key: EditorNavKey): EditorViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** Accepts both "1.5" and "1,5", since many locales use a decimal comma. */
internal fun String.parseQuantity(): Double? = trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

/**
 * Formats a quantity for editing in the user's locale, falling back to "1.5" style for locales whose
 * digits or decimal separator [parseQuantity] can't read back.
 */
internal fun Double.toQuantityText(locale: Locale = Locale.getDefault()): String {
    val symbols = DecimalFormatSymbols.getInstance(locale)
    val isParseable = symbols.zeroDigit == '0' && symbols.decimalSeparator in ".,"
    return formatNumber(this, if (isParseable) locale else Locale.ROOT)
}

private fun Item.toForm() = EditorForm(
    name = name,
    brand = brand.orEmpty(),
    barcode = barcode.orEmpty(),
    categoryId = category?.id,
    locationId = location?.id,
    quantityText = quantity.toQuantityText(),
    unit = unit,
    expiryDate = expiryDate,
    isOpened = openedDate != null,
    openedDate = openedDate,
    useWithinDaysText = useWithinDaysAfterOpening?.toString().orEmpty(),
    notes = notes.orEmpty(),
    imageUrl = imageUrl,
)

/**
 * Fills in what the user hasn't entered yet. Anything they typed wins over the looked-up product,
 * and the quantity is only replaced while it's still the default.
 */
private fun EditorForm.withProduct(product: ProductInfo): EditorForm {
    val quantityIsDefault = quantityText == EditorForm().quantityText && unit == EditorForm().unit
    return copy(
        name = name.ifBlank { product.name },
        brand = brand.ifBlank { product.brand.orEmpty() },
        categoryId = categoryId ?: product.categoryId,
        locationId = locationId ?: product.locationId,
        quantityText = product.quantity?.takeIf { quantityIsDefault }?.toQuantityText() ?: quantityText,
        unit = product.unit?.takeIf { quantityIsDefault } ?: unit,
        useWithinDaysText = useWithinDaysText.ifBlank { product.useWithinDaysAfterOpening?.toString().orEmpty() },
        imageUrl = imageUrl ?: product.imageUrl,
    )
}
