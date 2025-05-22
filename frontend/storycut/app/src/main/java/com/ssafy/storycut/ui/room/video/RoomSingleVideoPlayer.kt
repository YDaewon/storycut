package com.ssafy.storycut.ui.room.video

import android.util.Log
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
import com.ssafy.storycut.data.api.model.credential.UserInfo
import com.ssafy.storycut.data.api.model.chat.ChatDto

private const val TAG = "RoomSingleVideoPlayer"

@UnstableApi
@Composable
fun RoomSingleVideoPlayer(
    video: ChatDto,
    uploaderInfo: UserInfo? = null,
    isCurrentlyVisible: Boolean,
    onPlayerCreated: (ExoPlayer) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(isCurrentlyVisible) }

    // 로깅
    LaunchedEffect(video.id, uploaderInfo, isCurrentlyVisible) {
        Log.d(TAG, "RoomSingleVideoPlayer: videoId=${video.id}, isVisible=$isCurrentlyVisible")
        Log.d(TAG, "업로더 정보: ${uploaderInfo?.name ?: "없음"}")
    }

    // 플레이어 생성
    val exoPlayer = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = isCurrentlyVisible
            repeatMode = Player.REPEAT_MODE_ONE

            try {
                val mediaItem = MediaItem.fromUri(video.mediaUrl)
                setMediaItem(mediaItem)
                prepare()

                onPlayerCreated(this)
                Log.d(TAG, "ExoPlayer 생성 성공: videoId=${video.id}, url=${video.mediaUrl}")
            } catch (e: Exception) {
                Log.e(TAG, "ExoPlayer 생성 실패: ${e.message}")
            }
        }
    }

    // 플레이어 해제
    DisposableEffect(video.id) {
        onDispose {
            Log.d(TAG, "ExoPlayer 해제: videoId=${video.id}")
            exoPlayer.release()
        }
    }

    // 재생 상태 관리
    LaunchedEffect(isCurrentlyVisible, video.id) {
        if (isCurrentlyVisible) {
            exoPlayer.play()
            isPlaying = true
            Log.d(TAG, "비디오 재생 시작: ${video.id}")
        } else {
            exoPlayer.pause()
            isPlaying = false
            Log.d(TAG, "비디오 일시정지: ${video.id}")
        }
    }

    Box(
        modifier = modifier
            .clickable {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                } else {
                    exoPlayer.play()
                    isPlaying = true
                }
            }
    ) {
        // 비디오 플레이어
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.player = exoPlayer
            }
        )

        // 일시정지 상태일 때 재생 아이콘 표시
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

        // 비디오 정보 (하단)
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
                // 작성자 정보
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 프로필 이미지
                    if (uploaderInfo != null) {
                        AsyncImage(
                            model = uploaderInfo.profileImg,
                            contentDescription = "작성자 프로필",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            error = painterResource(id = R.drawable.ic_launcher_foreground)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                    }

                    Spacer(modifier = Modifier.padding(start = 10.dp))

                    // 작성자 이름
                    Text(
                        text = uploaderInfo?.nickname ?: uploaderInfo?.name ?: "사용자",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 비디오 제목
                Text(
                    text = video.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}