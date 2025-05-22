package com.ssafy.storycut.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Video
import com.google.api.services.youtube.model.VideoSnippet
import com.google.api.services.youtube.model.VideoStatus
import com.ssafy.storycut.data.api.RetrofitClient
import com.ssafy.storycut.data.api.model.credential.GoogleLoginRequest
import com.ssafy.storycut.data.api.model.credential.TokenDto
import com.ssafy.storycut.data.api.model.credential.UserInfo
import com.ssafy.storycut.data.api.model.credential.GooglePermissionResponse
import com.ssafy.storycut.data.local.datastore.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


private const val TAG = "AuthRepository"

@Singleton
class AuthRepository @Inject constructor(
    private val tokenManager: TokenManager,
    @ApplicationContext context: Context
) {
    // 현재 로그인한 사용자 정보를 저장
    private var currentUser: UserInfo? = null
    private val contentResolver: ContentResolver = context.contentResolver
    private val appContext = context

    /**
     * 구글 로그인 토큰을 서버로 전송하는 함수
     * @param idToken 구글 ID 토큰
     * @param onComplete 완료 시 호출되는 콜백 (결과 메시지, 토큰 쌍)
     */
    suspend fun sendTokenToServer(
        idToken: String,
        onComplete: (String, Pair<String, String>?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "토큰 전송")
                val response = RetrofitClient.authService.googleLogin(GoogleLoginRequest(idToken))

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.isSuccess) {
                        // 로그인 성공, 응답 데이터 처리
                        val result = responseBody.result
                        if (result != null) {
                            val accessToken = result.accessToken
                            val refreshToken = result.refreshToken

                            Log.d(TAG, accessToken)
                            Log.d(TAG, refreshToken)

                            // 토큰 저장
                            tokenManager.saveTokens(accessToken, refreshToken)

                            // 토큰 정보 반환
                            val tokenPair = Pair(accessToken, refreshToken)

                            getUserInfo()

                            withContext(Dispatchers.Main) {
                                onComplete("성공", tokenPair)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onComplete("성공했지만 토큰이 없습니다.", null)
                            }
                        }
                    } else {
                        // 서버에서 오류 응답
                        val errorMessage = responseBody?.message ?: "알 수 없는 오류"
                        Log.e(TAG, "서버 응답 오류: $errorMessage")
                        withContext(Dispatchers.Main) {
                            onComplete("실패: $errorMessage", null)
                        }
                    }
                } else {
                    // HTTP 오류
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "HTTP 오류 응답: ${response.code()} - $errorBody")
                    withContext(Dispatchers.Main) {
                        onComplete("실패: HTTP ${response.code()}", null)
                    }
                }
            } catch (e: Exception) {
                // 네트워크 오류 등
                Log.e(TAG, "토큰 전송 중 예외 발생", e)
                withContext(Dispatchers.Main) {
                    onComplete("실패: ${e.message}", null)
                }
            }
        }
    }

    /**
     * 사용자 정보를 가져오는 함수
     * @return 성공 시 UserInfo, 실패 시 null
     */
    suspend fun getUserInfo(): UserInfo? {
        return withContext(Dispatchers.IO) {
            try {
                // 저장된 토큰 가져오기
                val token = tokenManager.accessToken.first()
                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "토큰이 없습니다.")
                    return@withContext null
                }

                val authHeader = "Bearer $token"
                val response = RetrofitClient.authService.getUserInfo(authHeader)

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.isSuccess) {
                        // 사용자 정보를 가져오는데 성공
                        val userInfo = responseBody.result
                        if (userInfo != null) {
                            // 사용자 정보 저장
                            currentUser = userInfo
                            return@withContext userInfo
                        }
                    }
                    Log.e(TAG, "사용자 정보를 가져오는데 실패: ${responseBody?.message}")
                } else {
                    // HTTP 오류
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "사용자 정보 HTTP 오류: ${response.code()} - $errorBody")
                }

                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보를 가져오는 중 예외 발생", e)
                return@withContext null
            }
        }
    }

    /**
     * 리프레시 토큰을 사용하여 액세스 토큰을 갱신하는 함수
     * @return 성공 시 토큰 쌍, 실패 시 null
     */
    suspend fun refreshAccessToken(): Pair<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                // 저장된 리프레시 토큰 가져오기
                val refreshToken = tokenManager.refreshToken.first()
                if (refreshToken.isNullOrEmpty()) {
                    Log.e(TAG, "리프레시 토큰이 없습니다.")
                    return@withContext null
                }

                // 리프레시 토큰 헤더 설정
                val tokenDto = TokenDto(
                    accessToken = "",
                    refreshToken = refreshToken,
                    googleAccessToken = ""
                )
                val response = RetrofitClient.authService.refreshToken(tokenDto)

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.isSuccess) {
                        // 토큰 갱신 성공
                        val result = responseBody.result
                        if (result != null) {
                            val newAccessToken = result.accessToken
                            val newRefreshToken = result.refreshToken

                            // 새 토큰 저장
                            tokenManager.saveTokens(newAccessToken, newRefreshToken)
                            Log.d(TAG, "토큰 갱신 성공")

                            return@withContext Pair(newAccessToken, newRefreshToken)
                        }
                    }
                    Log.e(TAG, "토큰 갱신 실패: ${responseBody?.message}")
                } else {
                    // HTTP 오류
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "토큰 갱신 HTTP 오류: ${response.code()} - $errorBody")
                }

                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "토큰 갱신 중 예외 발생", e)
                return@withContext null
            }
        }
    }

    suspend fun getYouTubeAuthUrl(): GooglePermissionResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // 저장된 토큰 가져오기
                val token = tokenManager.accessToken.first()

                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "토큰이 없습니다.")
                    return@withContext null
                }

                val authHeader = "Bearer $token"
                val response = RetrofitClient.authService.getYouTubeAuthUrl(authHeader)

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody.isSuccess) {
                        return@withContext responseBody.result
                    }
                    Log.e(TAG, "유튜브 인증 URL을 가져오는데 실패: ${responseBody?.message}")
                } else {
                    // HTTP 오류
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "유튜브 인증 URL HTTP 오류: ${response.code()} - $errorBody")
                }

                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "유튜브 인증 URL을 가져오는 중 예외 발생", e)
                return@withContext null
            }
        }
    }

    // 클라우드 스토리지 URL에서 비디오 다운로드
    private suspend fun downloadFromCloudStorage(fileUrl: String): File = withContext(Dispatchers.IO) {
        try {
            // 임시 파일 생성
            val fileName = "youtube_upload_${UUID.randomUUID()}.mp4"
            val outputFile = File(appContext.cacheDir, fileName)

            // URL 연결 설정
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000  // 30초 타임아웃
            connection.readTimeout = 60000     // 60초 읽기 타임아웃
            connection.requestMethod = "GET"
            connection.connect()

            // 연결 상태 확인
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                Log.e(TAG, "클라우드 스토리지 다운로드 실패: $responseCode - ${connection.responseMessage}")
                throw Exception("서버 응답 오류: $responseCode")
            }

            // 파일 다운로드 - 진행률 로깅 추가
            val contentLength = connection.contentLength.toLong()
            var downloadedBytes = 0L
            val startTime = System.currentTimeMillis()

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8 * 1024)  // 8KB 버퍼
                    var bytesRead: Int

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // 10% 간격으로 진행률 로깅
                        if (contentLength > 0 && downloadedBytes % (contentLength / 10) < buffer.size) {
                            val progress = (downloadedBytes * 100 / contentLength).toInt()
                            Log.d(TAG, "다운로드 진행률: $progress%")
                        }
                    }
                    output.flush()
                }
            }

            val elapsedTime = (System.currentTimeMillis() - startTime) / 1000
            Log.d(TAG, "클라우드 스토리지 다운로드 완료 ($elapsedTime 초): ${outputFile.absolutePath}")
            Log.d(TAG, "다운로드된 파일 크기: ${outputFile.length()} 바이트")

            return@withContext outputFile

        } catch (e: Exception) {
            Log.e(TAG, "클라우드 스토리지 다운로드 중 오류 발생", e)
            throw e
        }
    }

    // Repository.kt
    suspend fun uploadVideoToYouTube(
        accessToken: String,
        videoUri: Uri,
        title: String,
        description: String,
        tags: List<String>
    ) {
        withContext(Dispatchers.IO) {
            var tempFile: File? = null

            try {
                Log.d(TAG, "유튜브 업로드 시작: $title")
                Log.d(TAG, "비디오 URI: $videoUri")

                // 입력 스트림 생성
                val inputStream = if (videoUri.toString().startsWith("http")) {
                    // HTTP/HTTPS URL인 경우 (Azure Blob, S3 등)
                    Log.d(TAG, "원격 URL에서 비디오 다운로드 시작")
                    tempFile = downloadFromCloudStorage(videoUri.toString())
                    tempFile.inputStream()
                } else {
                    // 로컬 Uri인 경우
                    Log.d(TAG, "로컬 URI에서 비디오 스트림 열기")
                    contentResolver.openInputStream(videoUri)
                } ?: throw Exception("비디오 파일을 열 수 없습니다")

                // YouTube API 클라이언트 생성
                val transport = NetHttpTransport()
                val jsonFactory = JacksonFactory.getDefaultInstance()

                // YouTube 서비스 초기화
                val credential = GoogleCredential().setAccessToken(accessToken)
                val youtube = YouTube.Builder(transport, jsonFactory, credential)
                    .setApplicationName("스토리컷")
                    .build()

                // 비디오 메타데이터 설정
                val video = Video()

                // 스니펫 정보 설정
                val snippet = VideoSnippet()
                snippet.title = title
                snippet.description = description

                // 기본 태그와 사용자 태그 병합
                val defaultTags = listOf("쇼츠")
                val allTags = (defaultTags + tags).distinct()
                snippet.tags = allTags

                snippet.categoryId = "22" // People & Blogs 카테고리
                video.snippet = snippet

                // 상태 정보 설정
                val status = VideoStatus()
                status.privacyStatus = "unlisted" // 또는 "private", "public"
                status.selfDeclaredMadeForKids = false
                video.status = status

                // 미디어 콘텐츠 생성
                val mediaContent = InputStreamContent("video/mp4", inputStream)

                // 업로드 요청 생성
                val videosInsertRequest = youtube.videos().insert(
                    "snippet,status",
                    video,
                    mediaContent
                )

                // 업로드 실행
                Log.d(TAG, "유튜브 API 호출 시작")
                val uploadedVideo = videosInsertRequest.execute()

                Log.d(TAG, "비디오가 성공적으로 업로드되었습니다! 비디오 ID: ${uploadedVideo.id}")
                Log.d(TAG, "비디오 URL: https://youtu.be/${uploadedVideo.id}")

            } catch (e: Exception) {
                Log.e(TAG, "유튜브 업로드 중 오류 발생", e)
                throw e
            } finally {
                // 임시 파일 삭제
                tempFile?.let {
                    if (it.exists()) {
                        val deleted = it.delete()
                        Log.d(TAG, "임시 파일 삭제 ${if (deleted) "성공" else "실패"}: ${it.absolutePath}")
                    }
                }
            }
        }
    }

    // AuthRepository.kt
    suspend fun refreshGoogleToken(): String {
        return withContext(Dispatchers.IO) {
            try {
                // JWT 토큰 가져오기
                val jwtToken = tokenManager.accessToken.first()
                    ?: throw Exception("JWT 토큰이 없습니다")

                // Bearer 형식으로 토큰 포맷팅
                val authorization = "Bearer $jwtToken"

                // API 호출
                val response = RetrofitClient.authService.refreshGoogleToken(authorization)

                // response.body()의 null 체크
                val responseBody = response.body()
                    ?: throw Exception("서버 응답이 없습니다")

                if (responseBody.isSuccess) {
                    // 응답에서 구글 토큰 정보 가져오기
                    val result = responseBody.result
                    if (result?.googleAccessToken != null) {
                        // 새 구글 액세스 토큰 저장
                        tokenManager.saveGoogleAccessTokens(result.googleAccessToken)

                        Log.d(TAG, "구글 토큰 갱신 성공: ${result.googleAccessToken}")
                        return@withContext result.googleAccessToken
                    } else {
                        throw Exception("구글 액세스 토큰이 응답에 포함되지 않았습니다.")
                    }
                } else {
                    throw Exception("구글 토큰 갱신 실패: ${responseBody.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "구글 토큰 갱신 실패", e)
                throw e
            }
        }
    }
}