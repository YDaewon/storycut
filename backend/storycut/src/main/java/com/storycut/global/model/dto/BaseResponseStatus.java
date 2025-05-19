package com.storycut.global.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BaseResponseStatus {
    // 성공 코드 (2xx)
    SUCCESS(true, HttpStatus.OK, 200, "요청에 성공하였습니다."),
    AUTHORIZATION_SUCCESS(true, HttpStatus.OK, 200, "토큰 발급에 성공하였습니다."),

    // 클라이언트 오류 코드 (4xx)
    BAD_REQUEST(false, HttpStatus.BAD_REQUEST, 400, "입력값을 확인해주세요."),
    UNAUTHORIZED(false, HttpStatus.UNAUTHORIZED, 401, "인증이 필요합니다."),
    FORBIDDEN(false, HttpStatus.FORBIDDEN, 403, "권한이 없습니다."),
    NOT_FOUND(false, HttpStatus.NOT_FOUND, 404, "대상을 찾을 수 없습니다."),

    // 서버 오류 코드 (5xx)
    INTERNAL_SERVER_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 내부 오류가 발생했습니다."),

    // JWT 토큰 관련 오류 코드 (1001~1099)
    INVALID_JWT_TOKEN(false, HttpStatus.UNAUTHORIZED, 1001, "유효하지 않은 자체 JWT 토큰입니다."),
    JWT_ACCESS_TOKEN_EXPIRED(false, HttpStatus.UNAUTHORIZED, 1002, "자체 액세스 토큰이 만료되었습니다."),
    JWT_REFRESH_TOKEN_EXPIRED(false, HttpStatus.UNAUTHORIZED, 1003, "자체 리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(false, HttpStatus.UNAUTHORIZED, 1004, "유효하지 않은 리프레시 토큰입니다."),
    TOKEN_GENERATION_FAILED(false, HttpStatus.INTERNAL_SERVER_ERROR, 1005, "토큰 생성에 실패했습니다."),
    TOKEN_VERIFICATION_FAILED(false, HttpStatus.UNAUTHORIZED, 1006, "토큰 검증에 실패했습니다."),
    
    // 구글/소셜 로그인 관련 오류 코드 (1101~1199)
    GOOGLE_LOGIN_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, 1101, "구글 로그인 처리 중 오류가 발생했습니다."),
    GOOGLE_ACCESS_TOKEN_EXPIRED(false, HttpStatus.UNAUTHORIZED, 1102, "구글 액세스 토큰이 만료되었습니다."),
    GOOGLE_REFRESH_TOKEN_NOT_FOUND(false, HttpStatus.NOT_FOUND, 1103, "구글 리프레시 토큰을 찾을 수 없습니다."),
    GOOGLE_TOKEN_REFRESH_FAILED(false, HttpStatus.INTERNAL_SERVER_ERROR, 1104, "구글 토큰 갱신에 실패했습니다."),
    GOOGLE_REFRESH_TOKEN_EXPIRED(false, HttpStatus.UNAUTHORIZED, 1105, "구글 리프레시 토큰이 만료되었습니다."),
    
    // 사용자 관련 오류 코드 (1201~1299)
    USER_NOT_FOUND(false, HttpStatus.NOT_FOUND, 1201, "사용자를 찾을 수 없습니다."),

    // 2000~ 2999 : 방 관련 에러
    NOT_FOUND_ROOM(false, HttpStatus.NOT_FOUND, 2000, "해당 방이 존재하지 않습니다."),
    NOT_VALID_HOST(false, HttpStatus.FORBIDDEN, 2001, "방의 호스트가 아니거나 이미 없는 방입니다."),
    ALREADY_MEMBER_ROOM(false, HttpStatus.BAD_REQUEST, 2002, "이미 방에 참여 중입니다."),
    NOT_VALID_PASSWORD(false, HttpStatus.BAD_REQUEST, 2003, "비밀번호가 일치하지 않습니다."),

    INVALID_INVITE_CODE(false, HttpStatus.BAD_REQUEST, 2004, "유효하지 않거나 만료된 초대코드입니다."),
    LENGTH_INVITE_CODE(false, HttpStatus.BAD_REQUEST, 2005, "초대코드는 6자리여야 합니다."),

    // 3000~ 3999 : 비디오 관련 에러
    NOT_FOUND_VIDEO(false, HttpStatus.NOT_FOUND, 3000, "해당 비디오가 존재하지 않습니다."),
    NOT_VALID_VIDEO(false, HttpStatus.BAD_REQUEST, 3001, "비디오가 유효하지 않습니다."),

    // 4000~ 4999 : 유튜브 API 관련 에러
    YOUTUBE_API_ERROR(false, HttpStatus.INTERNAL_SERVER_ERROR, 4000, "유튜브 API 호출 중 오류가 발생했습니다."),
    YOUTUBE_UPLOAD_FAILED(false, HttpStatus.INTERNAL_SERVER_ERROR, 4001, "유튜브 업로드에 실패했습니다."),
    YOUTUBE_ACCESS_DENIED(false, HttpStatus.FORBIDDEN, 4002, "유튜브 권한이 거부되었습니다."),

    // 5000~ 5999 : 메시지 관련 에러
    NOT_FOUND_MESSAGE(false, HttpStatus.NOT_FOUND, 5000, "해당 메시지를 찾을 수 없습니다."),
    NOT_VALID_MESSAGE(false, HttpStatus.BAD_REQUEST, 5001, "메시지가 유효하지 않습니다."),
    UNAUTHORIZED_MESSAGE(false, HttpStatus.FORBIDDEN, 5002, "메시지에 대한 권한이 없습니다."),


    ;

    private final boolean isSuccess;
    @JsonIgnore
    private final HttpStatus httpStatus;
    private final int code;
    private final String message;

    BaseResponseStatus(boolean isSuccess, HttpStatus httpStatus, int code, String message) {
        this.isSuccess = isSuccess;
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}