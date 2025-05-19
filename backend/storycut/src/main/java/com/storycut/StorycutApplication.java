package com.storycut;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        title = "Storycut API",
        version = "1.0.0",
        description = "공유방 기반 영상 프로젝트 플랫폼 API 명세"
    ),
    servers = {
        @Server(url = "/api/v1/spring", description = "Spring Context Path")
    }
)
@SpringBootApplication
public class StorycutApplication {

	public static void main(String[] args) {
		SpringApplication.run(StorycutApplication.class, args);
	}

}
