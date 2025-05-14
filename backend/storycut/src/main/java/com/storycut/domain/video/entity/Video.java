package com.storycut.domain.video.entity;

import com.storycut.global.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "video")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long id;
    
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "video_title", nullable = false)
    private String videoTitle;
    
    @Column(name = "video_url")
    private String videoUrl;
    
    @Column(name = "thumbnail")
    private String thumbnail;
    
    @Column(name = "original_video_id")
    private Long originalVideoId;

    @Column(name = "is_blur", nullable = false)
    private boolean isBlur;

    @Enumerated(EnumType.STRING)
    private UploadStatus uploadStatus;

    @Builder
    public Video(Long memberId, String videoTitle, Long originalVideoId, Boolean isBlur) {
        this.memberId = memberId;
        this.videoTitle = videoTitle;
        this.originalVideoId = originalVideoId;
        this.isBlur = isBlur;
        this.uploadStatus = UploadStatus.PROGRESS;
    }

    public void completeUpload(String videoUrl, String thumbnail) {
        this.videoUrl = videoUrl;
        this.thumbnail = thumbnail;
        this.uploadStatus = UploadStatus.COMPLETE;
    }
}
