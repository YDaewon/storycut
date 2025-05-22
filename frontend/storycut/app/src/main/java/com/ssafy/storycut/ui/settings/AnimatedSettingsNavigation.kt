package com.ssafy.storycut.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.storycut.ui.auth.AuthViewModel

@Composable
fun AnimatedSettingsNavigation(
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onProfileUpdated: () -> Unit = {} // 프로필 업데이트 콜백 추가
) {
    BackHandler(enabled = isVisible) {
        onDismiss()
    }

    // 화면 상태 관리 (설정 메인 / 닉네임 수정)
    var currentScreen by remember { mutableStateOf<SettingsScreen>(SettingsScreen.Main) }

    // 애니메이션 상태 관리
    var animationState by remember { mutableStateOf(false) }

    // isVisible이 변경될 때 애니메이션 상태 업데이트
    LaunchedEffect(isVisible) {
        animationState = isVisible
        // visible -> invisible로 변할 때 메인 화면으로 초기화
        if (!isVisible) {
            currentScreen = SettingsScreen.Main
        }
    }

    // 닉네임 수정 결과 모니터링
    LaunchedEffect(key1 = Unit) {
        settingsViewModel.updateResult.collect { result ->
            if (result is SettingsViewModel.UpdateResult.Success) {
                // 닉네임 업데이트 성공 시 콜백 호출
                onProfileUpdated()
            }
        }
    }

    if (animationState || isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (isVisible) 10f else 0f)
                .animateContentSize(animationSpec = tween(300))
                .offset(
                    x = androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (isVisible) 0.dp else 1000.dp,
                        animationSpec = tween(300)
                    ).value
                )
                .background(Color.White)
        ) {
            // 현재 화면에 따라 다른 컴포저블 표시
            when (currentScreen) {
                is SettingsScreen.Main -> {
                    SettingsScreen(
                        authViewModel = authViewModel,
                        onBackPressed = onDismiss,
                        onNavigateToLogin = onNavigateToLogin,
                        onNavigateToEditNickname = {
                            currentScreen = SettingsScreen.EditNickname
                        }
                    )
                }
                is SettingsScreen.EditNickname -> {
                    EditNicknameScreen(
                        settingsViewModel = settingsViewModel,
                        onBackPressed = {
                            currentScreen = SettingsScreen.Main
                        }
                    )
                }
            }
        }

        // 애니메이션이 완료된 후 상태 업데이트
        LaunchedEffect(isVisible) {
            kotlinx.coroutines.delay(300)
            if (!isVisible) {
                animationState = false
            }
        }
    }
}

// 화면 상태를 나타내는 sealed class
sealed class SettingsScreen {
    object Main : SettingsScreen()
    object EditNickname : SettingsScreen()
}