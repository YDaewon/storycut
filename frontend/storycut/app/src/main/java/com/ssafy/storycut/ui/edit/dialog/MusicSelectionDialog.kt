package com.ssafy.storycut.ui.edit.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun MusicSelectionDialog(
    onDismiss: () -> Unit,
    onAutoMusicClick: () -> Unit,
    onPromptMusicClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 안내 텍스트
                Text(
                    text = "배경 음악 만들기 방식을 선택하세요",
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // 자동으로 만들기 옵션
                Button(
                    onClick = {
                        onAutoMusicClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0B699)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("자동으로 만들기", color = Color.White)
                }

                // 프롬프트로 만들기 옵션
                Button(
                    onClick = {
                        onPromptMusicClick()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0B699)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("프롬프트로 만들기", color = Color.White)
                }
            }
        }
    }
}