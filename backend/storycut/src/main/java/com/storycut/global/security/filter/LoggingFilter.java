package com.storycut.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain)
        throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Swagger & Prometheus & mcp 요청은 로깅 제외
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith(
            "/actuator/prometheus") || uri.startsWith("/api/mcp/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // HTTP 메서드 + URI만 로그 출력
        log.info("{} {}", request.getMethod(), uri);

        filterChain.doFilter(request, response);
    }
}