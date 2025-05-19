package com.ssafy.storycut.ui.room.dialog

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.storycut.R
import com.ssafy.storycut.data.api.model.room.RoomDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ThumbnailEditDialog(
    roomDetail: RoomDto?,
    selectedImageUri: Uri?,
    onSelectImage: () -> Unit,
    onUpdateThumbnail: (Uri) -> Unit,
    onDismiss: () -> Unit,
    scope: CoroutineScope
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 다이얼로그 제목
                Text(
                    text = "썸네일 변경",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 미리보기 이미지
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                ) {
                    if (selectedImageUri != null) {
                        // 선택된 이미지 표시
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(selectedImageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "선택된 썸네일",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (roomDetail?.roomThumbnail != null && roomDetail.roomThumbnail != "default_thumbnail") {
                        // 기존 이미지 표시
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(roomDetail.roomThumbnail)
                                .crossfade(true)
                                .build(),
                            contentDescription = "현재 썸네일",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 기본 이미지
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "기본 썸네일",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }

                // 이미지 선택 버튼
                Button(
                    onClick = onSelectImage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0B699)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("이미지 선택")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 취소 버튼
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD0B699)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFD0B699))
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소")
                    }

                    // 변경하기 버튼
                    Button(
                        onClick = {
                            selectedImageUri?.let { uri ->
                                onUpdateThumbnail(uri)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0B699),
                            contentColor = Color.White
                        ),
                        enabled = selectedImageUri != null
                    ) {
                        Text("변경하기")
                    }
                }
            }
        }
    }
}