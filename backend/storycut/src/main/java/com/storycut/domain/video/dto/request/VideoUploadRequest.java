package com.storycut.domain.video.dto.request;

import com.storycut.domain.video.entity.Video;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadRequest {
    @NotBlank(message = "비디오 제목은 필수입니다.")
    @Size(max = 255, message = "비디오 제목은 255자 이하여야 합니다.")
    private String videoTitle;

    private Long originalVideoId;

    private Boolean isBlur;
    
    public Video toEntity(Long memberId) {
        return Video.builder()
                .memberId(memberId)
                .videoTitle(videoTitle)
                .originalVideoId(originalVideoId)
                .isBlur(isBlur)
                .build();
    }
}
