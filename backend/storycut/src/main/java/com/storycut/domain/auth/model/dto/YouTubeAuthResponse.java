package com.storycut.domain.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유튜브 권한 인증 URL 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YouTubeAuthResponse {
    private String authUrl;    // 유튜브 권한을 위한 구글 OAuth2 인증 URL
    private String state;      // CSRF 방지를 위한 상태 토큰
}
