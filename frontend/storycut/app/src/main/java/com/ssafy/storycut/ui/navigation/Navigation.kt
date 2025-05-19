package com.ssafy.storycut.ui.navigation

import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import com.ssafy.storycut.ui.navigation.BottomNavigationViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

object Navigation {
    // 앱 최상위 경로
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"
    const val SHORTS = "shorts"

    // 메인 화면 내부 경로
    object Main {
        const val HOME = "home"
        const val EDIT = "edit"
        const val SHORTS_UPLOAD = "shorts_upload"
        const val MYPAGE = "mypage"
        const val SETTINGS = "settings"
    }
}

/**
 * 네비게이션 관련 확장 함수들
 */

// 로그인 화면으로 이동
fun NavController.navigateToLogin() {
    Log.d("Navigation", "로그인 화면으로 이동 시작")
    navigate(Navigation.LOGIN) {
        popUpTo(Navigation.MAIN) { inclusive = true }
        launchSingleTop = true
    }
}

// 메인 화면으로 이동
fun NavController.navigateToMain() {
    navigate(Navigation.MAIN) {
        popUpTo(Navigation.SPLASH) { inclusive = true }
    }
}

// 스플래시 화면으로 이동
fun NavController.navigateToSplash() {
    navigate(Navigation.SPLASH)
}

// 쇼츠 화면으로 이동
fun NavController.navigateToShorts() {
    navigate(Navigation.SHORTS)
}

// 하단 네비게이션
fun NavController.navigateToMainTab(route: String, hideBottomNav: Boolean = false) {
    Log.d("Navigation", "메인 탭 이동: $route, 하단 네비게이션 숨김: $hideBottomNav")

    // HOME 화면으로 이동할 때는 항상 하단 네비게이션을 숨김
    val shouldHideBottomNav = hideBottomNav || route == Navigation.Main.HOME

    // 완전히 새로운 화면으로 이동하도록 설정
    navigate(route) {
        // 백스택 완전히 정리 (MAIN까지의 모든 백스택 삭제)
        popUpTo(Navigation.MAIN) {
            // MAIN 포함 여부 - 여기서는 제외하여 MAIN은 남기고 그 위의 화면들만 제거
            inclusive = false

            // 상태 저장 안 함
            saveState = false
        }

        // 같은 화면 여러번 쌓이지 않도록
        launchSingleTop = true

        // 이전 상태 복원 안 함
        restoreState = false
    }

    // 하단 네비게이션 숨김 상태를 ViewModel을 통해 저장
    if (shouldHideBottomNav) {
        val viewModel = BottomNavigationViewModel()
        viewModel.setBottomNavVisibility(false)
    }
}
