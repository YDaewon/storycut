package com.ssafy.storycut.ui.shorts

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.storycut.data.api.model.VideoDto
import com.ssafy.storycut.ui.common.VideoSelectorFullScreenDialog
import com.ssafy.storycut.ui.common.VideoUploadDialog
import com.ssafy.storycut.ui.mypage.VideoViewModel
import com.ssafy.storycut.ui.shorts.components.AuthScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import com.ssafy.storycut.ui.shorts.components.TagChip

@Composable
fun ShortsScreen(
    viewModel: ShortsViewModel = hiltViewModel(),
    videoViewModel: VideoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // UI 상태 관찰
    val uiState by viewModel.uiState.observeAsState(ShortsUiState.Loading)
    val youtubeAuthResponse by viewModel.youtubeAuthUrl.observeAsState()
    val error by viewModel.error.observeAsState()

    var isActionLoading by remember { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoThumbnail by remember { mutableStateOf<Bitmap?>(null) }

    // 내 비디오 관련 상태 추가
    val myVideos by videoViewModel.myVideos.collectAsState()
    val isVideosLoading by videoViewModel.isLoading.collectAsState()
    var showVideoDialog by remember { mutableStateOf(false) }
    var showVideoSelectorDialog by remember { mutableStateOf(false) }
    var selectedVideoDto by remember { mutableStateOf<VideoDto?>(null) }

    // 갤러리에서 선택한 비디오의 썸네일 로드 함수
    fun loadVideoThumbnail(uri: Uri) {
        scope.launch {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                selectedVideoThumbnail = retriever.getFrameAtTime(0)
                retriever.release()
            } catch (e: Exception) {
                selectedVideoThumbnail = null
            }
        }
    }

    // 갤러리에서 영상 선택
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
        selectedVideoDto = null  // 갤러리에서 선택 시 VideoDto 초기화
        uri?.let { loadVideoThumbnail(it) }  // 썸네일 로드
    }

    // 비디오 선택 함수들
    fun openGalleryVideos() {
        videoPickerLauncher.launch("video/*")
        showVideoDialog = false
    }

    fun openMyShorts() {
        videoViewModel.fetchMyVideos()  // 다이얼로그가 표시될 때 데이터 로드
        showVideoSelectorDialog = true
        showVideoDialog = false
    }

    // 에러 발생 시 로딩 상태 해제
    LaunchedEffect(error) {
        error?.let {
            isActionLoading = false
            // 몇 초 후 에러 메시지 초기화
            delay(3000)
            viewModel.clearError()
        }
    }

    // 인증 URL 응답 처리
    LaunchedEffect(youtubeAuthResponse) {
        youtubeAuthResponse?.let { response ->
            isActionLoading = false
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(response.authUrl))
            context.startActivity(intent)
        }
    }

    // 메인 화면 구성
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // UI 상태에 따른 화면 표시
        when (uiState) {
            ShortsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            ShortsUiState.Unauthenticated -> {
                AuthScreen(
                    onRequestAuth = {
                        isActionLoading = true
                        viewModel.getYouTubeAuthUrl()
                    },
                    isLoading = isActionLoading
                )
            }

            ShortsUiState.Authenticated -> {
                // 선택된 비디오의 URI 확인
                val effectiveUri = if (selectedVideoDto != null) {
                    try {
                        Uri.parse(selectedVideoDto?.videoUrl)
                    } catch (e: Exception) {
                        selectedVideoUri
                    }
                } else {
                    selectedVideoUri
                }

                // 커스텀 UploadScreen 구현으로 썸네일 직접 처리
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp)  // 패딩 증가
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)  // 요소 간 일정한 간격 적용
                ) {
                    // 상단 타이틀
                    Text(
                        text = "유튜브 쇼츠 업로드",
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),  // 간격을 24.dp로 늘림
                        textAlign = TextAlign.Center
                    )

                    // 비디오 선택 영역 (썸네일 포함) - 크기 확대
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)  // 높이 확대 (150dp → 220dp)
                            .clickable(enabled = !isActionLoading) { showVideoDialog = true },
                        shape = RoundedCornerShape(12.dp),  // 더 둥근 모서리
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)  // 그림자 추가
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (effectiveUri != null) {
                                if (selectedVideoDto?.thumbnail != null) {
                                    // 내 쇼츠에서 선택한 경우 (썸네일 있음)
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(selectedVideoDto?.thumbnail)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "선택된 비디오 썸네일",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 어두운 오버레이 추가하여 텍스트 가독성 향상
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    )

                                    // 중앙에 '선택된 비디오' 텍스트 표시
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),  // 크기 증가
                                                tint = Color.White
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "선택된 비디오",
                                                style = MaterialTheme.typography.titleLarge,  // 크기 증가
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )

                                            // 선택된 비디오의 제목 표시 (있는 경우)
                                            selectedVideoDto?.videoTitle?.let { title ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                } else if (selectedVideoThumbnail != null) {
                                    // 갤러리에서 선택한 경우 (썸네일 로드됨)
                                    Image(
                                        bitmap = selectedVideoThumbnail!!.asImageBitmap(),
                                        contentDescription = "선택된 비디오 썸네일",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 어두운 오버레이 추가하여 텍스트 가독성 향상
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    )

                                    // 중앙에 '선택된 비디오' 텍스트 표시
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),  // 크기 증가
                                                tint = Color.White
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "선택된 비디오",
                                                style = MaterialTheme.typography.titleLarge,  // 크기 증가
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                } else {
                                    // 갤러리에서 선택한 경우 (썸네일 로드 안됨)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFFCF7F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),  // 크기 증가
                                                tint = Color(0xFFD0B699)
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))  // 간격 증가

                                            Text(
                                                text = "선택된 비디오",
                                                style = MaterialTheme.typography.titleMedium,  // 크기 증가
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD0B699)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 비디오가 선택되지 않은 경우
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFFCF7F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp),  // 크기 증가
                                            tint = Color(0xFFD0B699)
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))  // 간격 증가

                                        Text(
                                            text = "비디오 선택",
                                            style = MaterialTheme.typography.titleMedium,  // 크기 증가
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD0B699)
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
                        }
                    }

                    // 제목, 설명, 태그, 업로드 버튼 등 - verticalArrangement로 인해 추가 Spacer 불필요
                    var title by remember { mutableStateOf("") }
                    var description by remember { mutableStateOf("") }
                    var tagInput by remember { mutableStateOf("") }
                    var tags by remember { mutableStateOf<List<String>>(emptyList()) }

                    // 제목 입력 필드
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("쇼츠 제목", color = Color.LightGray) },
                        placeholder = { Text("제목을 입력하세요", color = Color.LightGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(Color(0xFFFCF7F0)),
                        enabled = !isActionLoading,
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color(0xFFFCF7F0),
                            focusedContainerColor = Color(0xFFFCF7F0),
                            cursorColor = Color.LightGray,
                            focusedLabelColor = Color.LightGray,
                            unfocusedLabelColor = Color.LightGray,
                            selectionColors = TextSelectionColors(
                                handleColor = Color(0xFFD0B699),
                                backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
                            )
                        )
                    )

                    // 설명 입력 필드
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("쇼츠 설명", color = Color.LightGray) },
                        placeholder = { Text("설명을 입력하세요", color = Color.LightGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(Color(0xFFFCF7F0)),
                        enabled = !isActionLoading,
                        maxLines = 5,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color(0xFFFCF7F0),
                            focusedContainerColor = Color(0xFFFCF7F0),
                            cursorColor = Color.LightGray,
                            focusedLabelColor = Color.LightGray,
                            unfocusedLabelColor = Color.LightGray,
                            selectionColors = TextSelectionColors(
                                handleColor = Color(0xFFD0B699),
                                backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
                            )
                        )
                    )

                    // 태그 입력 영역
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 태그 입력 필드와 추가 버튼
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = tagInput,
                                onValueChange = { tagInput = it },
                                label = { Text("태그", color = Color.LightGray) },
                                placeholder = { Text("태그를 입력하세요", color = Color.LightGray) },
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(Color(0xFFFCF7F0)),
                                enabled = !isActionLoading,
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedContainerColor = Color(0xFFFCF7F0),
                                    focusedContainerColor = Color(0xFFFCF7F0),
                                    cursorColor = Color.LightGray,
                                    focusedLabelColor = Color.LightGray,
                                    unfocusedLabelColor = Color.LightGray,
                                    selectionColors = TextSelectionColors(
                                        handleColor = Color(0xFFD0B699),
                                        backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
                                    )
                                )
                            )

                            Button(
                                onClick = {
                                    if (tagInput.isNotBlank()) {
                                        tags = tags + tagInput.trim()
                                        tagInput = ""
                                    }
                                },
                                enabled = !isActionLoading && tagInput.isNotBlank(),
                                modifier = Modifier
                                    .height(56.dp)  // 높이 증가 (TextField와 맞춤)
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(8.dp),  // 모서리 둥글게
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD0B699),
                                    disabledContainerColor = Color(0xFFD0B699).copy(alpha = 0.5f)
                                )
                            ) {
                                Text("추가")
                            }
                        }

                        // 입력된 태그 표시
                        if (tags.isNotEmpty()) {
                            Text(
                                text = "입력된 태그",
                                style = MaterialTheme.typography.bodyLarge,  // 크기 증가
                                fontWeight = FontWeight.Bold
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tags.forEachIndexed { index, tag ->
                                    TagChip(
                                        tag = tag,
                                        onRemove = {
                                            tags = tags.filterIndexed { i, _ -> i != index }
                                        },
                                        enabled = !isActionLoading
                                    )
                                }
                            }
                        }
                    }

                    // 업로드 버튼
                    Button(
                        onClick = {
                            effectiveUri?.let { uri ->
                                viewModel.uploadToYouTube(
                                    videoUri = uri,
                                    title = title.ifBlank { "스토리컷에서 올린 영상" },
                                    description = description.ifBlank { "앱에서 자동 업로드된 영상입니다" },
                                    tags = tags
                                )
                                isActionLoading = true
                            }
                        },
                        enabled = !isActionLoading && effectiveUri != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),  // 높이 증가 (48dp → 60dp)
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFE4343)
                        ),
                        shape = RoundedCornerShape(12.dp)  // 모서리 더 둥글게
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)  // 크기 증가 (18dp → 24dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))  // 간격 증가 (4dp → 8dp)
                        Text(
                            text = "유튜브 쇼츠로 업로드하기",
                            style = MaterialTheme.typography.titleMedium,  // 글자 크기 증가
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 에러 메시지 표시 (모든 상태에서 공통)
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            error?.let {
                Snackbar {
                    Text(it)
                }
            }
        }

        // 액션 진행 중일 때 로딩 인디케이터 (인증/업로드 등)
        if (isActionLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
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
                        selectedVideoThumbnail = null  // 썸네일 초기화

                        // 비디오 URL을 URI로 변환
                        val videoUrl = video.videoUrl
                        if (videoUrl.startsWith("content://") || videoUrl.startsWith("file://") || videoUrl.startsWith("http")) {
                            selectedVideoUri = Uri.parse(videoUrl)
                        } else {
                            Toast.makeText(context, "비디오 URL 형식이 올바르지 않습니다", Toast.LENGTH_SHORT).show()
                        }

                        // 토스트 메시지로 선택 성공 알림
                        Toast.makeText(context, "비디오 '${video.videoTitle}' 선택됨", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "비디오 로드 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    showVideoSelectorDialog = false
                }
            )
        }
    }
}


@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rows = mutableListOf<MutableList<Placeable>>()
        val rowConstraints = constraints.copy(minWidth = 0)

        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(rowConstraints)

            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }

            currentRow.add(placeable)
            currentRowWidth += placeable.width
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val height = rows.sumOf { row -> row.maxOfOrNull { it.height } ?: 0 }

        layout(constraints.maxWidth, height) {
            var y = 0

            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0

                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width
                }

                y += rowHeight
            }
        }
    }
}