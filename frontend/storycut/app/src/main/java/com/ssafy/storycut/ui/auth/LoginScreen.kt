package com.ssafy.storycut.ui.auth

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    GoogleSignInScreen(viewModel = authViewModel)

    // 인증 상태 관찰
    val authState by authViewModel.uiState.collectAsState()
    val userInfo by authViewModel.userState.collectAsState()

    // LaunchedEffect를 사용하여 authState 와 userInfo 값이 변경될 때마다 체크
    LaunchedEffect(authState, userInfo) {
        when (authState) {
            is AuthUiState.Success -> {
                if (userInfo != null) {
                    Log.d(TAG, "로그인 성공, 사용자 정보 확인 후 메인 화면으로 이동")
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    Log.d(TAG, "로그인 성공했으나 사용자 정보가 없음, 재시도 후 시간 지연")
                    // 재시도 로직 추가 가능
                }
            }
            is AuthUiState.Error -> {
                // 오류 발생 시 로그 출력
                val errorMsg = (authState as AuthUiState.Error).message
                Log.e(TAG, "로그인 오류: $errorMsg")
            }
            else -> {
                // 다른 상태는 무시
            }
        }
    }
}