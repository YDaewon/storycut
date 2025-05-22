package com.ssafy.storycut.ui.edit.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun OptionDialog(
    onDismiss: () -> Unit,
    onMosaicClick: () -> Unit,
    onKoreanSubtitleClick: () -> Unit,
    onBackgroundMusicClick: () -> Unit,
    hasMosaic: Boolean = false,
    hasKoreanSubtitle: Boolean = false,
    hasBackgroundMusic: Boolean = false
) {
    // 표시할 옵션이 없으면 대화창을 닫음
    if (hasMosaic && hasKoreanSubtitle && hasBackgroundMusic) {
        onDismiss()
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color.Gray.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 21.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 안내 텍스트
                Text(
                    text = "추가할 옵션을 선택하세요",
                    fontSize = 14.sp,
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // 모자이크 옵션 (아직 추가되지 않은 경우에만 표시)
                if (!hasMosaic) {
                    Button(
                        onClick = {
                            onMosaicClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0B699)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("모자이크", color = Color.White)
                    }
                }

                // 한국어 자막 옵션 (아직 추가되지 않은 경우에만 표시)
                if (!hasKoreanSubtitle) {
                    Button(
                        onClick = {
                            onKoreanSubtitleClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0B699)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("한국어 자막 추가", color = Color.White)
                    }
                }

                // 배경 음악 추가 옵션 (아직 추가되지 않은 경우에만 표시)
                if (!hasBackgroundMusic) {
                    Button(
                        onClick = {
                            onBackgroundMusicClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0B699)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("배경 음악 추가", color = Color.White)
                    }
                }
            }
        }
    }
}