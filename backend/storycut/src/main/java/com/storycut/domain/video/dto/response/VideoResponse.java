package com.storycut.domain.video.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.storycut.domain.video.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResponse {
    private Long videoId;
    private Long memberId;
    private String videoTitle;
    private String videoUrl;
    private String thumbnail;
    private Long originalVideoId;
    @JsonProperty("isBlur")
    private boolean isBlur;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonIgnore
    public boolean isBlur() {
        return isBlur;
    }
    
    public static VideoResponse from(Video video) {
        return VideoResponse.builder()
                .videoId(video.getId())
                .memberId(video.getMemberId())
                .videoTitle(video.getVideoTitle())
                .videoUrl(video.getVideoUrl())
                .thumbnail(video.getThumbnail())
                .originalVideoId(video.getOriginalVideoId())
                .isBlur(video.isBlur())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();
    }
}
