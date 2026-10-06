package io.github.abhik9.expirywatch.feature.settings.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.ProductGrouping
import io.github.abhik9.expirywatch.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AdvancedSettingsViewModel @Inject constructor(
    private val settingsRepository: UserSettingsRepository,
) : ViewModel() {
    val settings: StateFlow<UserSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun setProductGrouping(grouping: ProductGrouping) = launch { settingsRepository.setProductGrouping(grouping) }

    fun setKeepProductsTogether(keepTogether: Boolean) =
        launch { settingsRepository.setKeepProductsTogether(keepTogether) }

    fun setRemindAboutExpired(remind: Boolean) = launch { settingsRepository.setRemindAboutExpired(remind) }

    fun setOnlineProductLookup(enabled: Boolean) = launch { settingsRepository.setOnlineProductLookup(enabled) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
