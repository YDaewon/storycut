package com.ssafy.storycut.ui.mypage

import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.ssafy.storycut.ui.auth.AuthViewModel

@UnstableApi
@Composable
fun VideoDetailScreen(
    videoId: String,
    navController: NavController,
    videoViewModel: VideoViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    isOriginalMode: Boolean = false // 원본 모드 파라미터 추가
) {
    val context = LocalContext.current
    val videoList by videoViewModel.myVideos.collectAsState()
    val currentVideo by videoViewModel.videoDetail.collectAsState()
    val userInfo by authViewModel.userState.collectAsState()
    val isLoading by videoViewModel.isLoading.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    var isExiting by remember { mutableStateOf(false) }

    val window = (context as? android.app.Activity)?.window

    // 플레이어 맵을 관리하기 위한 변수
    val players = remember { mutableMapOf<Int, ExoPlayer>() }

    // 앱 생명주기 상태를 저장
    var appInBackground by remember { mutableStateOf(false) }

    // 현재 선택된 비디오의 위치 찾기 (videoList가 로드된 후에만 시도, 원본 모드가 아닐 때만)
    val initialPage = remember(videoId, videoList, isOriginalMode) {
        if (!isOriginalMode && videoList.isNotEmpty()) {
            val index = videoList.indexOfFirst { it.videoId.toString() == videoId }
            if (index >= 0) {
                Log.d("VideoDetailScreen", "초기 비디오 인덱스 찾음: $index, ID=$videoId")
                index
            } else {
                Log.d("VideoDetailScreen", "비디오를 찾지 못함: ID=$videoId, 목록 크기=${videoList.size}")
                0
            }
        } else {
            Log.d("VideoDetailScreen", "원본 모드이거나 비디오 목록이 비어 있음, 기본 인덱스 0 사용")
            0
        }
    }

    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // VerticalPager 상태 설정 (원본 모드가 아닐 때만 사용)
    val pagerState = rememberPagerState(initialPage = initialPage) { videoList.size }

    // 페이저 상태가 변경될 때 현재 페이지를 업데이트하도록 LaunchedEffect 추가 (원본 모드가 아닐 때만)
    LaunchedEffect(initialPage, videoList.size, isOriginalMode) {
        if (!isOriginalMode && videoList.isNotEmpty() && pagerState.currentPage != initialPage) {
            pagerState.scrollToPage(initialPage)
        }
    }

    // 페이지 변경 시 현재 비디오 ID 로깅 (원본 모드가 아닐 때만)
    LaunchedEffect(pagerState.currentPage, videoList.size, isOriginalMode) {
        if (!isOriginalMode && videoList.isNotEmpty() && pagerState.currentPage < videoList.size) {
            val currentVideoId = videoList[pagerState.currentPage].videoId
            Log.d("VideoDetailScreen", "페이지 변경됨: ${pagerState.currentPage}, 비디오 ID: $currentVideoId")
        }
    }

    // 원본 영상 재생 처리 함수
    val handlePlayOriginal = { originalVideoId: Long ->
        Log.d("VideoDetailScreen", "원본 영상 재생 요청됨: $originalVideoId")

        try {
            // 원본 영상 상세 페이지로 이동
            navController.navigate("video_detail/${originalVideoId}/original")
        } catch (e: Exception) {
            Log.e("VideoDetailScreen", "원본 영상 재생 요청 중 오류", e)
            Toast.makeText(context, "원본 영상 이동 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 뒤로가기 처리 함수 정의
    val handleBackPress = {
        // 종료 상태로 변경하고 모든 플레이어 즉시 해제
        isExiting = true
        try {
            players.values.forEach { player ->
                try {
                    player.stop()
                    player.release()
                } catch (e: Exception) {
                    Log.e("VideoDetailScreen", "플레이어 해제 오류", e)
                }
            }
            players.clear()
        } catch (e: Exception) {
            Log.e("VideoDetailScreen", "플레이어 정리 중 오류", e)
        }

        // 화면 전환
        navController.popBackStack()
    }

    // 생명주기 감지를 위한 효과
    DisposableEffect(true) {
        val activity = context as? androidx.activity.ComponentActivity
        val callback = object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        }

        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                // 백그라운드로 이동할 때
                appInBackground = true
                try {
                    players.values.forEach { player ->
                        try {
                            if (player.isPlaying) {
                                player.pause()
                            }
                        } catch (e: Exception) {
                            Log.e("VideoDetailScreen", "onPause 처리 중 오류", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VideoDetailScreen", "onPause 플레이어 처리 중 오류", e)
                }
            }

            override fun onResume(owner: LifecycleOwner) {
                // 포그라운드로 돌아올 때
                if (appInBackground) {
                    appInBackground = false
                    try {
                        val currentPage = pagerState.currentPage
                        players[currentPage]?.let { player ->
                            try {
                                player.play()
                            } catch (e: Exception) {
                                Log.e("VideoDetailScreen", "onResume 처리 중 오류", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("VideoDetailScreen", "onResume 플레이어 처리 중 오류", e)
                    }
                }
            }
        }

        activity?.lifecycle?.addObserver(lifecycleObserver)
        activity?.onBackPressedDispatcher?.addCallback(callback)

        onDispose {
            activity?.lifecycle?.removeObserver(lifecycleObserver)
            callback.remove()
        }
    }

    // 안드로이드 시스템 뒤로가기 버튼 처리
    BackHandler(enabled = !isExiting) {
        handleBackPress()
    }

    // 비디오 상세 정보 로드
    LaunchedEffect(videoId, isOriginalMode) {
        try {
            // 사용자 정보 새로고침
            authViewModel.refreshUserInfoFromRoom()

            // 현재 비디오 상세 정보 로드 (API로 직접 요청)
            Log.d("VideoDetailScreen", "비디오 상세 정보 로드 요청: ID=$videoId, 원본 모드=$isOriginalMode")
            videoViewModel.fetchVideoDetail(videoId)

            // 원본 모드가 아닐 때만 비디오 목록 로드
            if (!isOriginalMode && videoList.isEmpty()) {
                videoViewModel.fetchMyVideos()
            }
        } catch (e: Exception) {
            error = "비디오를 로드할 수 없습니다: ${e.message}"
            Log.e("VideoDetailScreen", "비디오 로드 오류", e)
        }
    }

    // 화면 종료 시 모든 플레이어 해제
    DisposableEffect(Unit) {
        onDispose {
            try {
                players.values.forEach { player ->
                    try {
                        player.release()
                    } catch (e: Exception) {
                        Log.e("VideoDetailScreen", "화면 종료 시 플레이어 해제 오류", e)
                    }
                }
                players.clear()
            } catch (e: Exception) {
                Log.e("VideoDetailScreen", "화면 종료 시 플레이어 정리 중 오류", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading && currentVideo == null -> {
                // 로딩 중이고 현재 비디오가 없을 때 로딩 표시
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "오류가 발생했습니다",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { handleBackPress() }) {
                        Text("돌아가기")
                    }
                }
            }
            isOriginalMode && currentVideo == null && !isLoading -> {
                // 원본 모드이지만 비디오가 없고 로딩 중이 아닐 때
                Text(
                    text = "원본 영상을 불러올 수 없습니다",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            !isOriginalMode && videoList.isEmpty() && currentVideo == null && !isLoading -> {
                // 일반 모드이지만 비디오 목록과 현재 비디오가 모두 없고 로딩 중이 아닐 때
                Text(
                    text = "비디오가 없습니다",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            else -> {
                if (!isExiting) {
                    if (isOriginalMode) {
                        // 원본 모드일 때 - 단일 비디오 표시
                        if (currentVideo != null) {
                            SingleVideoPlayer(
                                video = currentVideo!!,
                                isCurrentlyVisible = !isExiting && !appInBackground,
                                onPlayerCreated = { player ->
                                    players[0] = player
                                },
                                userProfileImg = userInfo?.profileImg,
                                userName = userInfo?.name,
                                modifier = Modifier.fillMaxSize(),
                                isOriginalMode = true
                            )
                        }
                    } else {
                        // 일반 모드일 때 (비디오 목록이 있으면 페이저 사용)
                        if (videoList.isNotEmpty()) {
                            VerticalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .systemBarsPadding() // 시스템 바 영역 제외
                            ) { page ->
                                val video = videoList.getOrNull(page)
                                if (video != null) {
                                    SingleVideoPlayer(
                                        video = video,
                                        isCurrentlyVisible = page == pagerState.currentPage && !isExiting && !appInBackground,
                                        onPlayerCreated = { player ->
                                            players[page] = player
                                        },
                                        userProfileImg = userInfo?.profileImg,
                                        userName = userInfo?.name,
                                        modifier = Modifier.fillMaxSize(),
                                        onPlayOriginal = { originalId ->
                                            handlePlayOriginal(originalId)
                                        },
                                        isOriginalMode = false
                                    )
                                }
                            }
                        } else if (currentVideo != null) {
                            // 비디오 목록이 없지만 현재 비디오가 있는 경우 단일 비디오 표시
                            SingleVideoPlayer(
                                video = currentVideo!!,
                                isCurrentlyVisible = !isExiting && !appInBackground,
                                onPlayerCreated = { player ->
                                    players[0] = player
                                },
                                userProfileImg = userInfo?.profileImg,
                                userName = userInfo?.name,
                                modifier = Modifier.fillMaxSize(),
                                onPlayOriginal = { originalId ->
                                    handlePlayOriginal(originalId)
                                },
                                isOriginalMode = false
                            )
                        }
                    }
                }

                // 뒤로가기 버튼
                IconButton(
                    onClick = { handleBackPress() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White
                    )
                }

                // 원본 영상일 경우 "원본 영상" 라벨 표시
                if (isOriginalMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "원본 영상",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}