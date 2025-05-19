package com.ssafy.storycut.ui.shorts.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// TagChip 컴포넌트 - 크기 조정
@Composable
fun TagChip(
    tag: String,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),  // 패딩 증가
        shape = RoundedCornerShape(8.dp),  // 모서리 더 둥글게
        color = Color(0xFFD0B699).copy(alpha = if (enabled) 1f else 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),  // 패딩 증가
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.bodyMedium,  // 크기 증가
                color = Color.White
            )

            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.size(20.dp)  // 크기 증가 (16dp → 20dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "태그 삭제",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)  // 크기 증가 (12dp → 16dp)
                )
            }
        }
    }
}