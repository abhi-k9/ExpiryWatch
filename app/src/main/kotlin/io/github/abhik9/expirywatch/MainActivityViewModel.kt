package io.github.abhik9.expirywatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.domain.repository.UserSettingsRepository
import io.github.abhik9.expirywatch.core.model.UserSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState

    data class Success(val settings: UserSettings) : MainActivityUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settingsRepository: UserSettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<MainActivityUiState> = settingsRepository.settings
        .map<UserSettings, MainActivityUiState>(MainActivityUiState::Success)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MainActivityUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
