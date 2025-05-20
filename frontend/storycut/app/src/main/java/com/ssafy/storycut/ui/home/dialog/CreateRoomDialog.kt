package com.ssafy.storycut.ui.home.dialog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.ssafy.storycut.R
import com.ssafy.storycut.data.api.model.room.CreateRoomRequest
@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreateRoom: (CreateRoomRequest, Uri?) -> Unit
) {
    var roomTitle by remember { mutableStateOf("") }
    var roomPassword by remember { mutableStateOf("") }
    var roomContext by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf(false) }

    // 이미지 URI를 저장할 상태 변수
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // 현재 컨텍스트
    val context = LocalContext.current

    // 제목 유효성 검사 함수 추가
    val isTitleValid = roomTitle.length >= 2

    // 이미지 선택을 위한 액티비티 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
        }
    }

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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "공유방 생성",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 썸네일 이미지 선택 영역 (기존 코드 유지)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFCF7F0))
                        .clickable {
                            galleryLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 기존 이미지 처리 코드 유지
                    if (imageUri != null) {
                        // 선택한 이미지 표시
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(data = imageUri)
                                    .build()
                            ),
                            contentDescription = "선택한 썸네일",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 이미지 변경/삭제 아이콘
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            // 삭제 아이콘
                            Icon(
                                painter = painterResource(id = R.drawable.glogo),
                                contentDescription = "이미지 삭제",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                                    .clickable {
                                        imageUri = null
                                    }
                            )
                        }
                    } else {
                        // 이미지 선택 안내 표시
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.copy),
                                contentDescription = "이미지 선택",
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "썸네일 이미지 선택",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // 이미지 미선택 시 기본 이미지 사용 안내
                if (imageUri == null) {
                    Text(
                        text = "이미지를 선택하지 않으면 기본 이미지가 사용됩니다",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 방 제목 입력 (수정됨 - 유효성 검사 조건 추가)
                OutlinedTextField(
                    value = roomTitle,
                    onValueChange = { roomTitle = it },
                    label = { Text("방 제목 (2자 이상)") },
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
                    isError = showErrorMessage && (!isTitleValid)
                )

                // 나머지 필드는 동일하게 유지
                OutlinedTextField(
                    value = roomPassword,
                    onValueChange = { roomPassword = it },
                    label = { Text("비밀번호 (선택)") },
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
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(
                                    id = if (passwordVisible)
                                        R.drawable.visibility_on
                                    else
                                        R.drawable.visibility_off
                                ),
                                contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    singleLine = true
                )

                // 방 설명 입력
                OutlinedTextField(
                    value = roomContext,
                    onValueChange = { roomContext = it },
                    label = { Text("방 설명") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
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
                    isError = showErrorMessage && roomContext.isBlank()
                )

                // 에러 메시지 (수정됨 - 제목 길이 조건 추가)
                if (showErrorMessage) {
                    when {
                        roomTitle.isBlank() || roomContext.isBlank() -> {
                            Text(
                                text = "방 제목과 설명은 필수 입력 항목입니다",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        !isTitleValid -> {
                            Text(
                                text = "방 제목은 2자 이상이어야 합니다",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // 버튼 영역 (수정됨 - 유효성 검사 조건 추가)
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
                            if (roomTitle.isBlank() || roomContext.isBlank() || !isTitleValid) {
                                showErrorMessage = true
                            } else {
                                // CreateRoomRequest 객체 생성
                                val request = CreateRoomRequest(
                                    roomTitle = roomTitle,
                                    roomPassword = if (roomPassword.isBlank()) null else roomPassword,
                                    roomContext = roomContext,
                                    roomThumbnail = ""
                                )
                                // 다이얼로그 닫기
                                onDismiss()
                                // 방 생성 요청
                                onCreateRoom(request, imageUri)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0B699)
                        )
                    ) {
                        Text(text = "생성")
                    }
                }
            }
        }
    }
}