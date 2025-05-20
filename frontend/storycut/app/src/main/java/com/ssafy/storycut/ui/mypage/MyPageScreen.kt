package com.ssafy.storycut.ui.mypage


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ssafy.storycut.R
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.ui.auth.AuthViewModel
import com.ssafy.storycut.ui.settings.AnimatedSettingsNavigation
import kotlinx.coroutines.flow.first

@Composable
fun MyPageScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    myVideoViewModel: VideoViewModel = hiltViewModel(),
    navController: NavController? = null,
    tokenManager: TokenManager,
    onNavigateToLogin: () -> Unit = {}
) {
    val userInfo by authViewModel.userState.collectAsState()
    val videoList by myVideoViewModel.myVideos.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    val createdAt = userInfo?.createdAt
    val postCount = videoList.size

    // 포커스 관리를 위한 변수 추가
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    // 날짜 포맷변환
    fun formatCreatedAt(createdAt: String?): String {
        if (createdAt.isNullOrEmpty()) return "정보 없음"

        return try {
            val isoPattern = "yyyy-MM-dd'T'HH:mm:ss"
            val outputPattern = "yyyy.MM.dd"

            val inputFormat = java.text.SimpleDateFormat(isoPattern, java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat(outputPattern, java.util.Locale.getDefault())

            val date = inputFormat.parse(createdAt)
            outputFormat.format(date)
        } catch (e: Exception) {
            createdAt
        }
    }

    LaunchedEffect(Unit) {
        // 사용자 정보 새로고침
        authViewModel.refreshUserInfoFromRoom()

        // 토큰을 가져와서 내 비디오 목록 로드
        try {
            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {
                Log.d("VideoViewModel","${token}")
                myVideoViewModel.fetchMyVideos()
            }
        } catch (e: Exception) {
            println("토큰 가져오기 실패: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // 다른 화면으로 이동할 때 포커스 해제
    LaunchedEffect(navController?.currentBackStackEntry) {
        focusManager.clearFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // 화면의 다른 부분을 클릭하면 포커스 제거
                if (isFocused) {
                    focusManager.clearFocus()
                }
            }
    ) {
        // 기본 마이페이지 콘텐츠
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(if (showSettings) 0f else 1f)
        ) {
            // 설정 아이콘
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.setting_icon),
                    contentDescription = "설정",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            showSettings = true
                        }
                )
            }

            // 프로필 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = userInfo?.profileImg,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(text = userInfo?.nickname ?: "사용자", fontWeight = FontWeight.Bold)
                    Text(text = userInfo?.email ?: "이메일 없음", style = MaterialTheme.typography.bodySmall)
                    Text(text = "가입일: ${formatCreatedAt(createdAt)}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "게시글 수: $postCount", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = LocalTextStyle.current,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = Color(0xFFFCF7F0),
                        unfocusedContainerColor = Color(0xFFFCF7F0),
                        cursorColor = Color(0xFFD0B699),
                        selectionColors = TextSelectionColors(
                            handleColor = Color(0xFFD0B699),
                            backgroundColor = Color(0xFFD0B699).copy(alpha = 0.3f)
                        )
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "검색",
                            tint = Color(0xFFAAAAAA)
                        )
                    },
                    placeholder = {
                        Text(
                            "영상 검색하기",
                            color = Color(0xFFAAAAAA)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(32.dp)
                        )
                        .clip(RoundedCornerShape(32.dp))
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 비디오 목록 표시
            if (isLoading) {
                // 로딩 중 표시
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (videoList.isEmpty()) {
                // 비디오가 없는 경우
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "첫 쇼츠를 업로드해 보세요",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 비디오 그리드 표시
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(videoList.filter {
                        searchQuery.isEmpty() ||
                                it.videoTitle.contains(searchQuery, ignoreCase = true)
                    }) { video ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f/16f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    // 비디오 클릭 시 로그 추가
                                    Log.d("MyPageScreen", "비디오 클릭: ID=${video.videoId}, 제목=${video.videoTitle}")

                                    // 포커스 해제
                                    focusManager.clearFocus()

                                    // 비디오 상세 페이지로 이동
                                    navController?.navigate("video_detail/${video.videoId}")
                                }
                        ) {
                            // 썸네일 이미지
                            AsyncImage(
                                model = video.thumbnail,
                                contentDescription = video.videoTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.ic_launcher_foreground)
                            )

                            // 비디오 정보를 표시하는 오버레이 추가
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                            ) {
                                Text(
                                    text = video.videoTitle,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(
                                            color = Color.Black,
                                            blurRadius = 4f,
                                            offset = androidx.compose.ui.geometry.Offset(1f, 1f)
                                        )
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // 설정 화면 오버레이
        if (navController != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (showSettings) 1f else 0f)
            ) {
                AnimatedSettingsNavigation(
                    authViewModel = authViewModel,
                    isVisible = showSettings,
                    onDismiss = {
                        showSettings = false
                        // 설정 화면을 닫을 때도 포커스 해제
                        focusManager.clearFocus()
                    },
                    onNavigateToLogin = onNavigateToLogin
                )
            }
        }
    }
}