package com.storycut.domain.mediachat.controller;

import com.storycut.domain.auth.model.CustomUserDetails;
import com.storycut.domain.mediachat.dto.response.ChatMessageResponse;
import com.storycut.domain.mediachat.service.ChatService;
import com.storycut.domain.mediachat.dto.request.ChatMessageRequest;
import com.storycut.global.model.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 채팅 메시지 컨트롤러
 */
@RestController
@RequiredArgsConstructor
public class ChatController implements ChatAPI {

    private final ChatService chatService;

    @Override
    public ResponseEntity<BaseResponse<ChatMessageResponse>> sendVideoMessage(
            CustomUserDetails authUser,
            Long roomId,
            ChatMessageRequest request) {

        ChatMessageResponse response = chatService.saveVideoMessage(roomId, authUser.getMemberId(), request);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }

    /**
     * 채팅 메시지 목록 조회 API (페이징)
     */
    @Override
    public ResponseEntity<BaseResponse<List<ChatMessageResponse>>> getChatMessages(
            Long roomId,
            int page,
            int size) {

        List<ChatMessageResponse> response = chatService.getChatMessages(roomId, page, size);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }

    /**
     * 단일 채팅 메시지 조회 API
     */
    @Override
    public ResponseEntity<BaseResponse<ChatMessageResponse>> getMessage(String chatId) {
        ChatMessageResponse response = chatService.getChatMessage(chatId);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }

    /**
     * 단일 채팅 메시지 삭제 API
     */
    @Override
    public ResponseEntity<BaseResponse<Boolean>> deleteMessage(
            CustomUserDetails authUser,
            String chatId) {

        // 메시지 삭제 권한 체크 및 삭제 (방장 또는 작성자)
        boolean deleted = chatService.deleteMessage(chatId, authUser.getMemberId());
        return ResponseEntity.ok(new BaseResponse<>(deleted));
    }
}
