package com.ssafy.storycut.ui.home.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ssafy.storycut.data.api.model.room.RoomDto


@Composable
fun LeaveRoomDialog(
    room: RoomDto?,
    onDismiss: () -> Unit,
    onLeaveRoom: (String) -> Unit
) {
    if (room == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("방 나가기") },
        text = { Text("\"${room.roomTitle}\" 방에서 나가시겠습니까?") },
        confirmButton = {
            Button(
                onClick = {
                    onLeaveRoom(room.roomId.toString())
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("나가기")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray
                )
            ) {
                Text("취소")
            }
        },
        containerColor = Color.White // 배경색을 하얀색으로 설정
    )
}