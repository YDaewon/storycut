package com.ssafy.storycut.ui.room

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.storycut.R
import com.ssafy.storycut.data.api.model.VideoDto
import com.ssafy.storycut.data.api.model.chat.ChatDto
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.ui.mypage.VideoViewModel
import com.ssafy.storycut.ui.room.dialog.ThumbnailEditDialog
import com.ssafy.storycut.ui.room.upload.UploadShortDialog
import com.ssafy.storycut.ui.common.VideoSelectorFullScreenDialog
import com.ssafy.storycut.ui.room.settings.AnimatedRoomSettingsNavigation
import com.ssafy.storycut.ui.room.video.RoomVideoItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: String,
    navController: NavController,
    roomViewModel: RoomViewModel = hiltViewModel(),
    videoViewModel: VideoViewModel = hiltViewModel(),
    tokenManager: TokenManager
) {
    val roomDetail by roomViewModel.roomDetail.collectAsState()
    val roomMembers by roomViewModel.roomMembers.collectAsState()
    val inviteCode by roomViewModel.inviteCode.collectAsState()
    val isLoading by roomViewModel.isLoading.collectAsState()
    val error by roomViewModel.error.collectAsState()
    val uploadSuccess by roomViewModel.uploadSuccess.collectAsState()
    val thumbnailUpdateSuccess by roomViewModel.thumbnailUpdateSuccess.collectAsState()
    val myVideos by videoViewModel.myVideos.collectAsState()

    // 비디오 관련 상태 가져오기
    val roomVideos by roomViewModel.roomVideos.collectAsState()
    val isVideosLoading by roomViewModel.isVideosLoading.collectAsState()
    val hasMoreVideos by roomViewModel.hasMoreVideos.collectAsState()
    val currentPage by roomViewModel.currentPage.collectAsState()

    // 검색 관련 상태 추가
    var searchQuery by remember { mutableStateOf("") }
    var filteredVideos by remember { mutableStateOf<List<ChatDto>>(emptyList()) }

    // 다이얼로그 상태들
    var showInviteCodeDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showVideoSelectorDialog by remember { mutableStateOf(false) }
    var showThumbnailEditDialog by remember { mutableStateOf(false) }

    // 설정 화면 표시 상태 추가
    var showRoomSettings by remember { mutableStateOf(false) }

    var selectedVideo by remember { mutableStateOf<VideoDto?>(null) }
    var title by remember { mutableStateOf("") }
    var isGeneratingCode by remember { mutableStateOf(false) }

    // 썸네일 편집용 이미지 URI
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 초기 로딩 완료 여부를 추적하는 상태 추가
    var initialLoadCompleted by remember { mutableStateOf(false) }

    // 마지막으로 확인한 스크롤 위치를 저장
    var lastCheckedScrollPosition by remember { mutableStateOf(0) }

    // 로딩이 진행되지 않을 때만 추가 데이터 로드 가능
    var canLoadMore by remember { mutableStateOf(true) }

    val customSelectionColors = TextSelectionColors(
        handleColor = Color(0xFFD0B699),
        backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                color = Color.Black
            ),
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFFD0B699)),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "검색",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "쇼츠 검색",
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "지우기",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        )
    }

    // 이미지 선택기를 위한 런처
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // 검색어 변경 시 비디오 목록 필터링
    LaunchedEffect(searchQuery, roomVideos) {
        filteredVideos = if (searchQuery.isBlank()) {
            roomVideos
        } else {
            roomVideos.filter { video ->
                video.title?.contains(searchQuery, ignoreCase = true) ?: false
            }
        }
    }

    // 개선된 스크롤 감지 로직
    LaunchedEffect(scrollState.value) {
        // 초기 로딩이 완료되고 로딩 중이 아닐 때만 스크롤 확인
        if (initialLoadCompleted && !isVideosLoading && canLoadMore) {
            // 현재 스크롤 위치가 마지막으로 확인한 위치보다 크고(아래로 스크롤된 경우)
            // 스크롤이 끝에 가까워졌는지 확인
            val scrollPosition = scrollState.value
            val isNearBottom = scrollPosition >= scrollState.maxValue - 500 // 마진을 더 크게 설정

            // 아래로 스크롤 중이고 하단에 가까우면 추가 데이터 로드
            if (scrollPosition > lastCheckedScrollPosition && isNearBottom && hasMoreVideos && searchQuery.isBlank()) {
                Log.d("RoomDetailScreen", "하단에 도달, 더 로드 시작: $scrollPosition / ${scrollState.maxValue}")
                canLoadMore = false // 중복 로드 방지
                roomViewModel.loadMoreVideos(roomId)

                // 로딩 후 잠시 대기 후 다시 로드 가능하도록 설정
                delay(1000) // 1초 대기
                canLoadMore = true
            }

            // 마지막 확인 위치 업데이트
            lastCheckedScrollPosition = scrollPosition
        }
    }

    // 비디오 로딩 상태가 변경될 때 실행
    LaunchedEffect(isVideosLoading) {
        if (!isVideosLoading) {
            // 로딩이 완료되면 스크롤 위치 초기화하지 않고 그대로 유지
            canLoadMore = true
        }
    }

    // 다이얼로그 상태 초기화 함수
    fun resetDialogStates() {
        selectedVideo = null
        title = ""
        selectedImageUri = null
    }

    // 토스트 메시지를 위한 함수
    val showToast: (String) -> Unit = { message ->
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // 방 ID를 기준으로 최초 1회만 데이터 로드
    LaunchedEffect(roomId) {
        try {
            Log.d("RoomDetailScreen", "방 데이터 초기 로드 시작")

            // 초기 로딩 상태 설정
            initialLoadCompleted = false
            lastCheckedScrollPosition = 0

            // 방 정보 로드
            roomViewModel.getRoomDetail(roomId)
            roomViewModel.getRoomMembers(roomId)

            // 비디오 목록 로드 - 새로고침 플래그를 true로 설정하여 첫 페이지만 로드
            roomViewModel.getRoomVideos(roomId, true)

            // 내 비디오 목록도 미리 로드
            videoViewModel.fetchMyVideos()

            // 잠시 대기 후 초기 로딩 완료 표시
            delay(500) // 0.5초 대기 (UI가 업데이트될 시간 확보)
            initialLoadCompleted = true
            Log.d("RoomDetailScreen", "방 데이터 초기 로드 완료")
        } catch (e: Exception) {
            Log.e("RoomDetailScreen", "데이터 로드 실패: ${e.message}")
            initialLoadCompleted = true // 오류가 발생해도 로딩 상태 해제
        }
    }

    // 업로드 성공 시 처리
    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            showToast("쇼츠가 성공적으로 업로드되었습니다")
            roomViewModel.resetUploadState()
            // 업로드 성공 후 비디오 목록 새로고침
            roomViewModel.refreshRoomVideos(roomId)
        }
    }

    // 썸네일 업데이트 성공 시 처리
    LaunchedEffect(thumbnailUpdateSuccess) {
        if (thumbnailUpdateSuccess) {
            showToast("썸네일이 성공적으로 업데이트되었습니다")
            roomViewModel.resetThumbnailUpdateState()
            showThumbnailEditDialog = false
            selectedImageUri = null
        }
    }

    // 설정 화면 애니메이션 추가
    AnimatedRoomSettingsNavigation(
        roomId = roomId,
        roomViewModel = roomViewModel,
        isVisible = showRoomSettings,
        onDismiss = { showRoomSettings = false },
        onRoomEdit = { roomIdToEdit ->
            // 방 정보 수정 화면으로 이동
            navController.navigate("edit_room/$roomIdToEdit")
        },
        onLeaveRoom = {
            // 방 나가기 처리
            scope.launch {
                roomViewModel.leaveRoom(roomId)

                // 에러가 없으면 성공으로 간주하고 뒤로 이동
                if (roomViewModel.error.value.isEmpty()) {
                    showToast("방에서 나갔습니다")
                    navController.popBackStack()
                } else {
                    showToast(roomViewModel.error.value)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* 타이틀 없음 */ },
                navigationIcon = {
                    // 왼쪽에 로고 이미지 배치 - 크기 증가
                    Icon(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "로고",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(72.dp)  // 크기를 64dp로 설정
                    )
                },
                actions = {
                    IconButton(onClick = {
                        showRoomSettings = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "설정",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFCF7F0),  // FCF7F0 색상으로 변경
                    navigationIconContentColor = Color.Black,  // 아이콘 색상을 검정으로 변경
                    actionIconContentColor = Color.Black       // 아이콘 색상을 검정으로 변경
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                // 전체 화면을 스크롤 가능하게 수정
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // 상단 영역 (FCF7F0 색상, 하단에 50dp 둥근 모서리)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
                            .background(Color(0xFFFCF7F0))
                    ) {
                        Column {
                            // 썸네일 이미지 부분
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // 썸네일 이미지 로드
                                    if (roomDetail?.roomThumbnail != null && roomDetail?.roomThumbnail != "default_thumbnail") {
                                        // 서버 URL에서 이미지 로드
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(roomDetail?.roomThumbnail)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "방 썸네일",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        // 기본 이미지 표시
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.room_default_thumbnail),
                                                contentDescription = "기본 썸네일",
                                                contentScale = ContentScale.FillBounds,  // Crop에서 FillBounds로 변경
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }


                                    // 방장일 경우 썸네일 우측 상단에 편집 아이콘 표시
                                    if (roomDetail?.host == true) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)  // 원하는 크기로 설정
                                                    .background(
                                                        color = Color(0xFFD0B699),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { showThumbnailEditDialog = true }  // IconButton 대신 clickable 사용
                                                    .padding(2.dp),  // 내부 패딩 추가
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "썸네일 편집",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)  // 원 크기에 맞게 더 작게 설정
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 방 정보 섹션
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 방 제목과 정보 (좌우 배열)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // 방 제목
                                            Text(
                                                text = roomDetail?.roomTitle ?: "방 제목",
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // 방 설명
                                            Text(
                                                text = roomDetail?.roomContext ?: "방 설명",
                                                fontSize = 16.sp,
                                                color = Color.Gray,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(20.dp))

                                            // 멤버 수
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "멤버 : ",
                                                    fontSize = 16.sp,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "${roomMembers.size}명",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // 쇼츠 업로드 버튼
                                            FilledTonalButton(
                                                onClick = {
                                                    showUploadDialog = true
                                                },
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = Color(0xFFD0B699),  // 색상을 D0B699로 변경
                                                    contentColor = Color.White  // 글자 색상을 흰색으로 변경
                                                ),
                                                shape = RoundedCornerShape(4.dp),
                                                contentPadding = PaddingValues(
                                                    horizontal = 12.dp,
                                                    vertical = 8.dp
                                                ),
                                                modifier = Modifier.width(120.dp)
                                            ) {
                                                Text(
                                                    text = "쇼츠 업로드",
                                                    fontSize = 12.sp
                                                )
                                            }

                                            // 초대코드 버튼
                                            FilledTonalButton(
                                                onClick = {
                                                    if (inviteCode.isNotEmpty()) {
                                                        showInviteCodeDialog = true
                                                    } else {
                                                        scope.launch {
                                                            try {
                                                                isGeneratingCode = true
                                                                roomViewModel.createInviteCode(roomId)
                                                                showInviteCodeDialog = true
                                                            } catch (e: Exception) {
                                                                showToast("초대코드 생성 실패")
                                                            } finally {
                                                                isGeneratingCode = false
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = Color(0xFFD0B699),  // 색상을 D0B699로 변경
                                                    contentColor = Color.White  // 글자 색상을 흰색으로 변경
                                                ),
                                                shape = RoundedCornerShape(4.dp),
                                                contentPadding = PaddingValues(
                                                    horizontal = 12.dp,
                                                    vertical = 8.dp
                                                ),
                                                border = null,
                                                enabled = !isLoading && !isGeneratingCode,
                                                modifier = Modifier.width(120.dp)
                                            ) {
                                                if (isGeneratingCode) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                } else {
                                                    Text(
                                                        text = "초대 코드",
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .padding(bottom = 24.dp, top = 8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    tonalElevation = 2.dp
                                ) {
                                    CompositionLocalProvider(LocalTextSelectionColors provides customSelectionColors) {
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 14.sp,
                                                color = Color.Black
                                            ),
                                            singleLine = true,
                                            cursorBrush = SolidColor(Color(0xFFD0B699)),
                                            decorationBox = { innerTextField ->
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = "검색",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(20.dp)
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 8.dp)
                                                    ) {
                                                        if (searchQuery.isEmpty()) {
                                                            Text(
                                                                "쇼츠 검색",
                                                                color = Color.Gray.copy(alpha = 0.7f),
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                        innerTextField()
                                                    }

                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = { searchQuery = "" },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Clear,
                                                                contentDescription = "지우기",
                                                                tint = Color.Gray,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 비디오 목록 제목
                    Text(
                        text = "쇼츠 목록",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )

                    // 비디오 목록 표시 방식 변경 - filteredVideos를 사용하도록 수정
                    if (filteredVideos.isEmpty()) {
                        // 비디오가 없는 경우 메시지 표시
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "업로드된 쇼츠가 없습니다",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    } else {
                        // 비디오 목록 표시 - 그리드 형태로 배치
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .padding(horizontal = 16.dp)
                        ) {
                            // 항목을 2개씩 묶어서 행으로 표시
                            val videoChunks = filteredVideos.chunked(2)

                            videoChunks.forEach { rowVideos ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowVideos.forEach { video ->
                                        Box(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            RoomVideoItem(
                                                video = video,
                                                onClick = {
                                                    navController.navigate("room_video_detail/${roomId}/${video.id}")
                                                }
                                            )
                                        }
                                    }

                                    // 한 행에 비디오가 1개만 있는 경우 빈 공간 추가
                                    if (rowVideos.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            // 로딩 표시기 추가 (더 보기 버튼 대신)
                            if (isVideosLoading && currentPage > 0 && searchQuery.isBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // 초기 로딩 중인 경우 로딩 표시
                    if (isVideosLoading && currentPage == 0 && searchQuery.isBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // 하단 여백 추가
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // 초대코드 다이얼로그
    if (showInviteCodeDialog && inviteCode.isNotEmpty()) {
        val creationTime by roomViewModel.inviteCodeCreationTime.collectAsState()
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = ((currentTime - creationTime) / 1000).toInt()
        val remainingSeconds = maxOf(0, 600 - elapsedSeconds) // 10분(600초)에서 경과 시간 차감

        InviteCodeDialog(
            inviteCode = inviteCode,
            initialRemainingSeconds = remainingSeconds, // 계산된 남은 시간 전달
            onDismiss = { showInviteCodeDialog = false },
            onCopy = {
                clipboardManager.setText(AnnotatedString(inviteCode))
                showToast("초대코드가 클립보드에 복사되었습니다")
                showInviteCodeDialog = false
            }
        )
    }

    // 업로드 다이얼로그
    if (showUploadDialog) {
        UploadShortDialog(
            roomId = roomId,
            onDismiss = {
                showUploadDialog = false
                resetDialogStates() // 다이얼로그가 닫힐 때 상태 초기화
            },
            onUpload = { video, uploadTitle ->
                // 업로드 처리
                scope.launch {
                    try {
                        val token = tokenManager.accessToken.first()
                        if (token != null) {
                            // 문자열 그대로 사용
                            val videoUrl = video.videoUrl  // Uri.parse() 제거
                            val thumbnailUrl = video.thumbnail  // Uri.parse() 제거

                            val videoIdStr = video.videoId.toString()

                            // 수정된 uploadShort 메서드 호출
                            roomViewModel.uploadShort(
                                roomId = roomId,
                                videoUrl = videoUrl,  // String으로 변경
                                title = uploadTitle,
                                thumbnailUrl = thumbnailUrl,  // String으로 변경
                                videoId = videoIdStr
                            )

                            showUploadDialog = false
                            resetDialogStates() // 업로드 후 상태 초기화
                        }
                    } catch (e: Exception) {
                        showToast("업로드 중 오류가 발생했습니다: ${e.message}")
                    }
                }
            },
            onVideoSelectClick = {
                // 비디오 선택 다이얼로그 표시
                showVideoSelectorDialog = true
            },
            selectedVideo = selectedVideo,
            initialTitle = title,
            onTitleChanged = { newTitle -> title = newTitle }
        )
    }

    // 비디오 선택 전체 화면 다이얼로그
    if (showVideoSelectorDialog) {
        VideoSelectorFullScreenDialog(
            myVideos = myVideos,
            onDismiss = {
                showVideoSelectorDialog = false
            },
            onVideoSelected = { video ->
                selectedVideo = video
                showVideoSelectorDialog = false
            }
        )
    }

    // 썸네일 편집 다이얼로그 (분리된 컴포넌트 사용)
    if (showThumbnailEditDialog) {
        ThumbnailEditDialog(
            roomDetail = roomDetail,
            selectedImageUri = selectedImageUri,
            onSelectImage = {
                imagePickerLauncher.launch("image/*")
            },
            onUpdateThumbnail = { uri ->
                roomViewModel.updateRoomThumbnail(roomId, uri)
            },
            onDismiss = {
                showThumbnailEditDialog = false
                selectedImageUri = null
            },
            scope = scope
        )
    }
}