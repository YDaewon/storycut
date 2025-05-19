package com.storycut.global.exception;

import com.storycut.global.model.dto.BaseResponse;
import com.storycut.global.model.dto.BaseResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;  // MDC import 추가
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(0)
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<BaseResponseStatus>> handleBusinessException(
        BusinessException e, HttpServletRequest req) {
        try {
            // MDC에 구조화된 정보 추가
            MDC.put("exception_type", "BusinessException");
            MDC.put("uri", req.getRequestURI());
            MDC.put("http_status", e.getBaseResponseStatus().getHttpStatus().toString());
            MDC.put("error_code", String.valueOf(e.getBaseResponseStatus().getCode()));
            MDC.put("error_message", e.getMessage());

            // 사용자 정보가 있다면 추가 (예: 인증된 사용자 ID)
            String userId = req.getHeader("X-User-ID"); // 또는 시큐리티 컨텍스트에서 가져온 정보
            if (userId != null && !userId.isEmpty()) {
                MDC.put("user_id", userId);
            }

            String logMessage = String.format("""
                ⚠️ [BusinessException 발생]
                📍 URI: %s
                ❗ 예외 메시지: %s
                🔑 파라미터:
                %s
                """, req.getRequestURI(), e.getMessage(), getParams(req));

            LOG.error("\n{}", logMessage);

            return ResponseEntity.status(e.getBaseResponseStatus().getHttpStatus())
                .body(new BaseResponse<>(e.getBaseResponseStatus()));
        } finally {
            // 메서드 종료 시 MDC 정리 (메모리 누수 방지)
            MDC.clear();
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<BaseResponseStatus>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException e, HttpServletRequest req) {
        try {
            // MDC에 구조화된 정보 추가
            MDC.put("exception_type", "HttpMessageNotReadableException");
            MDC.put("uri", req.getRequestURI());
            MDC.put("http_status", HttpStatus.BAD_REQUEST.toString());
            MDC.put("error_code", String.valueOf(BaseResponseStatus.BAD_REQUEST.getCode()));
            MDC.put("error_message", e.getMessage());

            String logMessage = String.format("""
                ⚠️ [NotReadableException 발생]
                📍 URI: %s
                ❗ 예외 메시지: %s
                🔑 파라미터:
                %s
                """, req.getRequestURI(), e.getMessage(), getParams(req));

            LOG.error("\n{}", logMessage);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BaseResponse<>(BaseResponseStatus.BAD_REQUEST));
        } finally {
            MDC.clear();
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e, HttpServletRequest req) {
        try {
            // MDC에 구조화된 정보 추가
            MDC.put("exception_type", e.getClass().getSimpleName());
            MDC.put("uri", req.getRequestURI());
            MDC.put("http_status", HttpStatus.INTERNAL_SERVER_ERROR.toString());
            MDC.put("error_message", e.getMessage());

            // 예외 스택 트레이스 정보 (축약된 버전)
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace.length > 0) {
                MDC.put("exception_location", stackTrace[0].toString());
            }

            String logMessage = String.format("""
                ___________________ ⚠️ [Spring Exception 발생] ________________________
                📨 URI: %s
                ❗ 예외 메시지: %s
                ✔️ 파라미터: %s
                ___________________ ⚠️ [Exception 종료] ________________________
                """, req.getRequestURI(), e.getMessage(), getParams(req));

            LOG.error("\n{}", logMessage, e);  // 예외 객체도 함께 전달하여 스택 트레이스 로깅

            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            MDC.clear();
        }
    }

    private String getParams(HttpServletRequest req) {
        StringBuilder params = new StringBuilder();
        Enumeration<String> keys = req.getParameterNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            params.append("- ").append(key).append(": ").append(req.getParameter(key)).append("\n");
        }
        // 파라미터 정보도 MDC에 추가 (선택 사항)
        if (params.length() > 0) {
            MDC.put("request_params", params.toString());
        }
        return params.toString();
    }
}