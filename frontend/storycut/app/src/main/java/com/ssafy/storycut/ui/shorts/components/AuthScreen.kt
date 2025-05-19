package com.ssafy.storycut.ui.shorts.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(
    onRequestAuth: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 유튜브 아이콘
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "YouTube",
            modifier = Modifier.size(100.dp),
            tint = Color(0xFFFE4343)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 안내 텍스트
        Text(
            text = "유튜브 계정 연결이 필요합니다",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "쇼츠 업로드 기능을 사용하기 위해\n유튜브 계정 접근 권한이 필요합니다.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 연결 버튼
        Button(
            onClick = onRequestAuth,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFE4343)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "유튜브 계정 연결하기",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}