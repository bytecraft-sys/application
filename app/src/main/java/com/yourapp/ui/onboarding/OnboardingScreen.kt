package com.yourapp.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.ApplicationTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onNameChanged: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onOtpDigitChanged: (Int, String) -> Unit,
    onVerifyOtp: () -> Unit,
    onTraitClicked: (String) -> Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Step ${pagerState.currentPage + 1} of $ONBOARDING_PAGE_COUNT",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> ValuePropsScreen(modifier = Modifier.fillMaxSize())
                    1 -> ProfileFormScreen(
                        state = state,
                        onNameChanged = onNameChanged,
                        onAgeChanged = onAgeChanged,
                        onPhoneChanged = onPhoneChanged,
                        onOtpDigitChanged = onOtpDigitChanged,
                        onVerifyOtp = onVerifyOtp,
                        modifier = Modifier.fillMaxSize(),
                    )
                    2 -> TraitSelectionScreen(
                        selectedTraits = state.selectedTraits,
                        onTraitClicked = { trait ->
                            val reachedMax = onTraitClicked(trait)
                            if (reachedMax) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Max 3 traits selected")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    enabled = pagerState.currentPage > 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                ) {
                    Text("Back")
                }

                Button(
                    enabled = when (pagerState.currentPage) {
                        1 -> state.isProfileStepValid
                        2 -> !state.isSaving
                        else -> true
                    },
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> scope.launch { pagerState.animateScrollToPage(1) }
                            1 -> if (state.isProfileStepValid) {
                                scope.launch { pagerState.animateScrollToPage(2) }
                            }
                            2 -> onDone()
                        }
                    },
                ) {
                    Text(if (pagerState.currentPage == 2) "Done" else "Next")
                }
            }
        }
    }
}

private const val ONBOARDING_PAGE_COUNT = 3

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    ApplicationTheme {
        OnboardingScreen(
            state = OnboardingState(
                name = "Alex",
                age = "28",
                phone = "5551234567",
                otpDigits = listOf("1", "2", "3", "4"),
                isOtpVerified = true,
                selectedTraits = setOf("Curious", "Creative"),
            ),
            onNameChanged = {},
            onAgeChanged = {},
            onPhoneChanged = {},
            onOtpDigitChanged = { _, _ -> },
            onVerifyOtp = {},
            onTraitClicked = { false },
            onDone = {},
        )
    }
}
