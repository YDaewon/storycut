package com.ssafy.storycut.ui.edit

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.storycut.R
import com.ssafy.storycut.data.api.model.VideoDto
import com.ssafy.storycut.ui.edit.dialog.OptionDialog
import com.ssafy.storycut.ui.common.VideoUploadDialog
import com.ssafy.storycut.ui.common.VideoSelectorFullScreenDialog
import com.ssafy.storycut.ui.edit.dialog.MusicSelectionDialog
import com.ssafy.storycut.ui.mypage.VideoViewModel
import com.ssafy.storycut.ui.navigation.BottomNavigationViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EditScreen(
    viewModel: EditViewModel,
    videoViewModel: VideoViewModel,
    bottomNavViewModel: BottomNavigationViewModel = hiltViewModel(),
    onEditSuccess: (String) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showVideoDialog by remember { mutableStateOf(false) }
    var showOptionDialog by remember { mutableStateOf(false) }
    var showVideoSelectorDialog by remember { mutableStateOf(false) }

    // 선택된 VideoDto 저장을 위한 상태 추가
    var selectedVideoDto by remember { mutableStateOf<VideoDto?>(null) }

    // VideoViewModel에서 내 비디오 목록 가져오기
    val myVideos by videoViewModel.myVideos.collectAsState()
    val isVideosLoading by videoViewModel.isLoading.collectAsState()

    // 배경 음악 선택 다이얼로그 상태 변수 추가
    var showBackgroundMusicDialog by remember { mutableStateOf(false) }

    // 하단 네비게이션 표시 여부
    val isBottomNavVisible by bottomNavViewModel.isBottomNavVisible.observeAsState(true)

    // 오류 메시지 표시를 위한 효과
    LaunchedEffect(viewModel.error) {
        viewModel.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // 이벤트 수집
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditViewModel.EditEvent.Success -> {
                    onEditSuccess(event.videoId)
                    // 상태 초기화
                    viewModel.resetState()
                }
                is EditViewModel.EditEvent.Processing -> {
                    // 토스트 메시지 표시
                    Toast.makeText(
                        context,
                        "영상 처리 중입니다. 완료되면 알림이 전송됩니다.",
                        Toast.LENGTH_LONG
                    ).show()

                    // 상태 초기화
                    viewModel.resetState()
                }
                is EditViewModel.EditEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 로딩 상태일 때 로딩 인디케이터 표시 효과 추가
    LaunchedEffect(isVideosLoading) {
        if (isVideosLoading) {
            Toast.makeText(context, "비디오 목록을 불러오는 중...", Toast.LENGTH_SHORT).show()
        }
    }

    // 갤러리에서 비디오 선택을 위한 런처
    val galleryVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setSelectedVideo(it)
            selectedVideoDto = null // 갤러리에서 선택한 경우 선택된 VideoDto 초기화
        }
    }

    // 갤러리에서 이미지 선택을 위한 런처
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addMosaicImage(it) }
    }

    // 비디오 선택 함수들
    fun openGalleryVideos() {
        galleryVideoLauncher.launch("video/*")
        showVideoDialog = false
    }

    fun openMyShorts() {
        videoViewModel.fetchMyVideos()  // 다이얼로그가 표시될 때 데이터 로드
        showVideoSelectorDialog = true
        showVideoDialog = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(start = 6.dp, end = 6.dp, top =16.dp, bottom = 0.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 상단 타이틀
            Text(
                text = "영상 편집",
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )

            // 동영상 업로드 레이블
            Text(
                text = "동영상 업로드",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
            )

            // 동영상 업로드 영역 - 선택된 비디오 썸네일 표시 또는 + 아이콘
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = Color.Gray
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                    .clickable { showVideoDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (selectedVideoDto != null) {
                    // VideoDto에서 썸네일 URL이 있는 경우 해당 썸네일 표시
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(selectedVideoDto?.thumbnail)
                            .crossfade(true)
                            .build(),
                        contentDescription = "선택된 비디오",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (viewModel.videoSelected && viewModel.videoThumbnail != null) {
                    // viewModel에 썸네일이 있는 경우 (갤러리에서 선택한 경우)
                    Image(
                        bitmap = viewModel.videoThumbnail!!.asImageBitmap(),
                        contentDescription = "선택된 비디오",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (viewModel.videoSelected) {
                    // 비디오는 선택됐지만 썸네일이 아직 로드되지 않은 경우
                    CircularProgressIndicator()
                } else {
                    // 비디오가 선택되지 않은 경우 + 아이콘 표시
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "동영상 추가",
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF765a41)
                        )
                        Spacer(modifier = Modifier.height(8.dp))  // 간격 증가

                        Text(
                            text = "영상 선택",
                            style = MaterialTheme.typography.titleMedium,  // 크기 증가
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF765a41)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "영상 선택을 위해 클릭하세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD0B699).copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 구분선
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                color = Color.LightGray
            )

            // 모자이크 옵션 (조건부 표시)
            if (viewModel.hasMosaic) {
                OptionalSection(
                    title = "모자이크",
                    onRemove = { viewModel.toggleMosaic(false) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("제외할 인물을 올려 주세요", fontSize = 12.sp, color = Color.Gray)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // + 아이콘 (사진 추가 버튼) - 점선 테두리로 변경
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .drawWithContent {
                                        // 점선 테두리 그리기
                                        drawContent()
                                        val stroke = Stroke(
                                            width = 1f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                        drawRoundRect(
                                            color = Color.LightGray,
                                            style = stroke,
                                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                    }
                                    .clickable {
                                        if (viewModel.mosaicImages.size < 2) {
                                            photoPickerLauncher.launch("image/*")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // 회색 + 아이콘
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "인물 사진 추가",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            // 선택된 인물 사진들
                            viewModel.mosaicImages.forEachIndexed { index, uri ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                ) {
                                    // 사진 - 수정자 추가
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "인물 ${index + 1}",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    // 삭제 버튼
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-4).dp, y = 4.dp)
                                            .background(Color.White, CircleShape)
                                            .clickable {
                                                viewModel.removeMosaicImage(index)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.remove),
                                            contentDescription = "사진 삭제",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 한국어 자막 옵션 (조건부 표시)
            if (viewModel.applySubtitle) {
                OptionalSection(
                    title = "한국어 자막",
                    onRemove = { viewModel.toggleKoreanSubtitle(false) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // 자막 설정 영역 내용
                    Text(
                        "한국어 자막이 자동으로 생성됩니다.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                HorizontalDivider(
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 배경 음악 옵션 (조건부 표시)
            if (viewModel.hasBackgroundMusic) {
                OptionalSection(
                    title = "배경 음악",
                    onRemove = { viewModel.toggleBackgroundMusic(false) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // 자동 음악 생성 모드일 때와 프롬프트 모드일 때 다른 UI 표시
                    if (viewModel.autoMusic) {
                        // 자동 음악 생성 모드
                        Text(
                            "영상 내용에 맞는 배경 음악이 자동으로 생성됩니다.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    } else {
                        // 프롬프트 음악 생성 모드
                        OutlinedTextField(
                            value = viewModel.musicPromptText,
                            onValueChange = { viewModel.updateMusicPromptText(it) },
                            label = { Text("음악 분위기") },
                            shape = RoundedCornerShape(8.dp),
                            placeholder = {
                                if (viewModel.musicPromptText.isEmpty()) {
                                    Text(
                                        text = "음악 분위기를 설명해주세요 (예: 신나는, 슬픈, 로맨틱한)",
                                        color = Color.LightGray,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .padding(horizontal = 6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedLabelColor = Color(0xFFD0B699),
                                unfocusedLabelColor = Color.LightGray,
                                focusedBorderColor = Color(0xFFD0B699),
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0xFFFCF7F0),
                                unfocusedContainerColor = Color(0xFFFCF7F0),
                                cursorColor = Color(0xFFD0B699),
                                selectionColors = TextSelectionColors(
                                    handleColor = Color(0xFFD0B699),
                                    backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
                                )
                            )
                        )
                    }
                }
                HorizontalDivider(
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))  // 간격 추가

            // 옵션추가 버튼 - 모든 옵션이 추가된 경우 숨김
            val allOptionsAdded = viewModel.hasMosaic && viewModel.applySubtitle && viewModel.hasBackgroundMusic
            if (!allOptionsAdded) {  // 모든 옵션이 추가되지 않은 경우에만 버튼 표시
                Button(
                    onClick = { showOptionDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0B699)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "옵션추가",
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 영상 제목 입력 필드 (두 번째 파일에서 추가됨)
            OutlinedTextField(
                value = viewModel.videoTitle,
                onValueChange = { viewModel.updateVideoTitle(it) },
                label = { Text("영상 제목") },
                placeholder = { Text("영상 제목을 입력하세요", color = Color.LightGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                maxLines = 1,
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
                )
            )

            // 프롬프트 입력 영역
            OutlinedTextField(
                value = viewModel.promptText,
                onValueChange = { viewModel.updatePromptText(it) },
                label = { Text("프롬프트") },
                placeholder = { Text("프롬프트를 입력하세요", color = Color.LightGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                maxLines = 1,
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
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    // 선택된 비디오가 VideoDto에서 온 경우 처리
                    if (selectedVideoDto != null && !viewModel.videoSelected) {
                        try {
                            val videoUrl = selectedVideoDto?.videoUrl
                            val thumbnailUrl = selectedVideoDto?.thumbnail

                            if (videoUrl != null) {
                                if (videoUrl.startsWith("content://") || videoUrl.startsWith("file://")) {
                                    // 로컬 URI 처리
                                    val videoUri = Uri.parse(videoUrl)
                                    viewModel.setSelectedVideo(videoUri)
                                } else if (videoUrl.startsWith("http")) {
                                    // HTTP URL 처리 - 썸네일 URL도 함께 전달
                                    viewModel.setSelectedVideoUrl(videoUrl, thumbnailUrl)
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "비디오 로드 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    viewModel.processEditing()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0B699),
                    disabledContainerColor = Color(0xFFD0B699).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = (!viewModel.isLoading && (viewModel.videoSelected || selectedVideoDto != null)) // 선택된 비디오가 있거나 VideoDto가 있는 경우 활성화
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("편집하기", color = Color.White)
                }
            }
        }
    }

    // 비디오 업로드 다이얼로그 표시
    if (showVideoDialog) {
        VideoUploadDialog(
            onDismiss = { showVideoDialog = false },
            onGalleryClick = { openGalleryVideos() },
            onShortsClick = { openMyShorts() }
        )
    }

    // 옵션 다이얼로그 표시
    if (showOptionDialog) {
        OptionDialog(
            onDismiss = { showOptionDialog = false },
            onMosaicClick = { viewModel.toggleMosaic(true) },
            onKoreanSubtitleClick = { viewModel.toggleKoreanSubtitle(true) },
            onBackgroundMusicClick = {
                showBackgroundMusicDialog = true  // 옵션 다이얼로그에서 배경 음악 옵션 선택 시 새 다이얼로그 표시
                showOptionDialog = false
            },
            hasMosaic = viewModel.hasMosaic,
            hasKoreanSubtitle = viewModel.applySubtitle,
            hasBackgroundMusic = viewModel.hasBackgroundMusic
        )
    }

    // 비디오 선택기 다이얼로그 추가
    if (showVideoSelectorDialog) {
        VideoSelectorFullScreenDialog(
            myVideos = myVideos,
            onDismiss = {
                showVideoSelectorDialog = false
            },
            onVideoSelected = { video ->
                try {
                    // VideoDto 객체 자체를 저장
                    selectedVideoDto = video

                    // 비디오 제목도 설정
                    viewModel.updateVideoTitle(video.videoTitle)

                    // 웹 URL 처리를 위해 setSelectedVideoUrl 호출 - 썸네일 URL 함께 전달
                    viewModel.setSelectedVideoUrl(video.videoUrl, video.thumbnail)

                    // 토스트 메시지로 선택 성공 알림
                    Toast.makeText(context, "비디오 '${video.videoTitle}' 선택됨", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "비디오 로드 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                showVideoSelectorDialog = false
            }
        )
    }

    // 배경 음악 선택 다이얼로그 추가
    if (showBackgroundMusicDialog) {
        MusicSelectionDialog(
            onDismiss = { showBackgroundMusicDialog = false },
            onAutoMusicClick = {
                viewModel.setBackgroundMusic(true, true)  // 자동 음악 생성 모드로 설정
            },
            onPromptMusicClick = {
                viewModel.setBackgroundMusic(true, false)  // 프롬프트 음악 생성 모드로 설정
            }
        )
    }

    // 로딩 오버레이
    if (viewModel.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    "영상 처리 중...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OptionalSection(
    title: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
    ) {
        // 제목 및 제거 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            // 제거 버튼
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.remove),
                    contentDescription = "제거",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 섹션 내용
        content()
    }
}