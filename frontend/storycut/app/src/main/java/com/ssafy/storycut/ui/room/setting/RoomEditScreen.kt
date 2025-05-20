package com.ssafy.storycut.ui.room.setting

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.storycut.data.api.model.room.RoomDto
import com.ssafy.storycut.ui.room.RoomViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomEditScreen(
    roomId: String,
    roomViewModel: RoomViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onEditComplete: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 현재 방 정보 불러오기
    val room by roomViewModel.roomDetail.collectAsState()
    val isLoading by roomViewModel.isLoading.collectAsState()
    val error by roomViewModel.error.collectAsState()

    // 수정할 정보를 위한 상태 변수
    var roomTitle by remember { mutableStateOf("") }
    var roomContext by remember { mutableStateOf("") }
    var hasPassword by remember { mutableStateOf(false) }
    var roomPassword by remember { mutableStateOf("") }

    // 에러 메시지 상태
    var titleError by remember { mutableStateOf(false) }
    var contextError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    // 방 정보 초기화
    LaunchedEffect(room) {
        room?.let {
            roomTitle = it.roomTitle
            roomContext = it.roomContext
            hasPassword = it.hasPassword
        }
    }

    // 입력값 검증 함수
    val validateInputs = {
        var isValid = true

        if (roomTitle.isBlank()) {
            titleError = true
            isValid = false
        }

        if (roomContext.isBlank()) {
            contextError = true
            isValid = false
        }

        if (hasPassword && roomPassword.isBlank()) {
            passwordError = true
            isValid = false
        }

        isValid
    }

    // 방 정보 업데이트 함수
    val updateRoomInfo = {
        room?.let {
            val updatedRoom = RoomDto(
                roomId = it.roomId,
                hostId = it.hostId,
                roomTitle = roomTitle,
                roomContext = roomContext,
                hasPassword = hasPassword,
                roomThumbnail = it.roomThumbnail,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                memberCount = it.memberCount,
                host = it.host
            )

            coroutineScope.launch {
                // 비밀번호 전달
                roomViewModel.updateRoom(updatedRoom, if (hasPassword) roomPassword else null)

                // 에러가 없으면 성공으로 간주
                if (roomViewModel.error.value.isEmpty()) {
                    onEditComplete()
                }
            }
        }
    }

    // 오류 발생 시 처리
    LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            // 여기서 토스트 메시지를 표시할 수 있습니다
        }
    }

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("방 정보 수정") },
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
                ),
                actions = {
                    TextButton(
                        onClick = {
                            if (validateInputs()) {
                                updateRoomInfo()
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            "저장",
                            color = Color(0xFFD0B699),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFD0B699)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 방 제목 입력 필드
                Text(
                    text = "방 제목",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = roomTitle,
                    onValueChange = { roomTitle = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFCF7F0)),
                    placeholder = { Text("방 제목을 입력하세요 (필수)", color = Color.LightGray)},
                    singleLine = true,
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("방 제목을 입력해주세요") }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0B699),
                        focusedLabelColor = Color(0xFFD0B699),
                        unfocusedContainerColor = Color(0xFFFCF7F0),
                        focusedContainerColor = Color(0xFFFCF7F0),
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color(0xFFD0B699),
                        selectionColors = TextSelectionColors(
                            handleColor = Color(0xFFD0B699),
                            backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
                        )
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 방 설명 입력 필드
                Text(
                    text = "방 설명",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = roomContext,
                    onValueChange = {
                        roomContext = it
                        contextError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFFFCF7F0)),
                    placeholder = { Text("방 설명을 입력하세요 (필수)", color = Color.LightGray) },
                    isError = contextError,
                    supportingText = if (contextError) {
                        { Text("방 설명을 입력해주세요") }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0B699),
                        focusedLabelColor = Color(0xFFD0B699),
                        unfocusedBorderColor = Color.White,
                        cursorColor = Color(0xFFD0B699),
                        selectionColors = TextSelectionColors(
                            handleColor = Color(0xFFD0B699),
                            backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
                        )
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 비밀번호 설정
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "비밀번호 설정",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = hasPassword,
                        onCheckedChange = { hasPassword = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFD0B699),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }

                // 비밀번호 입력 필드 (hasPassword가 true일 때만 표시)
                AnimatedVisibility(visible = hasPassword) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = roomPassword,
                            onValueChange = {
                                roomPassword = it
                                passwordError = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFCF7F0)),
                            placeholder = { Text("비밀번호를 입력하세요", color = Color.LightGray) },
                            singleLine = true,
                            isError = passwordError,
                            supportingText = if (passwordError) {
                                { Text("비밀번호를 입력해주세요") }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0B699),
                                focusedLabelColor = Color(0xFFD0B699),
                                unfocusedBorderColor = Color.White,
                                cursorColor = Color(0xFFD0B699),
                                selectionColors = TextSelectionColors(
                                    handleColor = Color(0xFFD0B699),
                                    backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
                                )
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
fun AnimatedRoomEditNavigation(
    roomId: String,
    roomViewModel: RoomViewModel = hiltViewModel(),
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    // 시스템 뒤로가기 버튼 처리
    BackHandler(enabled = isVisible) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = Modifier.zIndex(if (isVisible) 10f else 0f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .imePadding()
        ) {
            RoomEditScreen(
                roomId = roomId,
                roomViewModel = roomViewModel,
                onBackPressed = onDismiss,
                onEditComplete = onDismiss // onEditSuccess에서 onEditComplete로 수정
            )
        }
    }
}