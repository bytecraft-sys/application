package com.yourapp.ui.onboarding

sealed interface UiEvent {
    data object NavigateToHome : UiEvent
}
