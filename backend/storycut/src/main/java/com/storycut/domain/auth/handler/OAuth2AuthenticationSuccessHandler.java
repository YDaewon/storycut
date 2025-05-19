package com.storycut.domain.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storycut.domain.auth.model.dto.TokenDto;
import com.storycut.domain.auth.service.TokenService;
import com.storycut.domain.auth.util.JWTUtil;
import com.storycut.domain.member.model.entity.Member;
import com.storycut.domain.member.repository.MemberRepository;
import com.storycut.global.exception.BusinessException;
import com.storycut.global.model.dto.BaseResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final TokenService tokenService;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 구글 응답 데이터 전체 출력 (디버깅용)
        log.info("===== 구글 OAuth2 응답 데이터 시작 =====");
        oAuth2User.getAttributes().forEach((key, value) -> {
            log.info("키: {}, 값: {}, 타입: {}", key, value, value != null ? value.getClass().getName() : "null");
        });
        log.info("===== 구글 OAuth2 응답 데이터 끝 =====");

        // Google의 고유 식별자(sub) 추출
        String providerId = oAuth2User.getAttribute("sub");

        // providerId로 사용자 찾기 (이메일 대신 providerId로 조회)
        Member member = memberRepository.findByProviderId(providerId)
                .orElseThrow(() -> new BusinessException(BaseResponseStatus.USER_NOT_FOUND));
        
        Long memberId = member.getId();

        // 토큰 생성 - memberId 사용
        String accessToken = jwtUtil.createAccessToken(memberId);
        String refreshToken = jwtUtil.createRefreshToken(memberId);
        
        // 리프레시 토큰 저장
        tokenService.saveRefreshToken(memberId, refreshToken);
        
        // 모바일 앱으로 리다이렉트 또는 토큰 응답
        String targetUrl = UriComponentsBuilder.fromUriString("stroycut://oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();
                
        // JSON 응답으로 처리
        TokenDto tokenDto = TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(tokenDto));
        
        log.info("OAuth2 로그인 성공 - 사용자 ID: {}, providerId: {}", memberId, providerId);
    }
}