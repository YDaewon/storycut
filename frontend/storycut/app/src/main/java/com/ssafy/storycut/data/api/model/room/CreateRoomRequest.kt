package com.ssafy.storycut.data.api.model.room

data class CreateRoomRequest(
    val roomTitle: String,
    val roomPassword: String?,
    val roomContext: String,
    val roomThumbnail: String
)
