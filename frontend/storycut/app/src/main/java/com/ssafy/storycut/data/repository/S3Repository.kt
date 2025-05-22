package com.ssafy.storycut.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.ssafy.storycut.data.api.RetrofitClient
import com.ssafy.storycut.data.api.model.BaseResponse
import com.ssafy.storycut.data.api.model.Tumbnail.ThumbNailUploadResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class S3Repository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "S3Repository"
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    }

    // URI에서 실제 파일 생성 (개선 버전)
    private fun getFileFromUri(uri: Uri): File {
        Log.d(TAG, "getFileFromUri: 시작, URI: $uri")

        // 파일 정보 가져오기 (이름, 크기)
        var fileName = "upload_image_${System.currentTimeMillis()}.jpg" // 기본 파일명
        var fileSize = -1L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // 파일 이름 가져오기
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val originalFileName = cursor.getString(nameIndex)
                        Log.d(TAG, "getFileFromUri: 원본 파일명: $originalFileName")
                        // 확장자를 유지하면서 타임스탬프 추가
                        val dotIndex = originalFileName.lastIndexOf(".")
                        if (dotIndex != -1) {
                            val extension = originalFileName.substring(dotIndex)
                            fileName = "upload_image_${System.currentTimeMillis()}$extension"
                        } else {
                            fileName = "upload_image_${System.currentTimeMillis()}.jpg"
                        }
                    }

                    // 파일 크기 가져오기
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        fileSize = cursor.getLong(sizeIndex)
                        Log.d(TAG, "getFileFromUri: 파일 크기: $fileSize 바이트")

                        // 크기 확인
                        if (fileSize > MAX_FILE_SIZE) {
                            Log.e(TAG, "getFileFromUri: 파일 크기 초과: $fileSize > $MAX_FILE_SIZE")
                            throw IOException("파일 크기가 10MB를 초과합니다.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFileFromUri: 파일 정보 조회 중 오류", e)
        }

        // 콘텐츠 타입 확인
        val mimeType = context.contentResolver.getType(uri)
        Log.d(TAG, "getFileFromUri: MIME 타입: $mimeType")

        // 이미지 타입인지 확인
        if (mimeType?.startsWith("image/") != true) {
            Log.e(TAG, "getFileFromUri: 이미지 파일이 아님 (MIME 타입: $mimeType)")
            throw IOException("이미지 파일만 업로드 가능합니다.")
        }

        // 캐시 디렉토리에 파일 생성
        val file = File(context.cacheDir, fileName)

        try {
            // URI에서 읽어와 파일에 쓰기
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(4 * 1024) // 4KB 버퍼
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastProgress = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // 진행률 로깅 (10% 단위)
                        if (fileSize > 0) {
                            val progress = (totalBytesRead * 100 / fileSize).toInt()
                            if (progress >= lastProgress + 10) {
                                lastProgress = progress / 10 * 10
                                Log.d(TAG, "getFileFromUri: 파일 복사 중... $progress%")
                            }
                        }
                    }

                    output.flush()
                }
            } ?: throw IOException("URI에서 입력 스트림을 열 수 없습니다: $uri")

            Log.d(TAG, "getFileFromUri: 파일 생성 성공: ${file.absolutePath}, 크기: ${file.length()} 바이트")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "getFileFromUri: 파일 생성 중 오류", e)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "getFileFromUri: 불완전한 파일 삭제됨")
            }
            throw IOException("이미지 파일 처리 중 오류 발생: ${e.message}")
        }
    }

    suspend fun uploadRoomThumbNailImage(imageUri: Uri): Response<BaseResponse<ThumbNailUploadResponse>> {
        Log.d(TAG, "uploadRoomThumbNailImage: 시작, URI: $imageUri")

        // 업로드 중인 파일 참조 (예외처리를 위해)
        var tempFile: File? = null

        try {
            // URI에서 파일 생성 시도
            tempFile = getFileFromUri(imageUri)

            // 파일이 있는지, 읽을 수 있는지 확인
            if (!tempFile.exists() || !tempFile.canRead()) {
                Log.e(TAG, "uploadRoomThumbNailImage: 파일이 존재하지 않거나 읽을 수 없음: ${tempFile.absolutePath}")
                throw IOException("파일에 접근할 수 없습니다.")
            }

            // 콘텐츠 타입 가져오기
            val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            Log.d(TAG, "uploadRoomThumbNailImage: 사용할 MIME 타입: $mimeType")

            // MultipartBody.Part 생성
            val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
            Log.d(TAG, "uploadRoomThumbNailImage: Part 생성 완료 - 파라미터명: 'file', 파일명: ${tempFile.name}")

            // API 호출 전 로그
            Log.d(TAG, "uploadRoomThumbNailImage: API 호출 시작")

            // API 호출
            val startTime = System.currentTimeMillis()
            val response = RetrofitClient.editService.uploadRoomThumbNailImage(imagePart)
            val endTime = System.currentTimeMillis()
            Log.d(TAG, "uploadRoomThumbNailImage: API 응답 수신 (${endTime - startTime}ms)")
            Log.d(TAG, "uploadRoomThumbNailImage: 응답 코드: ${response.code()}, 메시지: ${response.message()}")

            // 응답 처리
            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d(TAG, "uploadRoomThumbNailImage: 성공, 응답 본문: $responseBody")

                if (responseBody?.isSuccess == true) {
                    val result = responseBody.result
                    val url = result?.url
                    Log.d(TAG, "uploadRoomThumbNailImage: 업로드된 이미지 URL: $url")
                } else {
                    Log.w(TAG, "uploadRoomThumbNailImage: API 성공했지만 결과는 실패, 메시지: ${responseBody?.message}")
                }
            } else {
                // 실패 응답 로깅
                val errorBody = response.errorBody()?.string() ?: "오류 본문 없음"
                Log.e(TAG, "uploadRoomThumbNailImage: 오류 응답: $errorBody")
            }

            return response
        } catch (e: Exception) {
            Log.e(TAG, "uploadRoomThumbNailImage: 예외 발생", e)

            // 커스텀 응답 생성 대신 예외를 그대로 던짐
            // 호출자는 이 예외를 처리해야 함
            throw e
        } finally {
            // 임시 파일 정리 (선택적)
            if (tempFile?.exists() == true) {
                // 여기서는 임시 파일을 유지 (디버깅 용도로 필요할 수 있음)
                // 정리가 필요하면 tempFile.delete() 호출
                Log.d(TAG, "uploadRoomThumbNailImage: 임시 파일 유지됨: ${tempFile.absolutePath}")
            }
        }
    }
}