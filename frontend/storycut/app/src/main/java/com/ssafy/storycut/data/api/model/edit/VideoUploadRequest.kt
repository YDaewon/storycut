package com.ssafy.storycut.data.api.model.edit

data class VideoUploadRequest(
    val videoTitle: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null // 썸네일 URL 필드 추가
)