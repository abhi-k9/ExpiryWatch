package io.github.abhik9.expirywatch.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.abhik9.expirywatch.core.domain.usecase.ObserveInsightsUseCase
import io.github.abhik9.expirywatch.core.model.Insights
import io.github.abhik9.expirywatch.core.model.InsightsPeriod
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface InsightsUiState {
    data object Loading : InsightsUiState

    data class Success(val insights: Insights) : InsightsUiState
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    observeInsights: ObserveInsightsUseCase,
) : ViewModel() {
    private val _period = MutableStateFlow(InsightsPeriod.ONE_MONTH)
    val period: StateFlow<InsightsPeriod> = _period.asStateFlow()

    val uiState: StateFlow<InsightsUiState> = observeInsights(_period)
        .map<Insights, InsightsUiState>(InsightsUiState::Success)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), InsightsUiState.Loading)

    fun onPeriodChange(period: InsightsPeriod) {
        _period.value = period
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
