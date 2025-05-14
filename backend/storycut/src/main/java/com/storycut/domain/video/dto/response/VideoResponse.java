package com.storycut.domain.video.dto.response;

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
    private boolean isBlur;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
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
