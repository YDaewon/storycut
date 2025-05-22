package com.ssafy.storycut.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.ssafy.storycut.R
import com.ssafy.storycut.data.api.model.VideoDto
import android.widget.Toast
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.media3.common.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
@Composable
fun SingleVideoPlayer(
    video: VideoDto,
    isCurrentlyVisible: Boolean,
    onPlayerCreated: (ExoPlayer) -> Unit = {},
    userProfileImg: String? = null,
    userName: String? = null,
    modifier: Modifier = Modifier,
    onPlayOriginal: (Long) -> Unit = {},
    isOriginalMode: Boolean = false // 원본 모드 여부 추가
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(isCurrentlyVisible) }
    var showMoreOptions by remember { mutableStateOf(false) } // 더보기 메뉴 표시 상태
    val view = LocalView.current
    val window = (context as? android.app.Activity)?.window
    // 화면 꺼지지 않도록
    DisposableEffect(Unit) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    // 권한 요청 결과 처리
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 권한이 허용되면 다운로드 진행
            performDownload(context, video)
        } else {
            // 권한이 거부되면 메시지 표시
            Toast.makeText(context, "저장소 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 플레이어 상태 관리
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = isCurrentlyVisible
            repeatMode = Player.REPEAT_MODE_ONE  // 비디오 반복 재생

            // 비디오 URL 설정
            val mediaItem = MediaItem.Builder().setUri(video.videoUrl).build()
            Log.d("SingleVideoPlayer","원본 id : ${video.videoId} ,오리지널 ID ${video.originalVideoId}")
            setMediaItem(mediaItem)
            prepare()

            // 플레이어 콜백 호출
            onPlayerCreated(this)
        }
    }

    // 화면이 종료되면 ExoPlayer 해제
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 현재 보이는 상태 변경 감지
    LaunchedEffect(isCurrentlyVisible) {
        if (isCurrentlyVisible) {
            exoPlayer.play()
            isPlaying = true
        } else {
            exoPlayer.pause()
            isPlaying = false
        }
    }

    // 다운로드 기능 구현
    fun downloadVideo() {
        showMoreOptions = false

        // Android 13(API 33) 이상에서는 특정 권한 체크 없이 다운로드 가능
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            performDownload(context, video)
        }
        // Android 10(API 29) 이상에서는 공용 디렉토리 접근 권한 변경됨
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performDownload(context, video)
        }
        // Android 9 이하에서는 저장소 권한 필요
        else {
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 권한이 있는 경우 다운로드 실행
                    performDownload(context, video)
                }
                else -> {
                    // 권한 요청
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    // 원본 영상 보기 기능
    fun viewOriginalVideo() {
        showMoreOptions = false
        // 원본 비디오 ID가 있는 경우에만 처리
        video.originalVideoId?.let { originalId ->
            Log.d("SingleVideoPlayer", "원본 영상 재생 요청: $originalId")
            onPlayOriginal(originalId)
        } ?: run {
            // 원본 영상이 없는 경우 메시지 표시
            Toast.makeText(context, "원본 영상을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .clickable {
                // 화면 클릭 시 재생/일시정지 토글
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                } else {
                    exoPlayer.play()
                    isPlaying = true
                }
                // 옵션 메뉴가 열려있다면 닫기
                if (showMoreOptions) {
                    showMoreOptions = false
                }
            }
    ) {
        // 비디오 플레이어
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false  // 기본 컨트롤러 숨기기
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 재생/정지 아이콘 (정지 상태일 때만 표시)
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "재생",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // 더보기 아이콘 (우측 상단)
        IconButton(
            onClick = {
                showMoreOptions = !showMoreOptions
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "더보기",
                tint = Color.White
            )
        }

        if (showMoreOptions) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp)
                    .width(150.dp),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                // 원본 영상 보기 옵션 (원본이 있는 경우만 표시 + 원본 모드가 아닐 때만 표시)
                if (video.originalVideoId != null && !isOriginalMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewOriginalVideo() }
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.video_origin),
                            contentDescription = "원본 영상 보기",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "원본 영상 보기",
                            color = Color.DarkGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // 다운로드 옵션 (항상 표시)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { downloadVideo() }
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.video_download),
                        contentDescription = "다운로드",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "다운로드",
                        color = Color.DarkGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 비디오 정보 (하단에 표시)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 작성자 정보 (프로필 이미지와 함께 표시)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 프로필 이미지 추가
                    AsyncImage(
                        model = userProfileImg ?: R.drawable.ic_launcher_foreground,
                        contentDescription = "작성자 프로필",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        error = painterResource(id = R.drawable.ic_launcher_foreground)
                    )

                    Spacer(modifier = Modifier.padding(start = 10.dp))

                    // 작성자 이름
                    Text(
                        text = userName ?: video.videoTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 비디오 제목 (원본 모드일 경우 "(원본)" 표시)
                Text(
                    text = if (isOriginalMode) "${video.videoTitle} (원본)" else video.videoTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// 실제 다운로드 기능을 분리하여 구현
private fun performDownload(context: Context, video: VideoDto) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // 파일명에 사용할 수 없는 문자 처리
            val safeFileName = video.videoTitle.replace("[\\\\/:*?\"<>|]".toRegex(), "_")

            // DownloadManager를 사용하여 다운로드 요청 생성
            val request = DownloadManager.Request(Uri.parse(video.videoUrl))
                .setTitle("${safeFileName} 다운로드")
                .setDescription("비디오 다운로드 중...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // Android 10(API 29) 이상에서는 Download 디렉토리에 저장
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "StoryCut/${safeFileName}.mp4"
                )
            } else {
                // Android 9 이하에서는 Movies 디렉토리에 저장
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MOVIES,
                    "StoryCut/${safeFileName}.mp4"
                )
            }

            // 모바일 데이터 사용 허용
            request.setAllowedOverMetered(true)

            // 로밍 상태에서 다운로드 허용 (선택적)
            request.setAllowedOverRoaming(true)

            // DownloadManager를 통해 다운로드 시작
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            // UI 스레드에서 토스트 메시지 표시
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "다운로드가 시작되었습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // 오류 발생 시 UI 스레드에서 토스트 메시지 표시
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "다운로드 오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
