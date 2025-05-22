package com.ssafy.storycut.ui.shorts.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun UploadScreen(
    selectedVideoUri: Uri?,
    onSelectVideo: () -> Unit,
    onUpload: (Uri, String, String, List<String>) -> Unit,  // 태그 매개변수 추가
    isLoading: Boolean
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // 업로드
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 화면 제목
        Text(
            text = "유튜브 쇼츠 업로드",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 비디오 선택 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // 비디오 선택 영역
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable(enabled = !isLoading) { onSelectVideo() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)  // 최소 높이 설정
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)  // Box 내에서 상하 중앙 정렬
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center  // 수직 방향으로도 중앙 정렬
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (selectedVideoUri != null) "선택된 비디오" else "비디오 선택",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (selectedVideoUri != null) {
                            Text(
                                text = selectedVideoUri.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

        }

        Spacer(modifier = Modifier.height(16.dp))

        // 제목 입력 필드
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
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
            onValueChange = { description = it },
            label = { Text("쇼츠 설명") },
            placeholder = { Text("설명을 입력하세요") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            enabled = !isLoading,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 태그 입력 영역
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 태그 입력 필드와 추가 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("태그") },
                    placeholder = { Text("태그를 입력하세요") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (tagInput.isNotBlank()) {
                            tags = tags + tagInput.trim()
                            tagInput = ""
                        }
                    },
                    enabled = !isLoading && tagInput.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("추가")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 입력된 태그 표시
            if (tags.isNotEmpty()) {
                Text(
                    text = "입력된 태그",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        maxItemsInEachRow = 3
                    ) {
                        tags.forEachIndexed { index, tag ->
                            TagChip(
                                tag = tag,
                                onRemove = {
                                    tags = tags.filterIndexed { i, _ -> i != index }
                                },
                                enabled = !isLoading
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 업로드 버튼
        Button(
            onClick = {
                selectedVideoUri?.let { uri ->
                    onUpload(uri, title, description, tags)
                }
            },
            enabled = !isLoading && selectedVideoUri != null,
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
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    maxItemsInEachRow: Int = Int.MAX_VALUE,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rows = mutableListOf<MutableList<Placeable>>()
        val rowConstraints = constraints.copy(minWidth = 0)

        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        var currentRowCount = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(rowConstraints)

            if (currentRowWidth + placeable.width > constraints.maxWidth || currentRowCount >= maxItemsInEachRow) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowCount = 0
            }

            currentRow.add(placeable)
            currentRowWidth += placeable.width
            currentRowCount++
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val height = rows.sumOf { row -> row.maxOfOrNull { it.height } ?: 0 }

        layout(constraints.maxWidth, height) {
            var y = 0

            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0

                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width
                }

                y += rowHeight
            }
        }
    }
}