package com.ssafy.storycut.data.api.model.chat

data class ChatMessageRequest(
    val videoId: String,
    val title: String,
    val mediaUrl: String,
    val thumbnailUrl: String
)

data class ChatDto(
    val id: String,
    val roomId: Long,
    val senderId: Long,
    val videoId: String,
    val title: String,
    val mediaUrl: String,
    val thumbnailUrl: String,
    val createdAt: String
)