package com.storycut.domain.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {
    // StoryCut 서버 JWT 토큰
    private String accessToken;
    private String refreshToken;
    
    // 구글 API 사용을 위한 토큰 (필요한 경우에만 포함)
    private String googleAccessToken;
}
