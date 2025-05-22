package com.ssafy.storycut.data.api.model.edit

data class MosaicRequest(
    val videoId: Long,
    val videoTitle: String,
    val images: List<String>
)