package com.storycut.domain.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유튜브 API 권한을 위한 인증 코드 요청 DTO
 * authCode: 구글 OAuth2 인증 동의 후 받은 Authorization Code
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthCodeRequest {
    private String authCode;  // 구글 OAuth2 인증 코드
    private String state;     // CSRF 방지를 위한 상태 토큰
}
