package com.storycut.domain.auth.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class YouTubeController {
    private final RestTemplate restTemplate = new RestTemplate();

    private final String CLIENT_ID = "540631555497-sfbspp2d11a6jjn69uuhp7vdk6b01hve.apps.googleusercontent.com";
    private final String CLIENT_SECRET = "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";
    private final String REDIRECT_URI = "com.ssafy.storycut:/oauth2callback";

    // [1] 클라이언트가 호출할 로그인 URL 생성
    @GetMapping("/auth-url")
    public ResponseEntity<String> getGoogleAuthUrl() {
        String url = UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", CLIENT_ID)
                .queryParam("redirect_uri", REDIRECT_URI)
                .queryParam("response_type", "code")
                .queryParam("scope", "https://www.googleapis.com/auth/youtube.upload")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent") // 항상 refresh_token 받도록
                .build().toString();

        log.info("생성된 YouTube 인증 URL: {}", url);
        return ResponseEntity.ok(url);
    }
}
