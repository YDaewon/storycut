# Run Stage (경량 실행 이미지)
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리 생성
WORKDIR /app

# 로그 디렉토리 설정
RUN mkdir -p /var/log/springboot && chmod -R 755 /var/log/springboot

# Jenkins에서 Gradle 빌드 후 생성된 JAR 파일 복사
COPY build/libs/*.jar app.jar

# application-secret.yml 복사
COPY src/main/resources/application-secret.yml /app/application-secret.yml

# 포트 노출
EXPOSE 8080

# 실행 명령어 (프로필 및 타임존 설정 포함)
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod,secret", "-Dspring.config.additional-location=/app/", "-Duser.timezone=Asia/Seoul", "app.jar"]