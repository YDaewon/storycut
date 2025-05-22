package com.ssafy.storycut.ui.room.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.storycut.ui.room.RoomViewModel
import com.ssafy.storycut.ui.room.setting.AnimatedRoomEditNavigation
import com.ssafy.storycut.ui.room.setting.RoomSettingScreen

@Composable
fun AnimatedRoomSettingsNavigation(
    roomId: String,
    roomViewModel: RoomViewModel = hiltViewModel(),
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onRoomEdit: (String) -> Unit = {},
    onLeaveRoom: () -> Unit = {}
) {
    // 시스템 뒤로가기 버튼 처리
    BackHandler(enabled = isVisible) {
        onDismiss()
    }

    // 방 정보 수정 화면 표시 상태
    var showRoomEdit by remember { mutableStateOf(false) }

    // 설정 화면은 콘텐츠 영역을 완전히 덮는 오버레이로 표시
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // 방 설정 화면 표시
                    RoomSettingScreen(
                        roomId = roomId,
                        roomViewModel = roomViewModel,
                        onBackPressed = onDismiss,
                        onRoomEdit = {
                            // 방 정보 수정 화면 표시
                            showRoomEdit = true
                        },
                        onLeaveRoom = onLeaveRoom
                    )
                }
            }

            // 방 정보 수정 화면
            AnimatedRoomEditNavigation(
                roomId = roomId,
                roomViewModel = roomViewModel,
                isVisible = showRoomEdit,
                onDismiss = { showRoomEdit = false }
            )
        }
    }
}