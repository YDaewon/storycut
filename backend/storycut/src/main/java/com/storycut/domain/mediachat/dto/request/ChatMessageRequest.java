package com.storycut.domain.mediachat.dto.request;


import com.storycut.domain.mediachat.model.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    @Schema(description = "비디오 ID", example = "vid123456")
    @NotBlank(message = "비디오 ID는 필수입니다.")
    private String videoId;

    @Schema(description = "메시지 제목", example = "오늘의 명장면", required = true)
    @NotBlank(message = "메시지 제목은 필수입니다.")
    private String title;

    @Schema(description = "미디어 URL (비디오)", example = "https://s3.bucket.com/image.jpg")
    private String mediaUrl;

    @Schema(description = "썸네일 URL (S3)", example = "https://s3.amazonaws.com/bucket/thumbnails/example.jpg")
    @NotBlank(message = "썸네일 URL은 필수입니다.")
    private String thumbnailUrl;

    /**
     * 요청 DTO를 ChatMessage 엔티티로 변환합니다.
     *
     * @param roomId 메시지가 속한 Room ID
     * @param senderId 메시지 작성자 ID
     * @return 변환된 ChatMessage 엔티티
     */
    public ChatMessage toEntity(Long roomId, Long senderId) {
        return ChatMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .videoId(videoId)
                .title(title)
                .mediaUrl(mediaUrl)
                .thumbnailUrl(thumbnailUrl)
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }
}
