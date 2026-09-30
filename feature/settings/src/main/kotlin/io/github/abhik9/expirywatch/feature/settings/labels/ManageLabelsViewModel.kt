package io.github.abhik9.expirywatch.feature.settings.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.domain.repository.CategoryRepository
import io.github.abhik9.expirywatch.core.domain.repository.StorageLocationRepository
import io.github.abhik9.expirywatch.core.model.Category
import io.github.abhik9.expirywatch.core.model.StorageLocation
import io.github.abhik9.expirywatch.core.navigation.LabelKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A category or storage location, which are edited the same way. */
data class Label(
    val id: Long = 0,
    val name: String,
    val emoji: String,
)

@HiltViewModel(assistedFactory = ManageLabelsViewModel.Factory::class)
class ManageLabelsViewModel @AssistedInject constructor(
    @Assisted val kind: LabelKind,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: StorageLocationRepository,
) : ViewModel() {
    private val source: Flow<List<Label>> = when (kind) {
        LabelKind.CATEGORIES -> categoryRepository.observeCategories().map { list ->
            list.map { Label(it.id, it.name, it.emoji) }
        }

        LabelKind.LOCATIONS -> locationRepository.observeLocations().map { list ->
            list.map { Label(it.id, it.name, it.emoji) }
        }
    }

    /** `null` until loaded. */
    val labels: StateFlow<List<Label>?> = source
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun save(label: Label) {
        val name = label.name.trim()
        if (name.isEmpty()) return
        val emoji = label.emoji.trim().ifEmpty { DEFAULT_EMOJI }
        viewModelScope.launch {
            when (kind) {
                LabelKind.CATEGORIES -> categoryRepository.upsert(Category(label.id, name, emoji))
                LabelKind.LOCATIONS -> locationRepository.upsert(StorageLocation(label.id, name, emoji))
            }
        }
    }

    fun delete(label: Label) {
        viewModelScope.launch {
            when (kind) {
                LabelKind.CATEGORIES -> categoryRepository.delete(label.id)
                LabelKind.LOCATIONS -> locationRepository.delete(label.id)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(kind: LabelKind): ManageLabelsViewModel
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DEFAULT_EMOJI = "📦"
    }
}
