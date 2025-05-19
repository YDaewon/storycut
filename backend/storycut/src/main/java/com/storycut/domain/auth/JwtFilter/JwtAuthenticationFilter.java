package com.storycut.domain.auth.JwtFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storycut.domain.auth.util.JWTUtil;
import com.storycut.domain.auth.util.JWTUtil.TokenStatus;
import com.storycut.global.model.dto.BaseResponse;
import com.storycut.global.model.dto.BaseResponseStatus;
import com.storycut.global.model.enums.PublicEndpoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = jwtUtil.resolveToken(request);

        if (token != null) {
            TokenStatus tokenStatus = jwtUtil.checkToken(token);

            if (tokenStatus == TokenStatus.VALID) {
                // 유효한 토큰의 경우 인증 정보 설정
                Authentication authentication = jwtUtil.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else if (tokenStatus == TokenStatus.EXPIRED) {
                // 만료된 토큰에 대한 특별 처리
                log.debug("만료된 토큰: {}", token);

                // 401 Unauthorized 응답 설정
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");

                // 응답 바디 설정
                BaseResponse<Void> errorResponse = new BaseResponse<>(BaseResponseStatus.JWT_ACCESS_TOKEN_EXPIRED);
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return; // 필터 체인 진행하지 않음
            }
            // INVALID 상태는 별도 처리 안 함 (인증 없이 요청 진행)
        }

        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 현재 요청 URI
        String uri = request.getRequestURI();
        
        // 공개 URL 패턴 목록
        List<String> publicPaths = PublicEndpoint.getAll();
        
        // 공개 URL 패턴 중 하나라도 일치하면 필터를 적용하지 않음
        for (String pattern : publicPaths) {
            if (pattern.endsWith("/**")) {
                // 와일드카드 패턴 처리
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (uri.startsWith(prefix)) {
                    return true;
                }
            } else if (uri.equals(pattern)) {
                // 정확한 경로 매치
                return true;
            }
        }
        
        return false;
    }
}