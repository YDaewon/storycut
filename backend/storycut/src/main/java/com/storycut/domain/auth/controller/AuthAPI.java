package com.storycut.domain.auth.controller;

import com.storycut.domain.auth.model.CustomUserDetails;
import com.storycut.domain.auth.model.dto.GoogleLoginRequest;
import com.storycut.domain.auth.model.dto.TokenDto;
import com.storycut.domain.auth.model.dto.YouTubeAuthResponse;
import com.storycut.global.model.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Tag(name = "Authentication", description = "로그인, 토큰 관리, 로그아웃 API")
@RequestMapping("/auth")
public interface AuthAPI {

    @Operation(summary = "구글 로그인 (모바일)", description = "모바일 앱에서 구글 ID 토큰으로 로그인합니다")
    @PostMapping("/login")
    ResponseEntity<BaseResponse<TokenDto>> googleLogin(@RequestBody GoogleLoginRequest request);

    @Operation(summary = "웹 로그인", description = "웹 환경에서 구글 로그인 페이지로 리다이렉트합니다")
    @GetMapping("/web/login")
    void googleWebLogin(HttpServletResponse response) throws Exception;

    @Operation(summary = "테스트 로그인", description = "개발 환경에서만 사용 가능한 테스트 로그인")
    @GetMapping("/test-login")
    ResponseEntity<BaseResponse<TokenDto>> testLogin();

    @Operation(summary = "OAuth2 콜백 처리", description = "구글 인증 후 리다이렉트되는 콜백을 처리합니다")
    @GetMapping("/oauth2/callback")
    RedirectView handleOAuth2Callback(@RequestParam("code") String code,
        @RequestParam("state") String state);

    @Operation(summary = "유튜브 권한 URL 생성", description = "유튜브 업로드 권한 획득을 위한 인증 URL을 생성합니다")
    @GetMapping("/youtube/auth")
    ResponseEntity<BaseResponse<YouTubeAuthResponse>> getYouTubeAuthUrl(@AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "유튜브 권한 확인", description = "사용자가 유튜브 업로드 권한을 가지고 있는지 확인합니다")
    @GetMapping("/youtube/status")
    ResponseEntity<BaseResponse<Boolean>> checkYouTubeAccess(@AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "구글 토큰 갱신", description = "만료된 구글 액세스 토큰을 갱신합니다")
    @PostMapping("/google-refresh")
    ResponseEntity<BaseResponse<TokenDto>> refreshGoogleToken(@AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "JWT 토큰 갱신", description = "만료된 JWT 액세스 토큰을 리프레시 토큰으로 갱신합니다")
    @PostMapping("/refresh")
    ResponseEntity<BaseResponse<TokenDto>> refreshJwtToken(@RequestBody TokenDto tokenDto);

    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리하고 토큰을 무효화합니다")
    @PostMapping("/logout")
    ResponseEntity<BaseResponse<Void>> logout(@AuthenticationPrincipal CustomUserDetails userDetails);
}
