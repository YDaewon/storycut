package com.ssafy.storycut.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNicknameScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
) {
    val currentNickname = settingsViewModel.currentUserNickname.collectAsState().value ?: ""
    var nickname by remember { mutableStateOf(currentNickname) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    // 화면 진입 시 현재 사용자 정보 가져오기
    LaunchedEffect(Unit) {
        settingsViewModel.fetchCurrentUser()
    }

    // 닉네임 수정 결과 수집
    LaunchedEffect(settingsViewModel) {
        settingsViewModel.updateResult.collect { result ->
            isLoading = false
            when (result) {
                is SettingsViewModel.UpdateResult.Success -> {
                    showSuccessMessage = true
                    errorMessage = null
                }
                is SettingsViewModel.UpdateResult.Error -> {
                    errorMessage = result.message
                    showSuccessMessage = false
                }
                null -> {
                    showSuccessMessage = false
                    errorMessage = null
                }
            }
        }
    }

    // 베이지/브라운 계열 색상 정의
    val primaryButtonColor = Color(0xFFD0B699)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("닉네임 수정") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 현재 닉네임 표시
            Text(
                text = "현재 닉네임",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Text(
                text = currentNickname,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 새 닉네임 입력 필드
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    errorMessage = null
                    showSuccessMessage = false
                },
                label = { Text("새 닉네임") },
                singleLine = true,
                isError = errorMessage != null,
                supportingText = {
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (nickname.isNotBlank() && nickname != currentNickname) {
                            isLoading = true
                            settingsViewModel.updateNickname(nickname)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                        focusedLabelColor = Color(0xFFD0B699),
                unfocusedLabelColor = Color.LightGray,
                focusedBorderColor = Color(0xFFD0B699),
                unfocusedBorderColor = Color.White,
                focusedContainerColor = Color(0xFFFCF7F0),
                unfocusedContainerColor = Color(0xFFFCF7F0),
                cursorColor = Color(0xFFD0B699),
                selectionColors = TextSelectionColors(
                    handleColor = Color(0xFFD0B699),
                    backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 수정 버튼 - 새로운 색상과 모양 적용
            Button(
                onClick = {
                    if (nickname.isNotBlank() && nickname != currentNickname) {
                        focusManager.clearFocus()
                        isLoading = true
                        settingsViewModel.updateNickname(nickname)
                    } else if (nickname.isBlank()) {
                        errorMessage = "닉네임을 입력해주세요"
                    } else if (nickname == currentNickname) {
                        errorMessage = "현재 닉네임과 동일합니다"
                    }
                },
                enabled = !isLoading && nickname.isNotBlank() && nickname != currentNickname,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryButtonColor,
                    disabledContainerColor = primaryButtonColor.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("수정하기", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 성공 메시지
            if (showSuccessMessage) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = primaryButtonColor.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "닉네임이 성공적으로 변경되었습니다.",
                            color = Color(0xFFF4B3621),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}