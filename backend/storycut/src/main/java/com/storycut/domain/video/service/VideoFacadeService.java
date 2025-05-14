package com.storycut.domain.video.service;

import com.storycut.domain.video.dto.request.UploadComplate;
import com.storycut.domain.video.dto.request.VideoUploadRequest;
import com.storycut.domain.video.dto.response.VideoResponse;
import com.storycut.domain.video.entity.Video;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VideoFacadeService implements VideoService {
    
    private final VideoDetailService videoDetailService;
    
    @Override
    @Transactional
    public Long uploadVideo(Long memberId, VideoUploadRequest request) {
        // 요청 DTO를 엔티티로 변환하여 비디오 저장
        Video video = request.toEntity(memberId);
        Video savedVideo = videoDetailService.saveVideo(video);
        
        log.info("비디오가 업로드 시작. 비디오 ID: {}, 회원 ID: {}", savedVideo.getId(), memberId);
        
        // 응답 생성
        return savedVideo.getId();
    }

    @Override
    @Transactional
    public VideoResponse completeUpload(UploadComplate request){
        return videoDetailService.updateComplete(request);
    }
    
    @Override
    public VideoResponse getVideo(Long videoId) {
        // ID로 비디오 조회
        Video video = videoDetailService.findVideoById(videoId);
        
        // 응답 생성
        return videoDetailService.mapToResponse(video);
    }
    
    @Override
    public List<VideoResponse> getMemberVideos(Long memberId) {
        // 회원의 비디오 목록 조회
        List<Video> videos = videoDetailService.findVideosByMemberId(memberId);
        
        // 응답 생성
        return videos.stream()
                .map(videoDetailService::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<VideoResponse> getEditedVideos(Long originalVideoId) {
        // 원본 비디오 존재 확인
        videoDetailService.findVideoById(originalVideoId);
        
        // 편집된 비디오 목록 조회
        List<Video> videos = videoDetailService.findVideosByOriginalVideoId(originalVideoId);
        
        // 응답 생성
        return videos.stream()
                .map(videoDetailService::mapToResponse)
                .collect(Collectors.toList());
    }
}
