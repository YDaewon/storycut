package com.ssafy.storycut.data.api.model.room

data  class EditRoomDto (
    val roomTitle: String,
    val roomContext: String,
    val roomPassword: String? = null
)