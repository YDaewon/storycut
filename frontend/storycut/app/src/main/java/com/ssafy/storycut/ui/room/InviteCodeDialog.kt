package com.ssafy.storycut.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ssafy.storycut.R
import kotlinx.coroutines.delay

@Composable
fun InviteCodeDialog(
    inviteCode: String,
    initialRemainingSeconds: Int = 600, // 초기 남은 시간(초)을 매개변수로 받음
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    // 유효 시간을 표시하기 위한 카운트다운 상태
    val remainingTime = remember { mutableStateOf(initialRemainingSeconds) }

    // 카운트다운 효과
    LaunchedEffect(key1 = Unit) {
        while (remainingTime.value > 0) {
            delay(1000) // 1초마다 업데이트
            remainingTime.value -= 1
        }
    }

    // 분과 초로 변환
    val minutes = remainingTime.value / 60
    val seconds = remainingTime.value % 60

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "초대코드",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "아래 코드를 공유하여 친구를 초대하세요",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 유효 시간 표시 - 시간이 만료된 경우 표시 변경
                Text(
                    text = if (remainingTime.value > 0)
                        "유효 시간: ${minutes}분 ${seconds}초"
                    else
                        "유효 시간이 만료되었습니다. 새 코드를 생성하세요.",
                    fontSize = 12.sp,
                    color = when {
                        remainingTime.value <= 0 -> Color.Red
                        minutes < 2 -> Color.Red.copy(alpha = 0.8f)
                        else -> Color.Gray
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 초대코드 표시
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .background(
                            color = Color(0xFFFCF7F0),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inviteCode,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingTime.value > 0)
                            Color(0xFFD0B699)
                        else
                            Color.Gray,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )
                }

                // 버튼 영역
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD0B699)
                        ),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text(text = "닫기")
                    }

                    Button(
                        onClick = onCopy,
                        modifier = Modifier.weight(1f),
                        enabled = remainingTime.value > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0B699)
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.copy),
                            contentDescription = "복사",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "복사하기")
                    }
                }

                // 시간이 만료되었을 때 표시할 버튼
                if (remainingTime.value <= 0) {
                    Button(
                        onClick = onDismiss, // 닫기 후 새 코드 생성 로직은 상위 컴포넌트에서 처리
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(text = "새 코드 생성하기")
                    }
                }
            }
        }
    }
}