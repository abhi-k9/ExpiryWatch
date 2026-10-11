package io.github.abhik9.expirywatch.feature.shopping.putaway

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.domain.repository.StorageLocationRepository
import io.github.abhik9.expirywatch.core.domain.shopping.GetPutAwayDraftsUseCase
import io.github.abhik9.expirywatch.core.domain.shopping.PutAwayDraft
import io.github.abhik9.expirywatch.core.domain.shopping.PutAwayUseCase
import io.github.abhik9.expirywatch.core.model.StorageLocation
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Something bought, and whether to put it away now. */
data class PutAwayRow(
    val draft: PutAwayDraft,
    val included: Boolean = true,
) {
    val id: Long get() = draft.bought.id
}

sealed interface PutAwayState {
    data object Loading : PutAwayState

    /** @property showErrors whether to point out the rows that still need an expiry date. */
    data class Editing(val rows: List<PutAwayRow>, val showErrors: Boolean = false) : PutAwayState {
        val canSave: Boolean get() = rows.any { it.included }
    }

    data object Saving : PutAwayState

    data class Done(val count: Int) : PutAwayState
}

@HiltViewModel(assistedFactory = PutAwayViewModel.Factory::class)
class PutAwayViewModel @AssistedInject constructor(
    @Assisted listId: Long,
    getPutAwayDrafts: GetPutAwayDraftsUseCase,
    locationRepository: StorageLocationRepository,
    private val putAway: PutAwayUseCase,
) : ViewModel() {
    /** Compose state, so changing a row updates the screen synchronously. */
    var state by mutableStateOf<PutAwayState>(PutAwayState.Loading)
        private set

    val locations: StateFlow<List<StorageLocation>> = locationRepository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    init {
        viewModelScope.launch {
            state = PutAwayState.Editing(getPutAwayDrafts(listId).map { PutAwayRow(it) })
        }
    }

    fun setIncluded(id: Long, included: Boolean) = updateRow(id) { it.copy(included = included) }

    fun setExpiryDate(id: Long, date: LocalDate) = updateRow(id) { it.copy(draft = it.draft.copy(expiryDate = date)) }

    fun setLocation(id: Long, location: StorageLocation?) =
        updateRow(id) { it.copy(draft = it.draft.copy(location = location)) }

    private fun updateRow(id: Long, change: (PutAwayRow) -> PutAwayRow) {
        val editing = state as? PutAwayState.Editing ?: return
        state = editing.copy(rows = editing.rows.map { if (it.id == id) change(it) else it })
    }

    /** Puts away what's included, once each has an expiry date. */
    fun save() {
        val editing = state as? PutAwayState.Editing ?: return
        val chosen = editing.rows.filter { it.included }
        if (chosen.isEmpty()) return
        if (chosen.any { it.draft.expiryDate == null }) {
            state = editing.copy(showErrors = true)
            return
        }
        state = PutAwayState.Saving
        viewModelScope.launch {
            state = PutAwayState.Done(putAway(chosen.map { it.draft }))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(listId: Long): PutAwayViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
