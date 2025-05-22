package com.ssafy.storycut.ui.shorts.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SelectedVideoState(
    uri: Uri,
    title: String,
    description: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSelectOther: () -> Unit,
    onUpload: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 선택된 비디오 정보 카드
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "선택된 비디오",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = uri.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 제목 입력 필드
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("쇼츠 제목") },
            placeholder = { Text("제목을 입력하세요") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 설명 입력 필드
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("쇼츠 설명") },
            placeholder = { Text("설명을 입력하세요") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            enabled = !isLoading,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 업로드 버튼
        Button(
            onClick = onUpload,
            enabled = !isLoading && title.isNotBlank(),  // 제목이 입력되어야 활성화
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("유튜브 쇼츠로 업로드하기")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 다른 비디오 선택 버튼
        OutlinedButton(
            onClick = onSelectOther,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("다른 비디오 선택하기")
        }
    }
}