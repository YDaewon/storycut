package com.ssafy.storycut.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun VideoUploadDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onShortsClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 갤러리(비디오) 버튼
                Button(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE7B549)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("갤러리", color = Color.White)
                }

                // 내 쇼츠 버튼
                Button(
                    onClick = onShortsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE7B549)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("내 쇼츠", color = Color.White)
                }
            }
        }
    }
}