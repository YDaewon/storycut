package com.ssafy.storycut.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.storycut.R

/**
 * 구글 소셜 로그인 화면 컴포저블
 * 사용자가 구글 계정으로 로그인할 수 있는 UI를 제공합니다.
 * @param viewModel 인증 관련 로직을 처리하는 ViewModel (Hilt를 통해 주입)
 */
@Composable
fun GoogleSignInScreen(
    viewModel: AuthViewModel = hiltViewModel() // Hilt를 사용하여 AuthViewModel 주입
) {
    val context = LocalContext.current // 현재 컨텍스트 가져오기
    val credentialManager = remember { CredentialManager.create(context) } // CredentialManager 인스턴스 생성 및 remember로 재구성 방지
    val scrollState = rememberScrollState() // 스크롤 상태 기억
    
    // AuthViewModel의 uiState를 컴포즈로 수집
    val authUiState by viewModel.uiState.collectAsState()

    // 메인 컬럼 레이아웃
    Column(
        modifier = Modifier
            .fillMaxSize() // 전체 화면 차지
            .background(Color.White) // 배경색 흰색으로 설정
            .padding(16.dp) // 여백 추가
            .verticalScroll(scrollState), // 세로 스크롤 적용
        horizontalAlignment = Alignment.CenterHorizontally, // 가로 중앙 정렬
        verticalArrangement = Arrangement.Center // 세로 중앙 정렬로 변경
    ) {
        // 로고 이미지 추가
        Image(
            painter = painterResource(id = R.drawable.logo), // 구글 로고 이미지 리소스 (추가 필요)
            contentDescription = "Service Logo",
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 타이틀 텍스트
        Text(
            text = "Google로 로그인",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF202124) // 구글 텍스트 색상
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 서브타이틀 텍스트
        Text(
            text = "계속하려면 Google 계정을 선택하세요",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF5F6368) // 구글 서브텍스트 색상
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 여기서 기존 버튼 대신 PNG 이미지로 대체
        Image(
            painter = painterResource(id = R.drawable.glogo), // 구글 로그인 버튼 PNG (추가 필요)
            contentDescription = "Google Sign In Button",
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .clickable {
                    viewModel.signInWithGoogle(context, credentialManager) // 구글 로그인 처리 함수 호출
                }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 로딩 상태에서만 로딩 인디케이터 표시
        when (authUiState) {
            is AuthUiState.Loading -> {
                CircularProgressIndicator(
                    color = Color(0xFF4285F4) // 구글 블루 색상
                )
            }
            
            is AuthUiState.Error -> {
                val errorState = authUiState as AuthUiState.Error
                
                Text(
                    text = "로그인 실패", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEA4335) // 구글 레드 색상
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5F6368)
                )
            }
            
            else -> {} // Initial, Success 상태에서는 아무것도 표시하지 않음
        }
    }
}