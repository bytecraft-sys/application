package com.yourapp.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OnboardingRoute(
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                UiEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    OnboardingScreen(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onAgeChanged = viewModel::onAgeChanged,
        onPhoneChanged = viewModel::onPhoneChanged,
        onOtpDigitChanged = viewModel::onOtpDigitChanged,
        onVerifyOtp = viewModel::verifyOtp,
        onTraitClicked = viewModel::toggleTrait,
        onDone = viewModel::onOnboardingComplete,
    )
}
