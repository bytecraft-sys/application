package com.yourapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yourapp.ui.chat.ChatViewModel
import com.yourapp.ui.history.ChatHistoryScreen
import com.yourapp.ui.home.HomeRoute
import com.yourapp.ui.onboarding.OnboardingRoute
import com.yourapp.ui.profile.ProfileRoute
import com.yourapp.ui.splash.SplashViewModel
import com.yourapp.ui.splash.StartDestination

@Composable
fun AppNavGraph(
    navController: NavHostController,
    splashViewModel: SplashViewModel = hiltViewModel(),
) {
    val startDestination by splashViewModel.startDestination.collectAsState()

    when (val destination = startDestination) {
        null -> SplashScreen()
        else -> NavHost(
            navController = navController,
            startDestination = destination.route,
        ) {
            composable(AppRoute.Onboarding.route) {
                OnboardingRoute(
                    onNavigateToHome = {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Onboarding.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(AppRoute.Home.route) {
                val viewModel: ChatViewModel = hiltViewModel()
                HomeRoute(
                    onOpenProfile = {
                        navController.navigate(AppRoute.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHistory = {
                        navController.navigate(AppRoute.History.route)
                    },
                    viewModel = viewModel
                )
            }
            composable(
                route = AppRoute.Session.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId")
                val viewModel: ChatViewModel = hiltViewModel()
                LaunchedEffect(sessionId) {
                    sessionId?.let { viewModel.loadSession(it) }
                }
                HomeRoute(
                    onOpenProfile = {
                        navController.navigate(AppRoute.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHistory = {
                        navController.navigate(AppRoute.History.route)
                    },
                    viewModel = viewModel
                )
            }
            composable(AppRoute.History.route) {
                ChatHistoryScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToChat = { sessionId ->
                        navController.navigate(AppRoute.Session.createRoute(sessionId))
                    },
                    onNavigateToNewChat = {
                        navController.navigate(AppRoute.Home.route) {
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(AppRoute.Profile.route) {
                ProfileRoute(
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(AppRoute.Onboarding.route) {
                            popUpTo(0) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

private val StartDestination.route: String
    get() = when (this) {
        StartDestination.Home -> AppRoute.Home.route
        StartDestination.Onboarding -> AppRoute.Onboarding.route
    }

private sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")
    data object Onboarding : AppRoute("onboarding")
    data object Profile : AppRoute("profile")
    data object History : AppRoute("chat_history")
    data object Session : AppRoute("chat_session/{sessionId}") {
        fun createRoute(sessionId: String) = "chat_session/$sessionId"
    }
}
