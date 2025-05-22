package com.ssafy.storycut.ui.splash

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.ssafy.storycut.R
import com.ssafy.storycut.ui.auth.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.ssafy.storycut.ui.splash.components.Loader

private const val TAG = "SplashScreen"

@Composable
fun SplashScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    val userInfo by authViewModel.userState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 토큰 체크 완료 여부를 추적하는 상태
    var hasValidToken by remember { mutableStateOf(false) }
    var tokenCheckCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            Log.d(TAG, "스플래시 화면: 토큰 유효성 확인 시작...")

            // 토큰 유효성 확인
            try {
                // 토큰 체크를 위한 변수 추가
                hasValidToken = false

                // AuthViewModel의 checkTokenValidity 호출
                authViewModel.checkTokenValidity()

                // 최소 1.5초 동안 스플래시 화면 보여주기
                delay(1500)

                // 토큰 유효성 확인 결과 (userInfo가 null이 아니면 토큰이 유효하다고 판단)
                hasValidToken = userInfo != null

                Log.d(TAG, "스플래시 화면: 토큰 체크 완료, 유효한 토큰: $hasValidToken, 사용자 정보: ${userInfo != null}")

                // 토큰이 유효하고 사용자 정보가 있는 경우 메인 화면으로 이동
                if (hasValidToken && userInfo != null) {
                    Log.d(TAG, "스플래시 화면: 메인 화면으로 이동")
                    navController.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    // 토큰이 유효하지 않거나 사용자 정보가 없는 경우 로그인 화면으로 이동
                    Log.d(TAG, "스플래시 화면: 로그인 화면으로 이동")
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                // 오류 발생 시 로그인 화면으로 이동
                Log.e(TAG, "스플래시 화면: 토큰 체크 중 오류 발생", e)
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            } finally {
                tokenCheckCompleted = true
            }
        }
    }

    // 스플래시 화면 UI
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Loader()
        }
    }
}