package com.yourapp.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed class StartDestination {
    data object Home : StartDestination()

    data object Onboarding : StartDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    userProfileRepository: UserProfileRepository,
) : ViewModel() {
    val startDestination: StateFlow<StartDestination?> = userProfileRepository.userProfileFlow
        .map { profile ->
            if (profile == null) {
                StartDestination.Onboarding
            } else {
                StartDestination.Home
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
