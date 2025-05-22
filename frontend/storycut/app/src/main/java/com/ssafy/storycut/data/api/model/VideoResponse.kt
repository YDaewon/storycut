package com.ssafy.storycut.data.api.model

data class VideoDto(
    val videoId: Long,
    val memberId: Long,
    val videoTitle: String,
    val videoUrl: String,
    val thumbnail: String,
    val originalVideoId: Long,
    val blur: Boolean,
    val createdAt: String,
    val updatedAt: String
)