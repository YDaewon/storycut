package com.ssafy.storycut.data.api.model.edit

data class VideoProcessRequest(
    val prompt: String,
    val videoId: Long,
    val images: List<String> = emptyList(),
    val videoTitle : String,
    val subtitle: Boolean,
    val musicPrompt: String,
    val autoMusic: Boolean
)