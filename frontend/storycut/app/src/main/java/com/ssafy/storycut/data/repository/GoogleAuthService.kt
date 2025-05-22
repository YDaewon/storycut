package com.ssafy.storycut.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.ssafy.storycut.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GoogleAuthService"
private const val WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID;

@Singleton
class GoogleAuthService @Inject constructor() {

    suspend fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager,
        onTokenReceived: (String) -> Unit
    ) {
        try {
            // 구글 로그인 옵션 설정
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            // 인증 요청 생성
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 사용자 인증 정보 가져오기
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignInResult(result, onTokenReceived)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "구글 로그인 실패: ${e.message}")

            // 승인된 계정이 없는 경우, 새 계정 선택 화면 표시
            if (e.message?.contains("사용자에게 계정이 없습니다") == true) {
                try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(WEB_CLIENT_ID)
                        .setAutoSelectEnabled(true)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(
                        request = request,
                        context = context
                    )

                    handleSignInResult(result, onTokenReceived)
                } catch (e: GetCredentialException) {
                    Log.e(TAG, "새 계정 선택 실패: ${e.message}")
                }
            }
        }
    }

    private fun handleSignInResult(
        result: GetCredentialResponse,
        onTokenReceived: (String) -> Unit
    ) {
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken

                        // 토큰을 콜백으로 전달
                        onTokenReceived(idToken)

                        // 로그에 토큰 기록 (디버깅용)
                        Log.d(TAG, "구글 ID 토큰: $idToken")
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "올바르지 않은 구글 ID 토큰 응답 수신", e)
                    }
                } else {
                    Log.e(TAG, "예상치 못한 형식의 사용자 인증 정보")
                }
            }
            else -> {
                Log.e(TAG, "예상치 못한 형식의 사용자 인증 정보")
            }
        }
    }
}