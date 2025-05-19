package com.storycut.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 관련 설정
 * 외부 API 요청을 위한 WebClient 빈 등록
 */
@Configuration
public class WebClientConfig {

    /**
     * 기본 WebClient 빈 생성
     * 구글 API 등 외부 서비스 호출에 사용
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .build();
    }
}
