package com.storycut.global.config;

import com.storycut.global.model.enums.PublicEndpoint;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringdocConfig {

    @Value("${app.baseUrl}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        // 상대 경로를 사용하여 CORS 이슈 방지
        Server localServer = new Server();
        localServer.setUrl("/");
        localServer.setDescription("Local Server");

        // Security 요구사항 추가 (전역 설정)
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("JWT");

        return new OpenAPI()
            .servers(List.of(localServer))
            .info(new Info()
                .title("Storycut API")
                .version("1.0")
                .description("Storycut 서비스 API 명세서"))
            .components(new Components()
                .addSecuritySchemes("JWT", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization")
                    .description("JWT Access 토큰을 입력하세요. 예: eyJhbGciOiJ...")
                )
            )
            // 전역 보안 요구사항 추가
            .addSecurityItem(securityRequirement);
    }

//    @Bean
//    public OpenApiCustomizer customOpenApi() {
//        return openApi -> openApi.getComponents().getSchemas().keySet()
//            .removeIf(name -> name.startsWith("BaseResponse"));
//    }

    // OpenApiCustomiser를 이용해, 공개 API를 제외한 엔드포인트에 자동으로 보안 요구사항 추가
    @Bean
    public OpenApiCustomizer securityOpenApiCustomiser() {
        return openApi -> {
            List<String> publicUrls = PublicEndpoint.getAll();
            
            openApi.getPaths().forEach((path, pathItem) -> {
                // 공개 URL에 해당하는 경로는 보안 요구사항 추가하지 않음
                boolean isPublicPath = publicUrls.stream()
                    .anyMatch(url -> {
                        // /** 패턴 처리
                        String urlPattern = url.replace("/**", "");
                        return path.startsWith(urlPattern) || path.matches(urlPattern.replace("*", ".*"));
                    });
                
                if (!isPublicPath) {
                    pathItem.readOperations().forEach(operation -> {
                        operation.addSecurityItem(new SecurityRequirement().addList("JWT"));
                    });
                }
            });
        };
    }
}