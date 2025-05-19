package com.ssafy.storycut.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ssafy.storycut.data.api.model.VideoDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSelectorFullScreenDialog(
    myVideos: List<VideoDto>,
    onDismiss: () -> Unit,
    onVideoSelected: (VideoDto) -> Unit
) {
    var selectedVideo by remember { mutableStateOf<VideoDto?>(null) }

    DisposableEffect(Unit) {
        selectedVideo = null
        onDispose { }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("영상 선택") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "뒤로 가기"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFD0B699), titleContentColor = Color.White)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF5F5F5))
                    ) {
                        if (myVideos.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "업로드된 영상이 없습니다",
                                    color = Color.Gray
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 72.dp) // 하단 버튼 공간 확보
                            ) {
                                items(myVideos) { video ->
                                    VideoSelectionItem(
                                        video = video,
                                        isSelected = selectedVideo?.videoId == video.videoId,
                                        onClick = {
                                            selectedVideo = if (selectedVideo?.videoId == video.videoId) null else video
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ✅ 하단 확인 버튼
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { selectedVideo?.let { onVideoSelected(it) } },
                        enabled = selectedVideo != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0B699),
                            disabledContainerColor = Color(0xFFD0B699).copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            "확인",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
