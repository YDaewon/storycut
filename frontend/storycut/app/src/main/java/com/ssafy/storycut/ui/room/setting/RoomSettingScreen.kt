package com.ssafy.storycut.ui.room.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.storycut.ui.room.RoomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSettingScreen(
    roomId: String,
    roomViewModel: RoomViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onRoomEdit: (String) -> Unit = {},
    onLeaveRoom: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }

    // 컬렉트 스테이트를 사용하여 StateFlow 값 수집
    val room by roomViewModel.roomDetail.collectAsState()

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // 전체 화면 설정
        topBar = {
            TopAppBar(
                title = { Text("방 설정") },
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
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 방 제목 표시
            Text(
                text = room?.roomTitle ?: "방 제목",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 방 정보 수정
            RoomSettingItem(
                title = "방 정보 수정",
                icon = Icons.Default.Edit,
                onClick = { onRoomEdit(roomId) },
                showDivider = true
            )

            // 방 나가기
            RoomSettingItem(
                title = "방 나가기",
                icon = Icons.Default.ExitToApp,
                onClick = { showLeaveConfirmDialog = true },
                isWarning = true,
                showDivider = false
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 방 나가기 확인 다이얼로그
    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text("방 나가기") },
            text = {
                Text("정말로 이 방을 나가시겠습니까? 초대 코드가 없으면 다시 입장할 수 없습니다.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeaveRoom()
                        showLeaveConfirmDialog = false
                    }
                ) {
                    Text("나가기", color = Color(0xFFD0B699))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun RoomSettingItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isWarning: Boolean = false,
    showDivider: Boolean = true
) {
    val textColor = if (isWarning) {
        Color(0xFFD0B699) // 앱 테마 색상으로 변경
    } else {
        Color.DarkGray
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