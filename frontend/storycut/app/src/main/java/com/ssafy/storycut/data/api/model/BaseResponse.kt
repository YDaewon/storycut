package com.ssafy.storycut.data.api.model

data class BaseResponse<T>(
    val isSuccess: Boolean,
    val code: Long,
    val message: String,
    val result: T? = null
)
