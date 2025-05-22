package com.ssafy.storycut.ui.home.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
@Composable
fun EnterRoomDialog(
    onDismiss: () -> Unit,
    onEnterRoom: (String, String?) -> Unit  // 초대코드와 비밀번호를 전달
) {
    var inviteCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }  // 비밀번호 추가
    var showErrorMessage by remember { mutableStateOf(false) }

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
                    text = "초대코드로 입장",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = { inviteCode = it },
                    label = { Text("초대코드") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
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
                    ),
                    singleLine = true,
                    isError = showErrorMessage && inviteCode.isBlank()
                )

                if (showErrorMessage && inviteCode.isBlank()) {
                    Text(
                        text = "초대코드를 입력해주세요",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // 비밀번호 입력 (선택 사항)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("비밀번호 (선택사항)") },
                    placeholder = { Text("비밀번호가 있는 경우 입력하세요", color = Color.LightGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                    ),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()  // 비밀번호 마스킹
                )

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
                        border = BorderStroke(1.dp, Color(0xFFD0B699))
                    ) {
                        Text(text = "취소")
                    }

                    Button(
                        onClick = {
                            if (inviteCode.isBlank()) {
                                showErrorMessage = true
                            } else {
                                // 비밀번호가 비어있으면 null로 전달
                                val passwordToSend = if (password.isBlank()) null else password
                                onEnterRoom(inviteCode, passwordToSend)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0B699)
                        )
                    ) {
                        Text(text = "입장")
                    }
                }
            }
        }
    }
}