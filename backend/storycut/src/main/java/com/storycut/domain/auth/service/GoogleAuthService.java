package com.storycut.domain.auth.service;

import com.storycut.domain.auth.model.GoogleOAuth2UserInfo;
import com.storycut.domain.auth.model.OAuth2UserInfo;
import com.storycut.domain.auth.model.dto.TokenDto;
import com.storycut.domain.auth.model.dto.YouTubeAuthResponse;
import com.storycut.domain.auth.util.JWTUtil;
import com.storycut.domain.member.model.entity.Member;
import com.storycut.global.exception.BusinessException;
import com.storycut.global.model.dto.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 구글 인증 서비스 - 모바일 앱 구글 로그인 및 유튜브 권한 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final JWTUtil jwtUtil;
    private final TokenService tokenService;
    private final CustomOAuth2UserService oAuth2UserService;

    // 구글 API URL 상수
    private static final String GOOGLE_TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    
    // 유튜브 업로드 권한 스코프
    private static final String YOUTUBE_UPLOAD_SCOPE = "https://www.googleapis.com/auth/youtube.upload";
    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String GOOGLE_CLIENT_ID;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String GOOGLE_CLIENT_SECRET;
    
    @Value("${app.baseUrl}")
    private String BASE_URL;
    
    @Value("${app.dev-mode:false}")
    private boolean devMode;
    
    /**
     * 구글 ID 토큰으로 로그인 처리
     */
    @Transactional
    public TokenDto processGoogleLogin(String idToken) {
        try {
            // 개발 모드에서는 더미 사용자 정보 사용
            Map<String, Object> tokenInfo;
            if (devMode && "test".equals(idToken)) {
                tokenInfo = createDummyUserInfo();
            } else {
                // WebClient를 통해 구글 API 호출하여 ID 토큰 검증
                WebClient webClient = WebClient.builder().build();
                tokenInfo = webClient.get()
                    .uri(GOOGLE_TOKEN_INFO_URL + idToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
                    
                if (tokenInfo == null) {
                    throw new BusinessException(BaseResponseStatus.TOKEN_VERIFICATION_FAILED);
                }
            }
            
            // OAuth2UserInfo 객체 생성 및 회원 정보 저장/갱신
            OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(tokenInfo);
            Member member = oAuth2UserService.saveOrUpdateMember(userInfo);
            Long memberId = member.getId();
            
            // 서버 JWT 토큰 발급 및 저장
            String accessToken = jwtUtil.createAccessToken(memberId);
            String refreshToken = jwtUtil.createRefreshToken(memberId);
            tokenService.saveRefreshToken(memberId, refreshToken);
            
            // JWT 토큰 반환
            return TokenDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (Exception e) {
            log.error("구글 로그인 실패", e);
            throw new BusinessException(BaseResponseStatus.GOOGLE_LOGIN_ERROR);
        }
    }
    
    /**
     * 유튜브 권한 인증 URL 생성
     */
    @Transactional
    public YouTubeAuthResponse generateYouTubeAuthUrl(Long memberId) {
        // CSRF 방지를 위한 state 생성
        String state = UUID.randomUUID().toString();
        
        // 상태 저장
        tokenService.saveAuthState(state, memberId);
        
        // 인증 URL 생성 (PKCE 없이)
        String authUrl = GOOGLE_AUTH_URL + "?" +
                "client_id=" + GOOGLE_CLIENT_ID + "&" +
//                "redirect_uri=" + BASE_URL + "/api/auth/oauth2/callback" + "&" +
                "redirect_uri=" + BASE_URL + "/api/v1/spring/auth/oauth2/callback" + "&" +
                "response_type=code&" +
                "scope=openid%20email%20profile%20" + YOUTUBE_UPLOAD_SCOPE + "&" +
                "access_type=offline&" +
                "prompt=consent&" +
                "state=" + state;
        
        return YouTubeAuthResponse.builder()
                .authUrl(authUrl)
                .state(state)
                .build();
    }
    
    /**
     * OAuth2 인증 코드로 구글 토큰 교환
     */
    @Transactional
    public TokenDto exchangeAuthCodeForTokens(String code, Long memberId) {
        // 개발 모드에서는 더미 토큰 사용
        if (devMode) {
            String googleAccessToken = "dummy_google_access_token_" + System.currentTimeMillis();
            String googleRefreshToken = "dummy_google_refresh_token_" + System.currentTimeMillis();
            
            // 사용자 정보 업데이트
            Member member = tokenService.getMemberById(memberId);
            member.updateGoogleAccessToken(googleAccessToken);
            
            // 리프레시 토큰 저장 (암호화 없이)
            tokenService.saveGoogleRefreshToken(memberId, googleRefreshToken);
            
            return TokenDto.builder()
                    .googleAccessToken(googleAccessToken)
                    .build();
        }
        
        try {
            // Google Token API 호출을 위한 파라미터 준비
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", GOOGLE_CLIENT_ID);
            formData.add("client_secret", GOOGLE_CLIENT_SECRET);
            formData.add("code", code);
            formData.add("grant_type", "authorization_code");
            formData.add("redirect_uri", BASE_URL + "/api/v1/spring/auth/oauth2/callback");
            
            // WebClient를 사용하여 Google Token API 호출
            Map<String, Object> response = WebClient.builder().build()
                    .post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("access_token") || !response.containsKey("refresh_token")) {
                throw new BusinessException(BaseResponseStatus.TOKEN_GENERATION_FAILED);
            }
            
            // 토큰 추출
            String googleAccessToken = (String) response.get("access_token");
            String googleRefreshToken = (String) response.get("refresh_token");
            
            // 사용자 정보 업데이트
            Member member = tokenService.getMemberById(memberId);
            member.updateGoogleAccessToken(googleAccessToken);
            
            // 리프레시 토큰 저장 (암호화 없이)
            tokenService.saveGoogleRefreshToken(memberId, googleRefreshToken);
            
            return TokenDto.builder()
                    .googleAccessToken(googleAccessToken)
                    .build();
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("구글 인증 코드로 토큰 교환 중 오류 발생", e);
            throw new BusinessException(BaseResponseStatus.TOKEN_GENERATION_FAILED);
        }
    }
    
    /**
     * 구글 액세스 토큰 갱신
     */
    @Transactional
    public TokenDto refreshGoogleAccessToken(Long memberId) {
        // 구글 액세스 토큰 갱신
        String newGoogleAccessToken = tokenService.refreshGoogleAccessToken(memberId);
        
        return TokenDto.builder()
                .googleAccessToken(newGoogleAccessToken)
                .build();
    }
    
    /**
     * 사용자가 유튜브 권한을 가지고 있는지 확인
     */
    public boolean hasYouTubeAccess(Long memberId) {
        return tokenService.hasGoogleRefreshToken(memberId);
    }
    
    /**
     * 테스트용 더미 사용자 정보 생성
     */
    private Map<String, Object> createDummyUserInfo() {
        Map<String, Object> dummyInfo = new HashMap<>();
        dummyInfo.put("sub", "test_user_id_" + UUID.randomUUID().toString().substring(0, 8));
        dummyInfo.put("email", "test@example.com");
        dummyInfo.put("name", "테스트 사용자");
        dummyInfo.put("picture", "https://example.com/default_profile.jpg");
        dummyInfo.put("email_verified", true);
        return dummyInfo;
    }
    
    /**
     * 테스트용 간단 로그인 처리 (개발 환경 전용)
     */
    @Transactional
    public TokenDto processTestLogin() {
        if (!devMode) {
            throw new BusinessException(BaseResponseStatus.UNAUTHORIZED);
        }
        
        return processGoogleLogin("test");
    }
}