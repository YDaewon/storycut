package com.storycut.global.model.enums;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 인증이 필요 없는 공개 엔드포인트 정의
 */
@Getter
@AllArgsConstructor
public enum PublicEndpoint {
    // 인증 관련 경로
    AUTH_LOGIN("/auth/login"),       // 로그인 API
    AUTH_REFRESH("/auth/refresh"),   // 토큰 갱신 API
    AUTH_WEB_LOGIN("/auth/web/login"), // 웹 로그인 리다이렉트
    AUTH_TEST_LOGIN("/auth/test-login"), // 테스트 로그인
    AUTH_OAUTH2_CALLBACK("/auth/oauth2/callback"), // OAuth2 콜백

    // OAuth2 관련 경로
    OAUTH2("/oauth2/**"),
    OAUTH2_AUTHORIZATION("/oauth2/authorization/**"),
    LOGIN_OAUTH2_CODE("/login/oauth2/code/**"),

    // 멤버 공개 API
    MEMBER_PUBLIC("/api/v1/spring/member/public/**"),

    // 정적 리소스
    ROOT("/"),
    STATIC("/static/**"),
    CSS("/css/**"),
    JS("/js/**"),
    IMAGES("/images/**"),
    FAVICON("/favicon.ico"),

    // 오류 페이지
    ERROR("/error"),

    // Swagger/API 문서
    SWAGGER_UI("/swagger-ui/**"),
    API_DOCS("/v3/api-docs/**"),
    SWAGGER_RESOURCES("/swagger-resources/**"),
    WEBJARS("/webjars/**"),
    SWAGGER_HTML("/swagger-ui.html"),

    // 모니터링 및 헬스 체크
    ACTUATOR("/actuator/**"),
    HEALTH("/health");

    private final String url;

    public static List<String> getAll() {
        return List.of(values()).stream().map(PublicEndpoint::getUrl).toList();
    }
}