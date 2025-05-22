package com.ssafy.storycut.data.api.model

data class VideoShareRequest(
    val videoId: String,
    val title: String,
    val mediaUrl: String,
    val thumbnailUrl: String
)

data class VideoShareDto(
    val id: String,
    val roomId: Long,
    val senderId: Long,
    val videoId: String,
    val title: String,
    val mediaUrl: String,
    val thumbnailUrl: String,
    val createdAt: String
)