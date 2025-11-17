package com.professor.playstorebaseproject.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.professor.playstorebaseproject.app.AppPreferences
import com.professor.playstorebaseproject.iab.SubscriptionItem
import com.professor.playstorebaseproject.utils.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UIState<PremiumUiState>>(UIState.Loading)
    val uiState: StateFlow<UIState<PremiumUiState>> = _uiState.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<SubscriptionItem>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions.asStateFlow()

    fun updateSubscriptions(subscriptions: List<SubscriptionItem>) {
        _subscriptions.value = subscriptions
        
        val uiState = PremiumUiState(
            subscriptions = subscriptions,
            isPremium = appPreferences.getBoolean(AppPreferences.IS_PREMIUM)
        )
        _uiState.value = UIState.Success(uiState)
    }

    fun setPremiumStatus(isPremium: Boolean) {
        appPreferences.setBoolean(AppPreferences.IS_PREMIUM, isPremium)
    }
}

data class PremiumUiState(
    val subscriptions: List<SubscriptionItem>,
    val isPremium: Boolean
)