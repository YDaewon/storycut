package com.ssafy.storycut.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.storycut.R
import com.ssafy.storycut.data.api.model.room.RoomDto
import com.ssafy.storycut.ui.home.dialog.CreateRoomDialog
import com.ssafy.storycut.ui.home.dialog.EnterRoomDialog
import com.ssafy.storycut.ui.home.dialog.LeaveRoomDialog
import com.ssafy.storycut.ui.home.dialog.RoomOptionsDialog
import com.ssafy.storycut.ui.navigation.BottomNavigationViewModel
import com.ssafy.storycut.ui.navigation.Navigation
import com.ssafy.storycut.ui.navigation.navigateToMainTab

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    bottomNavViewModel: BottomNavigationViewModel = hiltViewModel(),
    onRoomClick: (String) -> Unit = {},
    onCreateRoomClick: () -> Unit = {},
    onEnterRoomClick: () -> Unit = {},
    navController: NavHostController? = null
) {
    // ViewModel에서 방 목록 데이터 가져오기
    val myRooms by homeViewModel.myRooms.observeAsState(emptyList())
    val isLoading by homeViewModel.isLoading.observeAsState(false)
    val error by homeViewModel.error.observeAsState("")

    // 새로 생성된 방 ID 관찰
    val createdRoomId by homeViewModel.createdRoomId.observeAsState("")
    val enteredRoomId by homeViewModel.enteredRoomId.observeAsState("")

    // 다이얼로그 표시 상태
    var showRoomOptionsDialog by remember { mutableStateOf(false) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var showEnterRoomDialog by remember { mutableStateOf(false) }
    var showLeaveRoomDialog by remember { mutableStateOf(false) }
    var selectedRoom by remember { mutableStateOf<RoomDto?>(null) }

    // 생성 또는 입장 진행 상태
    var isProcessingRoom by remember { mutableStateOf(false) }

    // 스크롤 상태
    val lazyListState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    // 스크롤 상태 변경을 위한 상태
    var shouldScrollToTop by remember { mutableStateOf(false) }

    // 스크롤 처리
    LaunchedEffect(shouldScrollToTop) {
        if (shouldScrollToTop) {
            lazyListState.animateScrollToItem(0)
            shouldScrollToTop = false
        }
    }

    // 새로 생성된 방으로 자동 이동
    LaunchedEffect(createdRoomId) {
        if (createdRoomId.isNotEmpty()) {
            isProcessingRoom = false
            onRoomClick(createdRoomId)
            homeViewModel.clearCreatedRoomId()
        }
    }

    // 입장한 방으로 자동 이동
    LaunchedEffect(enteredRoomId) {
        if (enteredRoomId.isNotEmpty()) {
            isProcessingRoom = false
            onRoomClick(enteredRoomId)
            homeViewModel.clearEnteredRoomId()
        }
    }

    // 컴포넌트가 처음 표시될 때 데이터 로드
    LaunchedEffect(key1 = true) {
        homeViewModel.getMyRooms()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 로딩 상태 또는 에러 표시
        if (isLoading && !isProcessingRoom) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // 전체 콘텐츠를 하나의 LazyColumn으로 구성
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 상단 영역 (FCF7F0 + 곡선)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // FCF7F0 색상 배경 (로고와 버튼들)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFCF7F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp) // 요소 간 간격 감소 (16.dp -> 12.dp)
                            ) {
                                // 로고 이미지
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "StoryCut",
                                    modifier = Modifier
                                        .padding(vertical = 8.dp) // 패딩 감소 (16.dp -> 8.dp)
                                        .height(80.dp)
                                )

                                // 버튼 1: YouTube 업로드
                                Button(
                                    onClick = {
                                        navController?.navigateToMainTab(Navigation.Main.SHORTS_UPLOAD, hideBottomNav = false, bottomNavViewModel = bottomNavViewModel)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD0B699)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "YouTube 업로드",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.icon_tube),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                // 버튼 2: 영상 편집
                                Button(
                                    onClick = {
                                        // 영상 편집 기능
                                        navController?.navigateToMainTab(Navigation.Main.EDIT, hideBottomNav = false, bottomNavViewModel = bottomNavViewModel)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD0B699)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "영상 편집",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.icon_edit),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                // 버튼 3: 마이페이지
                                Button(
                                    onClick = {
                                        navController?.navigateToMainTab(Navigation.Main.MYPAGE, hideBottomNav = false, bottomNavViewModel = bottomNavViewModel)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD0B699)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "마이페이지",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.icon_me),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 헤더 (Room 타이틀과 Add 버튼 + 곡선)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)  // 헤더 영역 높이 지정
                    ) {
                        // 배경에 곡선 그리기
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 배경을 FCF7F0로 채우기
                            drawRect(color = Color(0xFFFCF7F0))

                            // 흰색 곡선 그리기 (이전 버전처럼 부드럽게)
                            val path = Path().apply {
                                // 시작점 (왼쪽 상단)
                                moveTo(0f, 0f)
                                // 우측 방향으로 직선
                                lineTo(size.width * 0.7f, 0f)
                                // 부드러운 곡선으로 우측 하단으로 이동
                                cubicTo(
                                    size.width * 0.85f, 0f, // 첫 번째 제어점
                                    size.width, size.height * 0.3f, // 두 번째 제어점
                                    size.width, size.height // 끝점 (우측 하단)
                                )
                                // 좌측 하단까지 직선
                                lineTo(0f, size.height)
                                // 시작점으로 돌아가기
                                close()
                            }
                            drawPath(
                                path = path,
                                color = Color.White // 흰색 배경
                            )
                        }

                        // 참가중인 방 텍스트와 추가 버튼을 함께 배치
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)  // 16.dp에서 24.dp로 증가
                        ) {
                            // 참가중인 방 텍스트
                            Text(
                                text = "참가중인 방",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // 추가 버튼
                            Button(
                                onClick = { showRoomOptionsDialog = true },
                                modifier = Modifier.size(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE8C999)
                                ),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "추가",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 방 목록 표시 (방이 없는 경우)
                if (myRooms.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "참가중인 공유방이 없습니다",
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    // 방 목록 표시 (방이 있는 경우)
                    item {
                        Spacer(modifier = Modifier.height(12.dp))  // 간격 추가
                    }
                    items(myRooms) { room ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            RoomItemWithLongPress(
                                room = room,
                                onClick = { onRoomClick(room.roomId.toString()) },
                                onLongClick = {
                                    selectedRoom = room
                                    showLeaveRoomDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // 다이얼로그들과 진행 중 오버레이 표시
        if (showRoomOptionsDialog) {
            RoomOptionsDialog(
                onDismiss = { showRoomOptionsDialog = false },
                onCreateRoom = {
                    showRoomOptionsDialog = false
                    showCreateRoomDialog = true
                },
                onEnterRoom = {
                    showRoomOptionsDialog = false
                    showEnterRoomDialog = true
                }
            )
        }

        if (showCreateRoomDialog) {
            CreateRoomDialog(
                onDismiss = { showCreateRoomDialog = false },
                onCreateRoom = { request, imageUri ->
                    showCreateRoomDialog = false
                    isProcessingRoom = true
                    homeViewModel.createRoom(request, imageUri)
                }
            )
        }

        if (showEnterRoomDialog) {
            EnterRoomDialog(
                onDismiss = { showEnterRoomDialog = false },
                onEnterRoom = { inviteCode, password ->
                    showEnterRoomDialog = false
                    isProcessingRoom = true
                    homeViewModel.enterRoom(inviteCode, password)
                }
            )
        }

        // 방 나가기 다이얼로그
        if (showLeaveRoomDialog) {
            LeaveRoomDialog(
                room = selectedRoom,
                onDismiss = { showLeaveRoomDialog = false },
                onLeaveRoom = { roomId ->
                    homeViewModel.leaveRoom(roomId)
                    showLeaveRoomDialog = false
                }
            )
        }

        if (isProcessingRoom) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFD0B699)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "방으로 이동 중...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}