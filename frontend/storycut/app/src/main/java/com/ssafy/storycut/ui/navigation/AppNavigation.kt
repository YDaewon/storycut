// AppNavigation.kt
package com.ssafy.storycut.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.ui.auth.AuthViewModel
import com.ssafy.storycut.ui.auth.LoginScreen
import com.ssafy.storycut.ui.splash.SplashScreen

private const val TAG = "AppNavigation"

@Composable
fun AppNavigation(
    tokenManager: TokenManager,
    isDeepLink: Boolean = false,
    isFromFcm: Boolean = false,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    val startDestination = if (isDeepLink || isFromFcm) Navigation.MAIN else Navigation.SPLASH
    val shouldNavigateToShorts = remember { mutableStateOf(isDeepLink) }
    val shouldNavigateToMyPage = remember { mutableStateOf(isFromFcm) }

    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val navigateToLogin = activity?.intent?.getBooleanExtra("NAVIGATE_TO_LOGIN", false) ?: false

    // 로그인 화면으로 즉시 이동해야 하는 경우
    LaunchedEffect(navigateToLogin) {
        if (navigateToLogin) {
            navController.navigate(Navigation.LOGIN) {
                popUpTo(Navigation.SPLASH) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 스플래시 화면
        composable(Navigation.SPLASH) {
            SplashScreen(navController, authViewModel)
        }

        // 로그인 화면
        composable(Navigation.LOGIN) {
            LoginScreen(navController, authViewModel)
        }

        composable(Navigation.MAIN) {
            MainScreen(
                authViewModel = authViewModel,
                tokenManager = tokenManager,
                onNavigateToLogin = {
                    navController.navigate(Navigation.LOGIN) {
                        // Main 화면 포함 이전 화면들을 모두 백스택에서 제거하여 뒤로가기로 돌아갈 수 없게 함
                        popUpTo(Navigation.MAIN) { inclusive = true }
                    }
                },
                navigateToShorts = shouldNavigateToShorts.value,
                onShortsNavigationConsumed = { shouldNavigateToShorts.value = false },
                navigateToMyPage = shouldNavigateToMyPage.value,
                onMyPageNavigationConsumed = { shouldNavigateToMyPage.value = false }
            )
        }
    }
}