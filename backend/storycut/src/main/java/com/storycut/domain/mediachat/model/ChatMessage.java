package com.storycut.domain.mediachat.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 채팅 메시지 MongoDB 모델
 * <p>
 * Room ID를 참조하여 채팅 로그를 저장하는 MongoDB 문서 모델입니다.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    @Indexed
    private Long roomId;

    private Long senderId;

    private String videoId;

    private String title;

    private String mediaUrl;

    private String thumbnailUrl;
    
    private LocalDateTime createdAt;
}
