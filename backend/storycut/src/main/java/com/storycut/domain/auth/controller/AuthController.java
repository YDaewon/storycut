package com.storycut.domain.auth.controller;

import com.storycut.domain.auth.model.CustomUserDetails;
import com.storycut.domain.auth.model.dto.GoogleLoginRequest;
import com.storycut.domain.auth.model.dto.TokenDto;
import com.storycut.domain.auth.model.dto.YouTubeAuthResponse;
import com.storycut.domain.auth.service.GoogleAuthService;
import com.storycut.domain.auth.service.TokenService;
import com.storycut.global.model.dto.BaseResponse;
import com.storycut.global.model.dto.BaseResponseStatus;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthAPI {

    private final TokenService tokenService;
    private final GoogleAuthService googleAuthService;

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    @Value("${app.baseUrl}")
    private String baseUrl;

    @Override
    public ResponseEntity<BaseResponse<TokenDto>> googleLogin(GoogleLoginRequest request) {
        log.info("[구글 로그인] 모바일 앱 요청");
        TokenDto tokenDto = googleAuthService.processGoogleLogin(request.getIdToken());
        return ResponseEntity.ok(new BaseResponse<>(tokenDto));
    }

    @Override
    public void googleWebLogin(HttpServletResponse response) throws Exception {
        log.info("[구글 로그인] 웹 요청 - OAuth 리다이렉트");
        response.sendRedirect("/api/v1/spring/oauth2/authorization/google");
    }

    @Override
    public ResponseEntity<BaseResponse<TokenDto>> testLogin() {
        if (!devMode) {
            return ResponseEntity.badRequest().body(new BaseResponse<>(BaseResponseStatus.UNAUTHORIZED));
        }
        log.info("[테스트 로그인] 개발 모드에서 테스트 계정 로그인");
        TokenDto tokenDto = googleAuthService.processTestLogin();
        return ResponseEntity.ok(new BaseResponse<>(tokenDto));
    }

    @Override
    public RedirectView handleOAuth2Callback(String code, String state) {
        log.info("[OAuth2 콜백] 인증 코드 수신 - state: {}", state);
        Long memberId = tokenService.validateAuthState(state);
        if (memberId == null) {
            log.warn("[OAuth2 콜백] 유효하지 않은 상태 토큰: {}", state);
            return new RedirectView(baseUrl + "/auth/error?error=invalid_state");
        }

        try {
            TokenDto tokenDto = googleAuthService.exchangeAuthCodeForTokens(code, memberId);
            tokenService.deleteAuthState(state);
            log.info("[OAuth2 콜백] 토큰 교환 성공 - 사용자 ID: {}", memberId);
            return new RedirectView("storycut://auth/success?token=" + tokenDto.getGoogleAccessToken());
        } catch (Exception e) {
            log.error("[OAuth2 콜백] 토큰 교환 실패", e);
            return new RedirectView(baseUrl + "/auth/error?error=token_exchange_failed");
        }
    }

    @Override
    public ResponseEntity<BaseResponse<YouTubeAuthResponse>> getYouTubeAuthUrl(CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        log.info("[유튜브 권한] URL 생성 요청 - 사용자 ID: {}", memberId);
        YouTubeAuthResponse response = googleAuthService.generateYouTubeAuthUrl(memberId);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }

    @Override
    public ResponseEntity<BaseResponse<Boolean>> checkYouTubeAccess(CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        log.info("[유튜브 권한] 상태 확인 - 사용자 ID: {}", memberId);
        boolean hasAccess = googleAuthService.hasYouTubeAccess(memberId);
        return ResponseEntity.ok(new BaseResponse<>(hasAccess));
    }

    @Override
    public ResponseEntity<BaseResponse<TokenDto>> refreshGoogleToken(CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        log.info("[토큰 갱신] 구글 액세스 토큰 갱신 - 사용자 ID: {}", memberId);
        TokenDto newGoogleTokenDto = googleAuthService.refreshGoogleAccessToken(memberId);
        return ResponseEntity.ok(new BaseResponse<>(newGoogleTokenDto));
    }

    @Override
    public ResponseEntity<BaseResponse<TokenDto>> refreshJwtToken(TokenDto tokenDto) {
        log.info("[토큰 갱신] JWT 액세스 토큰 갱신 요청");
        TokenDto newTokenDto = tokenService.refreshAccessToken(tokenDto.getRefreshToken());
        return ResponseEntity.ok(new BaseResponse<>(newTokenDto));
    }

    @Override
    public ResponseEntity<BaseResponse<Void>> logout(CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        log.info("[로그아웃] 요청 - 사용자 ID: {}", memberId);
        tokenService.logout(memberId);
        return ResponseEntity.ok(new BaseResponse<>());
    }
}
