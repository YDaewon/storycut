package com.ssafy.storycut.ui.splash.components

import android.util.Log
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun Loader() {
    // 로티 JSON 파일 가져오기
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset("splash.json"))
    val composition = compositionResult.value
    // 로티 애니메이션 정렬하기
    val animationState = animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    if (composition != null) {
        // 로티 애니메이션 뷰
        LottieAnimation(
            composition = composition,
            progress = { animationState.progress },
            modifier = Modifier             // 로티 위젯 크기 설정
                .fillMaxWidth()
                .padding(16.dp)
        )
    } else {
        // 로딩 중 대체 UI
        Log.d("Loader", "Lottie composition is still null")
        CircularProgressIndicator()
    }
}