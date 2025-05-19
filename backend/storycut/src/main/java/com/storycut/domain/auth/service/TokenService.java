package com.storycut.domain.auth.service;

import com.storycut.domain.auth.model.dto.TokenDto;
import com.storycut.domain.auth.util.JWTUtil;
import com.storycut.domain.member.model.entity.Member;
import com.storycut.domain.member.repository.MemberRepository;
import com.storycut.global.exception.BusinessException;
import com.storycut.global.model.dto.BaseResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 토큰 관리 서비스 - JWT 토큰 및 구글 토큰 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JWTUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final HttpServletRequest request;
    
    // Redis 키 접두사 상수
    private static final String REFRESH_TOKEN_PREFIX = "RT:";
    private static final String GOOGLE_REFRESH_TOKEN_PREFIX = "G_RT:";
    private static final String AUTH_STATE_PREFIX = "STATE:";
    private static final String TOKEN_BLACKLIST_PREFIX = "BL:";
    
    // 토큰 만료 시간 상수
    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidityMs;
    
    // 리프레시 토큰 만료 시간 (초 단위로 변환)
    private long getRefreshTokenExpireTime() {
        return refreshTokenValidityMs / 1000; // 밀리초를 초로 변환
    }
    
    private static final long AUTH_STATE_EXPIRE_TIME = 10 * 60; // 10분 (초 단위)
    
    // Google API 상수
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String GOOGLE_CLIENT_ID;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String GOOGLE_CLIENT_SECRET;
    
    @Value("${app.dev-mode:false}")
    private boolean devMode;

    /**
     * 리프레시 토큰 저장
     */
    @Transactional
    public void saveRefreshToken(Long memberId, String refreshToken) {
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + memberId,
                refreshToken,
                getRefreshTokenExpireTime(),
                TimeUnit.SECONDS
        );
    }
    
    /**
     * 구글 리프레시 토큰 저장 (암호화 없이)
     */
    @Transactional
    public void saveGoogleRefreshToken(Long memberId, String googleRefreshToken) {
        redisTemplate.opsForValue().set(GOOGLE_REFRESH_TOKEN_PREFIX + memberId, googleRefreshToken);
    }
    

    
    /**
     * 인증 상태 저장 (CSRF 방지)
     */
    @Transactional
    public void saveAuthState(String state, Long memberId) {
        redisTemplate.opsForValue().set(
                AUTH_STATE_PREFIX + state,
                String.valueOf(memberId),
                AUTH_STATE_EXPIRE_TIME,
                TimeUnit.SECONDS
        );
    }
    
    /**
     * 인증 상태 검증 및 사용자 ID 반환
     */
    @Transactional(readOnly = true)
    public Long validateAuthState(String state) {
        String memberIdStr = redisTemplate.opsForValue().get(AUTH_STATE_PREFIX + state);
        return memberIdStr != null ? Long.valueOf(memberIdStr) : null;
    }
    
    /**
     * 인증 상태 토큰 삭제
     */
    @Transactional
    public void deleteAuthState(String state) {
        if (state != null) {
            redisTemplate.delete(AUTH_STATE_PREFIX + state);
        }
    }
    

    
    /**
     * 구글 리프레시 토큰 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasGoogleRefreshToken(Long memberId) {
        return redisTemplate.hasKey(GOOGLE_REFRESH_TOKEN_PREFIX + memberId);
    }

    /**
     * JWT 액세스 토큰 갱신
     */
    @Transactional
    public TokenDto refreshAccessToken(String refreshToken) {
        // 리프레시 토큰 상태 확인
        JWTUtil.TokenStatus tokenStatus = jwtUtil.checkToken(refreshToken);
        
        // 토큰 상태에 따른 처리
        if (tokenStatus == JWTUtil.TokenStatus.EXPIRED) {
            throw new BusinessException(BaseResponseStatus.JWT_REFRESH_TOKEN_EXPIRED);
        } else if (tokenStatus != JWTUtil.TokenStatus.VALID) {
            throw new BusinessException(BaseResponseStatus.INVALID_JWT_TOKEN);
        }

        // 유효한 토큰에서 멤버 ID 추출
        Long memberId = jwtUtil.getMemberId(refreshToken);

        // 저장된 리프레시 토큰과 일치하는지 확인
        String savedRefreshToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + memberId);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(BaseResponseStatus.REFRESH_TOKEN_INVALID);
        }

        // 새 액세스 토큰 발급
        String newAccessToken = jwtUtil.createAccessToken(memberId);
        
        return TokenDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // 기존 리프레시 토큰 유지
                .build();
    }
    
    /**
     * 구글 액세스 토큰 갱신
     */
    @Transactional
    public String refreshGoogleAccessToken(Long memberId) {
        // 개발 모드에서는 더미 데이터 반환
        if (devMode) {
            String dummyToken = "dummy_google_token_" + System.currentTimeMillis();
            
            // 멤버 엔티티 업데이트
            Member member = getMemberById(memberId);
            member.updateGoogleAccessToken(dummyToken);
            memberRepository.save(member);
            
            return dummyToken;
        }

        // Redis에서 구글 리프레시 토큰 가져오기
        String googleRefreshToken = redisTemplate.opsForValue().get(GOOGLE_REFRESH_TOKEN_PREFIX + memberId);
        
        if (googleRefreshToken == null || googleRefreshToken.isEmpty()) {
            throw new BusinessException(BaseResponseStatus.GOOGLE_REFRESH_TOKEN_NOT_FOUND);
        }
        
        try {
            
            // Google Token API 호출을 위한 파라미터 준비
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", GOOGLE_CLIENT_ID);
            formData.add("client_secret", GOOGLE_CLIENT_SECRET);
            formData.add("refresh_token", googleRefreshToken);
            formData.add("grant_type", "refresh_token");
            
            // WebClient를 사용하여 Google Token API 호출
            WebClient webClient = WebClient.builder().build();
            Map<String, Object> response = webClient.post()
                    .uri(GOOGLE_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response == null || !response.containsKey("access_token")) {
                throw new BusinessException(BaseResponseStatus.GOOGLE_TOKEN_REFRESH_FAILED);
            }

            String newGoogleAccessToken = (String) response.get("access_token");

            // 새 액세스 토큰을 Member 엔티티에 저장
            Member member = getMemberById(memberId);
            member.updateGoogleAccessToken(newGoogleAccessToken);
            memberRepository.save(member);

            return newGoogleAccessToken;
        } catch (WebClientResponseException e) {
            // Google API 응답 오류 처리
            log.error("구글 토큰 갱신 API 오류: {}", e.getMessage());

            if (e.getStatusCode().is4xxClientError()) {
                // 401 또는 400 오류가 발생하면 리프레시 토큰이 유효하지 않거나 만료된 것으로 판단
                if (e.getStatusCode().value() == 400 && e.getResponseBodyAsString().contains("invalid_grant")) {
                    // 'invalid_grant' 오류는 보통 리프레시 토큰이 만료되었거나 취소되었음을 의미
                    throw new BusinessException(BaseResponseStatus.GOOGLE_REFRESH_TOKEN_EXPIRED);
                }
                // 그 외 클라이언트 오류는 액세스 토큰 만료로 처리
                throw new BusinessException(BaseResponseStatus.GOOGLE_ACCESS_TOKEN_EXPIRED);
            }
            // 5xx 등 서버 오류는 일반 갱신 실패로 처리
            throw new BusinessException(BaseResponseStatus.GOOGLE_TOKEN_REFRESH_FAILED);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("구글 액세스 토큰 갱신 중 오류 발생", e);
            throw new BusinessException(BaseResponseStatus.GOOGLE_TOKEN_REFRESH_FAILED);
        }
    }

    /**
     * 로그아웃 처리
     */
    @Transactional
    public void logout(Long memberId) {
        // Redis에서 리프레시 토큰 삭제
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + memberId);
        
        // 현재 요청에서 토큰 추출
        String accessToken = jwtUtil.resolveToken(request);
        
        if (accessToken != null) {
            JWTUtil.TokenStatus tokenStatus = jwtUtil.checkToken(accessToken);
            
            // 액세스 토큰 블랙리스트에 추가 (남은 유효 시간동안)
            if (tokenStatus == JWTUtil.TokenStatus.VALID) {
                long expiration = jwtUtil.getExpirationTime(accessToken) - System.currentTimeMillis();
                if (expiration > 0) {
                    redisTemplate.opsForValue().set(
                            TOKEN_BLACKLIST_PREFIX + accessToken,
                            "logout",
                            expiration,
                            TimeUnit.MILLISECONDS
                    );
                    log.info("액세스 토큰 블랙리스트 추가 - 사용자 ID: {}, 만료까지 남은 시간: {}ms", memberId, expiration);
                }
            }
        }
        
        log.info("사용자 로그아웃 처리 완료 - ID: {}", memberId);
    }

    /**
     * ID로 회원 조회
     */
    @Transactional(readOnly = true)
    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BaseResponseStatus.USER_NOT_FOUND));
    }
}