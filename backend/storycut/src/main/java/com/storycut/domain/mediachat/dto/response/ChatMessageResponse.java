package com.storycut.domain.mediachat.dto.response;

import com.storycut.domain.mediachat.model.ChatMessage;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 채팅 메시지 응답 DTO
 */
@Getter
@Builder
public class ChatMessageResponse {

    private String id;
    private Long roomId;
    private Long senderId;
    // 비디오 관련 필드 추가
    private String videoId;
    private String title;
    private String mediaUrl;
    private String thumbnailUrl;
    private LocalDateTime createdAt;

    /**
     * ChatMessage 엔티티를 응답 DTO로 변환합니다.
     *
     * @param chatMessage 채팅 메시지 엔티티
     * @return 채팅 메시지 응답 DTO
     */
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .roomId(chatMessage.getRoomId())
                .senderId(chatMessage.getSenderId())
                .videoId(chatMessage.getVideoId())
                .title(chatMessage.getTitle())
                .mediaUrl(chatMessage.getMediaUrl())
                .thumbnailUrl(chatMessage.getThumbnailUrl())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
