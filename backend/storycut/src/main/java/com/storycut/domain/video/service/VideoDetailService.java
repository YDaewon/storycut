package com.storycut.domain.video.service;

import com.storycut.domain.video.dto.request.UploadComplate;
import com.storycut.domain.video.dto.response.VideoResponse;
import com.storycut.domain.video.entity.Video;
import com.storycut.domain.video.repository.VideoRepository;
import com.storycut.global.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.storycut.global.model.dto.BaseResponseStatus.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoDetailService {
    
    private final VideoRepository videoRepository;

    public Video findVideoById(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_VIDEO));
    }

    public List<Video> findVideosByMemberId(Long memberId, Boolean isOriginal) {
        return videoRepository.findByMemberIdAndIsOriginal(memberId, isOriginal);
    }

    public List<Video> findVideosByOriginalVideoId(Long originalVideoId) {
        return videoRepository.findByOriginalVideoId(originalVideoId);
    }

    public VideoResponse updateComplete(UploadComplate request) {
        Video video = findVideoById(request.getVideoId());
        video.completeUpload(request.getVideoUrl(), request.getThumbnail());
        return mapToResponse(video);
    }

    @Transactional
    public Video saveVideo(Video video) {
        return videoRepository.save(video);
    }

    public VideoResponse mapToResponse(Video video) {
        return VideoResponse.from(video);
    }
}
