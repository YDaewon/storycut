package com.ssafy.storycut.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.ssafy.storycut.data.api.model.VideoDto
import com.ssafy.storycut.data.api.model.edit.VideoProcessRequest
import com.ssafy.storycut.data.api.model.edit.VideoUploadRequest
import com.ssafy.storycut.data.api.service.EditService
import com.ssafy.storycut.data.local.datastore.FCMTokenManager
import com.ssafy.storycut.data.local.datastore.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

@Singleton
class EditRepository @Inject constructor(
    private val editService: EditService,
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val fcmTokenManager: FCMTokenManager,
    private val plainOkHttpClient: OkHttpClient  // OkHttpClient 주입 추가
) {

    suspend fun uploadVideoWithPresignedUrl(videoUri: Uri, videoTitle: String, thumbnailBitmap: Bitmap? = null): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                // URI에서 파일 생성
                val videoFile = context.contentResolver.openInputStream(videoUri)?.use { inputStream ->
                    val tempFile = File.createTempFile("video", ".mp4", context.cacheDir)
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    tempFile
                } ?: throw Exception("비디오 파일을 처리할 수 없습니다")

                // 파일이 비어있는지 확인
                if (videoFile.length() == 0L) {
                    return@withContext Result.failure(Exception("비디오 파일이 비어있습니다"))
                }

                // 썸네일 업로드 및 URL 얻기
                var thumbnailUrl: String? = null
                if (thumbnailBitmap != null) {
                    // 비트맵을 파일로 변환
                    val thumbnailFile = File.createTempFile("thumbnail", ".jpg", context.cacheDir)
                    FileOutputStream(thumbnailFile).use { outputStream ->
                        thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    }

                    // 썸네일 이미지 업로드
                    thumbnailUrl = uploadThumbnailFile(thumbnailFile)
                }

                // 파일명에서 확장자 추출
                val originalFilename = videoUri.lastPathSegment ?: "video.mp4"

                // 1. Presigned URL 발급 받기
                val presignedResponse = editService.getPresignedUrl(originalFilename)

                if (!presignedResponse.isSuccessful || presignedResponse.body() == null) {
                    return@withContext Result.failure(Exception("Presigned URL 발급 실패: ${presignedResponse.message()}"))
                }

                val presignedData = presignedResponse.body()!!
                val uploadUrl = presignedData.uploadUrl
                val videoUrl = presignedData.videoUrl

                Log.d("EditRepository", "Presigned URL 발급 성공: $uploadUrl")

                // 2. Azure Blob Storage에 파일 업로드
                val mimeType = context.contentResolver.getType(videoUri) ?: "video/mp4"
                val requestBody = videoFile.asRequestBody(mimeType.toMediaTypeOrNull())

                // Azure Blob Storage에 필요한 헤더 추가
                val request = Request.Builder()
                    .url(uploadUrl)
                    .put(requestBody)
                    .addHeader("x-ms-blob-type", "BlockBlob")
                    .addHeader("Content-Type", mimeType)
                    .addHeader("Content-Length", videoFile.length().toString())
                    .build()

                Log.d("EditRepository", "Azure Blob Storage 업로드 요청 헤더: ${request.headers}")
                Log.d("EditRepository", "Azure Blob Storage 업로드 URL: $uploadUrl")

                val uploadResponse = plainOkHttpClient.newCall(request).execute()

                if (!uploadResponse.isSuccessful) {
                    val errorBody = uploadResponse.body?.string() ?: "응답 없음"
                    Log.e("EditRepository", "Azure Blob Storage 업로드 실패: ${uploadResponse.code}, ${uploadResponse.message}, 응답: $errorBody")
                    return@withContext Result.failure(Exception("Azure Blob Storage 업로드 실패: ${uploadResponse.message}"))
                }

                Log.d("EditRepository", "Azure Blob Storage 업로드 성공: 응답 코드 ${uploadResponse.code}")

                // 3. 서버에 비디오 등록 (썸네일 URL 추가)
                val authToken = "Bearer ${tokenManager.accessToken.first() ?: ""}"
                val videoUploadRequest = VideoUploadRequest(
                    videoTitle = videoTitle,
                    videoUrl = videoUrl,
                    thumbnailUrl = thumbnailUrl
                )

                val response = editService.uploadVideo(authToken, videoUploadRequest)

                if (response.isSuccessful) {
                    val baseResponse = response.body()
                    if (baseResponse?.isSuccess == true && baseResponse.result != null) {
                        // VideoDto에서 videoId 추출
                        val videoDto = baseResponse.result
                        Log.d("EditRepository", "비디오 등록 성공: ID=${videoDto.videoId}")

                        Result.success(videoDto.videoId)
                    } else {
                        Result.failure(Exception("비디오 등록 실패: ${baseResponse?.message ?: "알 수 없는 오류"}"))
                    }
                } else {
                    // 오류 응답의 바디를 로그에 기록
                    val errorBody = response.errorBody()?.string()
                    Log.e("EditRepository", "서버 오류 응답: ${errorBody ?: "없음"}")

                    Result.failure(Exception("비디오 등록 실패: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("EditRepository", "비디오 업로드 예외", e)
                Result.failure(e)
            }
        }
    }

    suspend fun registerExistingVideo(
        videoUrl: String,
        videoTitle: String,
        thumbnailUrl: String? = null
    ): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                // 서버에 비디오 등록
                val authToken = "Bearer ${tokenManager.accessToken.first() ?: ""}"
                val videoUploadRequest = VideoUploadRequest(
                    videoTitle = videoTitle,
                    videoUrl = videoUrl,
                    thumbnailUrl = thumbnailUrl
                )

                val response = editService.uploadVideo(authToken, videoUploadRequest)

                if (response.isSuccessful) {
                    val baseResponse = response.body()
                    if (baseResponse?.isSuccess == true && baseResponse.result != null) {
                        // VideoDto에서 videoId 추출
                        val videoDto = baseResponse.result
                        Log.d("EditRepository", "비디오 등록 성공: ID=${videoDto.videoId}")

                        Result.success(videoDto.videoId)
                    } else {
                        Result.failure(Exception("비디오 등록 실패: ${baseResponse?.message ?: "알 수 없는 오류"}"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("EditRepository", "서버 오류 응답: ${errorBody ?: "없음"}")

                    Result.failure(Exception("비디오 등록 실패: ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e("EditRepository", "비디오 등록 예외", e)
                Result.failure(e)
            }
        }
    }

    // 썸네일 파일 업로드 함수 (presigned URL 사용)
    private suspend fun uploadThumbnailFile(thumbnailFile: File): String? {
        return try {
            // 1. 썸네일용 Presigned URL 발급 받기
            val presignedResponse = editService.getPresignedUrl(thumbnailFile.name)

            if (!presignedResponse.isSuccessful || presignedResponse.body() == null) {
                Log.e("EditRepository", "썸네일 Presigned URL 발급 실패: ${presignedResponse.message()}")
                return null
            }

            val presignedData = presignedResponse.body()!!
            val uploadUrl = presignedData.uploadUrl
            val thumbnailUrl = presignedData.videoUrl // 실제로는 이미지 URL이지만 API는 동일함

            Log.d("EditRepository", "썸네일 Presigned URL 발급 성공: $uploadUrl")

            // 2. Azure Blob Storage에 썸네일 업로드
            val requestBody = thumbnailFile.asRequestBody("image/jpeg".toMediaTypeOrNull())

            // Azure Blob Storage에 필요한 헤더 추가
            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .addHeader("x-ms-blob-type", "BlockBlob")
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("Content-Length", thumbnailFile.length().toString())
                .build()

            val uploadResponse = plainOkHttpClient.newCall(request).execute()

            if (!uploadResponse.isSuccessful) {
                val errorBody = uploadResponse.body?.string() ?: "응답 없음"
                Log.e("EditRepository", "썸네일 Azure Blob Storage 업로드 실패: ${uploadResponse.code}, ${uploadResponse.message}, 응답: $errorBody")
                return null
            }

            Log.d("EditRepository", "썸네일 Azure Blob Storage 업로드 성공: $thumbnailUrl")
            thumbnailUrl
        } catch (e: Exception) {
            Log.e("EditRepository", "썸네일 업로드 중 오류 발생", e)
            null
        }
    }

    suspend fun uploadImages(imageUris: List<Uri>): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val imageParts = mutableListOf<MultipartBody.Part>()

                // 각 URI에서 파일 생성 및 MultipartBody.Part 추가
                for (imageUri in imageUris) {
                    // 실제 MIME 타입 가져오기
                    val mimeType = context.contentResolver.getType(imageUri) ?: "image/*"

                    val imageFile = context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                        // 파일 확장자를 MIME 타입에서 추출
                        val extension = when (mimeType) {
                            "image/jpeg", "image/jpg" -> ".jpg"
                            "image/png" -> ".png"
                            "image/gif" -> ".gif"
                            "image/webp" -> ".webp"
                            "image/heic", "image/heif" -> ".heic"
                            "image/bmp" -> ".bmp"
                            else -> ".tmp" // 기본 확장자
                        }

                        val tempFile = File.createTempFile("image", extension, context.cacheDir)
                        FileOutputStream(tempFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        tempFile
                    } ?: continue

                    // 실제 MIME 타입으로 RequestBody 생성
                    val imageRequestBody = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("files", imageFile.name, imageRequestBody)
                    imageParts.add(imagePart)
                }

                if (imageParts.isEmpty()) {
                    throw Exception("처리할 이미지가 없습니다")
                }

                // API 호출
                val response = editService.uploadImages(imageParts)

                if (response.isSuccessful) {
                    val baseResponse = response.body()
                    if (baseResponse?.isSuccess == true && baseResponse.result != null) {
                        // ImageUploadResponse 객체에서 URL 리스트를 추출
                        val imageUrls = baseResponse.result.imageUrls
                        Result.success(imageUrls)
                    } else {
                        Result.failure(Exception("이미지 업로드 실패: ${baseResponse?.message ?: "알 수 없는 오류"}"))
                    }
                } else {
                    Result.failure(Exception("이미지 업로드 실패: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun processVideo(
        prompt: String?,
        videoId: Long,
        imageUrls: List<String>,
        videoTitle: String,
        applySubtitle: Boolean,
        musicPrompt: String?,
        autoMusic: Boolean = false
    ): Result<Boolean> {  // VideoDto 대신 Boolean으로 변경 (성공 여부만 반환)
        return withContext(Dispatchers.IO) {
            try {
                // 요청 객체 생성
                val request = VideoProcessRequest(
                    prompt = prompt ?: "",
                    videoId = videoId,
                    images = imageUrls,
                    videoTitle = videoTitle,
                    subtitle = applySubtitle,
                    musicPrompt = musicPrompt ?: "",
                    autoMusic = autoMusic  // autoMusic 필드 추가
                )

                // 인증 토큰 가져오기
                val authToken = "Bearer ${tokenManager.accessToken.first() ?: ""}"

                // FCM 토큰 가져오기
                val deviceToken = getFCMToken()

                // API 호출
                val response = editService.processVideo(authToken, deviceToken, request)

                // 응답 처리 (단순화)
                val baseResponse = response.body()

                if (response.isSuccessful && baseResponse?.isSuccess == true) {
                    Log.d("EditRepository", "영상 처리 요청 성공: ${baseResponse.message}")
                    Result.success(true)
                } else {
                    // 실패 처리
                    val errorMsg = baseResponse?.message ?: response.message() ?: "알 수 없는 오류"
                    Log.e("EditRepository", "영상 처리 요청 실패: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                Log.e("EditRepository", "영상 처리 예외 발생", e)
                Result.failure(e)
            }
        }
    }

    // FCM 토큰 가져오기 (없으면 새로 요청)
    private suspend fun getFCMToken(): String {
        // 저장된 토큰 확인
        val savedToken = fcmTokenManager.getToken()

        // 토큰이 있으면 바로 반환
        if (savedToken.isNotEmpty()) {
            return savedToken
        }

        // 토큰이 없으면 새로 요청
        return suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    // 토큰 저장
                    fcmTokenManager.saveToken(token)
                    // 결과 반환
                    continuation.resume(token)
                } else {
                    Log.e("EditRepository", "FCM 토큰 가져오기 실패", task.exception)
                    // 빈 문자열 반환
                    continuation.resume("")
                }
            }
        }
    }


}