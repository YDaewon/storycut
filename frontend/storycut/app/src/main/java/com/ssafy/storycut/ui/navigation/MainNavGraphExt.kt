package com.ssafy.storycut.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ssafy.storycut.data.local.datastore.TokenManager
import com.ssafy.storycut.ui.auth.AuthViewModel
import com.ssafy.storycut.ui.edit.EditScreen
import com.ssafy.storycut.ui.home.HomeScreen
import com.ssafy.storycut.ui.mypage.MyPageScreen
import com.ssafy.storycut.ui.mypage.VideoViewModel
import com.ssafy.storycut.ui.settings.SettingsScreen
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ssafy.storycut.ui.edit.EditViewModel
import com.ssafy.storycut.ui.mypage.VideoDetailScreen
import com.ssafy.storycut.ui.room.RoomDetailScreen // 추가된 import
import com.ssafy.storycut.ui.room.video.RoomVideoDetailScreen
import com.ssafy.storycut.ui.shorts.ShortsScreen
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

/**
 * 메인 화면의 네비게이션 그래프를 확장 함수로 정의
 */
@OptIn(UnstableApi::class)
fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    tokenManager: TokenManager,
    onNavigateToLogin: () -> Unit,
    innerPadding: PaddingValues
) {
    composable(Navigation.Main.HOME) {
        HomeScreen(
            onRoomClick = { roomId ->
                navController.navigate("room_detail/$roomId")
            },
            navController = navController // NavController 전달
        )
    }

    composable(Navigation.Main.EDIT) { backStackEntry ->
        // hiltViewModel()을 사용하여 EditViewModel 인스턴스 생성
        val editViewModel = hiltViewModel<EditViewModel>()
        val VideoViewModel = hiltViewModel<VideoViewModel>()
        val bottomNavViewModel = hiltViewModel<BottomNavigationViewModel>()
        EditScreen(
            viewModel = editViewModel,
            videoViewModel = VideoViewModel,
            bottomNavViewModel = bottomNavViewModel,
            onEditSuccess = { videoId ->
                // 편집 성공 시 비디오 상세 페이지로 이동
                navController.navigate("video_detail/$videoId")
            },
        )
    }

    composable(Navigation.Main.SHORTS_UPLOAD) {
        ShortsScreen()
    }

    composable(Navigation.Main.MYPAGE) {
        // hiltViewModel()을 사용하여 VideoViewModel 인스턴스 생성
        val myVideoViewModel = hiltViewModel<VideoViewModel>()
        MyPageScreen(
            authViewModel = authViewModel,
            myVideoViewModel = myVideoViewModel,
            tokenManager = tokenManager,
            navController = navController,
            onNavigateToLogin = onNavigateToLogin
        )
    }

    composable(Navigation.Main.SETTINGS) {
        SettingsScreen(
            authViewModel = authViewModel,
            onBackPressed = { navController.popBackStack() },
            onNavigateToLogin = onNavigateToLogin
        )
    }

    composable(
        route = "room_detail/{roomId}",
        arguments = listOf(navArgument("roomId") { type = NavType.StringType })
    ) { backStackEntry ->
        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
        RoomDetailScreen(
            roomId = roomId,
            navController = navController,
            tokenManager = tokenManager
        )
    }

    composable(
        route = "video_detail/{videoId}",
        arguments = listOf(navArgument("videoId") { type = NavType.StringType })
    ) { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
        VideoDetailScreen(
            videoId = videoId,
            navController = navController,
        )
    }

    // Room Video Detail 화면 (중복 제거함)
    composable(
        route = "room_video_detail/{roomId}/{videoId}",
        arguments = listOf(
            navArgument("roomId") { type = NavType.StringType },
            navArgument("videoId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
        RoomVideoDetailScreen(
            roomId = roomId,
            videoId = videoId,
            navController = navController,
            tokenManager = tokenManager
        )
    }

    composable(
        route = "video_detail/{videoId}/original",
        arguments = listOf(navArgument("videoId") { type = NavType.StringType })
    ) { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
        VideoDetailScreen(
            videoId = videoId,
            navController = navController,
            isOriginalMode = true // 원본 모드 플래그 추가
        )
    }
}