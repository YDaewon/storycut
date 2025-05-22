package com.ssafy.storycut.ui.settings

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ssafy.storycut.R
import com.ssafy.storycut.ui.auth.AuthViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToEditNickname: () -> Unit = {}, // 닉네임 수정 화면으로 이동하는 콜백 추가
) {
    val scrollState = rememberScrollState()
    // 회원탈퇴 다이얼로그 표시 상태
    val showDeleteAccountDialog = remember { mutableStateOf(false) }

    // 로그인 화면 이동 이벤트 수신
    LaunchedEffect(authViewModel) {
        authViewModel.navigationEvent.collect { event ->
            when (event) {
                is AuthViewModel.NavigationEvent.NavigateToLogin -> {
                    onNavigateToLogin()
                }
            }
        }
    }

    // 회원탈퇴 확인 다이얼로그
    if (showDeleteAccountDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog.value = false },
            title = { Text("회원탈퇴") },
            text = { Text("정말 탈퇴하시겠습니까? 모든 데이터가 삭제됩니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog.value = false
                        authViewModel.deleteAccount()
                    }
                ) {
                    Text("탈퇴", color = Color(0xFFD0B699))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog.value = false }
                ) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.White, // 명시적으로 배경색 흰색으로 설정
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White // TopAppBar 배경색도 흰색으로 설정
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White) // 컬럼 배경색도 흰색으로 설정
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 설정 항목 목록 - 일반 항목
            SettingItem(
                title = "닉네임 수정",
                icon = Icons.Default.Person,
                onClick = onNavigateToEditNickname, // 콜백 함수 연결
                showDivider = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 로그아웃 항목 - 이벤트 방식으로 처리
            SettingItemWithResourceIcon(
                title = "로그아웃",
                iconResId = R.drawable.setting_logout,
                onClick = {
                    // 로그아웃 함수 호출 후 네비게이션은 이벤트를 통해 처리
                    authViewModel.logout()
                },
                isWarning = true,
                showDivider = true
            )

            // 회원탈퇴 - 다이얼로그 표시로 변경
            SettingItemWithResourceIcon(
                title = "회원탈퇴",
                iconResId = R.drawable.setting_delete,
                onClick = { showDeleteAccountDialog.value = true },
                isWarning = true,
                showDivider = false
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isWarning: Boolean = false,
    showDivider: Boolean = true
) {
    val textColor = if (isWarning) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }

        if (!isWarning) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun SettingItemWithResourceIcon(
    title: String,
    iconResId: Int,
    onClick: () -> Unit,
    isWarning: Boolean = false,
    showDivider: Boolean = true
) {
    val textColor = if (isWarning) {
        Color(0xFFD0B699)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }

        if (!isWarning) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
    }
}