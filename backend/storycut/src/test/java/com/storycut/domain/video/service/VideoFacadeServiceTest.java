package com.storycut.domain.video.service;

import com.storycut.domain.video.dto.request.VideoUploadRequest;
import com.storycut.domain.video.dto.response.VideoResponse;
import com.storycut.domain.video.entity.Video;
import com.storycut.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoFacadeServiceTest {

    @Mock
    private VideoDetailService videoDetailService;

    @InjectMocks
    private VideoFacadeService videoFacadeService;

    private Long memberId;
    private Long videoId;
    private Long originalVideoId;
    private Video mockVideo;
    private VideoResponse mockVideoResponse;
    private VideoUploadRequest mockUploadRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        memberId = 1L;
        videoId = 100L;
        originalVideoId = 99L;
        
        // Video 엔티티 초기화 (실제 엔티티 구조에 맞춤)
        mockVideo = Video.builder()
                .memberId(memberId)
                .videoName("테스트 비디오")
                .videoUrl("https://example.com/video.mp4")
                .thumbnail("https://example.com/thumbnail.jpg")
                .originalVideoId(originalVideoId)
                .isBlur(false)
                .build();
        
        // Reflection을 통해 ID 설정 (ID는 생성자에서 설정할 수 없음)
        try {
            java.lang.reflect.Field idField = Video.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(mockVideo, videoId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // VideoResponse 객체 초기화 (실제 DTO 구조에 맞춤)
        mockVideoResponse = VideoResponse.builder()
                .videoId(videoId)
                .memberId(memberId)
                .videoName("테스트 비디오")
                .videoUrl("https://example.com/video.mp4")
                .thumbnail("https://example.com/thumbnail.jpg")
                .originalVideoId(originalVideoId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        // VideoUploadRequest 객체 초기화 (실제 DTO 구조에 맞춤)
        mockUploadRequest = new VideoUploadRequest(
                "테스트 비디오",
                "https://example.com/video.mp4",
                "https://example.com/thumbnail.jpg",
                originalVideoId,
                false
        );
    }

    @Test
    @DisplayName("비디오 업로드 성공 테스트")
    void uploadVideo_Success() {
        // Given
        when(videoDetailService.saveVideo(any(Video.class))).thenReturn(mockVideo);
        when(videoDetailService.mapToResponse(any(Video.class))).thenReturn(mockVideoResponse);
        
        // When
        VideoResponse result = videoFacadeService.uploadVideo(memberId, mockUploadRequest);
        
        // Then
        assertNotNull(result);
        assertEquals(videoId, result.getVideoId());
        assertEquals(memberId, result.getMemberId());
        assertEquals("테스트 비디오", result.getVideoName());
        verify(videoDetailService, times(1)).saveVideo(any(Video.class));
        verify(videoDetailService, times(1)).mapToResponse(any(Video.class));
    }
    
    @Test
    @DisplayName("비디오 정보 조회 성공 테스트")
    void getVideo_Success() {
        // Given
        when(videoDetailService.findVideoById(videoId)).thenReturn(mockVideo);
        when(videoDetailService.mapToResponse(mockVideo)).thenReturn(mockVideoResponse);
        
        // When
        VideoResponse result = videoFacadeService.getVideo(videoId);
        
        // Then
        assertNotNull(result);
        assertEquals(videoId, result.getVideoId());
        assertEquals("테스트 비디오", result.getVideoName());
        verify(videoDetailService, times(1)).findVideoById(videoId);
        verify(videoDetailService, times(1)).mapToResponse(mockVideo);
    }
    
    @Test
    @DisplayName("회원의 비디오 목록 조회 성공 테스트")
    void getMemberVideos_Success() {
        // Given
        List<Video> videos = Collections.singletonList(mockVideo);
        when(videoDetailService.findVideosByMemberId(memberId)).thenReturn(videos);
        when(videoDetailService.mapToResponse(mockVideo)).thenReturn(mockVideoResponse);
        
        // When
        List<VideoResponse> result = videoFacadeService.getMemberVideos(memberId);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(videoId, result.get(0).getVideoId());
        verify(videoDetailService, times(1)).findVideosByMemberId(memberId);
        verify(videoDetailService, times(1)).mapToResponse(mockVideo);
    }
    
    @Test
    @DisplayName("원본 비디오 기반 편집 비디오 목록 조회 성공 테스트")
    void getEditedVideos_Success() {
        // Given
        Video editedVideo1 = Video.builder()
                .memberId(memberId)
                .videoName("편집 비디오 1")
                .videoUrl("https://example.com/edited1.mp4")
                .thumbnail("https://example.com/thumbnail1.jpg")
                .originalVideoId(originalVideoId)
                .isBlur(false)
                .build();
        
        Video editedVideo2 = Video.builder()
                .memberId(memberId)
                .videoName("편집 비디오 2")
                .videoUrl("https://example.com/edited2.mp4")
                .thumbnail("https://example.com/thumbnail2.jpg")
                .originalVideoId(originalVideoId)
                .isBlur(true)
                .build();
        
        // Reflection을 통해 ID 설정
        try {
            java.lang.reflect.Field idField = Video.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(editedVideo1, 101L);
            idField.set(editedVideo2, 102L);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        List<Video> editedVideos = Arrays.asList(editedVideo1, editedVideo2);
        
        VideoResponse editedResponse1 = VideoResponse.builder()
                .videoId(101L)
                .memberId(memberId)
                .videoName("편집 비디오 1")
                .videoUrl("https://example.com/edited1.mp4")
                .thumbnail("https://example.com/thumbnail1.jpg")
                .originalVideoId(originalVideoId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        VideoResponse editedResponse2 = VideoResponse.builder()
                .videoId(102L)
                .memberId(memberId)
                .videoName("편집 비디오 2")
                .videoUrl("https://example.com/edited2.mp4")
                .thumbnail("https://example.com/thumbnail2.jpg")
                .originalVideoId(originalVideoId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        when(videoDetailService.findVideoById(originalVideoId)).thenReturn(mockVideo);
        when(videoDetailService.findVideosByOriginalVideoId(originalVideoId)).thenReturn(editedVideos);
        when(videoDetailService.mapToResponse(editedVideo1)).thenReturn(editedResponse1);
        when(videoDetailService.mapToResponse(editedVideo2)).thenReturn(editedResponse2);
        
        // When
        List<VideoResponse> result = videoFacadeService.getEditedVideos(originalVideoId);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("편집 비디오 1", result.get(0).getVideoName());
        assertEquals("편집 비디오 2", result.get(1).getVideoName());
        verify(videoDetailService, times(1)).findVideoById(originalVideoId);
        verify(videoDetailService, times(1)).findVideosByOriginalVideoId(originalVideoId);
        verify(videoDetailService, times(1)).mapToResponse(editedVideo1);
        verify(videoDetailService, times(1)).mapToResponse(editedVideo2);
    }
    
    @Test
    @DisplayName("존재하지 않는 비디오 조회 실패 테스트")
    void getVideo_NonExistent_ThrowsException() {
        // Given
        Long nonExistentVideoId = 999L;
        when(videoDetailService.findVideoById(nonExistentVideoId)).thenThrow(BusinessException.class);
        
        // When & Then
        assertThrows(BusinessException.class, () -> videoFacadeService.getVideo(nonExistentVideoId));
        verify(videoDetailService, times(1)).findVideoById(nonExistentVideoId);
        verify(videoDetailService, never()).mapToResponse(any(Video.class));
    }
    
    @Test
    @DisplayName("존재하지 않는 원본 비디오로 편집 비디오 조회 실패 테스트")
    void getEditedVideos_NonExistentOriginal_ThrowsException() {
        // Given
        Long nonExistentVideoId = 999L;
        when(videoDetailService.findVideoById(nonExistentVideoId)).thenThrow(BusinessException.class);
        
        // When & Then
        assertThrows(BusinessException.class, () -> videoFacadeService.getEditedVideos(nonExistentVideoId));
        verify(videoDetailService, times(1)).findVideoById(nonExistentVideoId);
        verify(videoDetailService, never()).findVideosByOriginalVideoId(anyLong());
    }
    
    @Test
    @DisplayName("회원이 비디오가 없을 때 빈 목록 반환 테스트")
    void getMemberVideos_NoVideos_ReturnsEmptyList() {
        // Given
        when(videoDetailService.findVideosByMemberId(memberId)).thenReturn(Collections.emptyList());
        
        // When
        List<VideoResponse> result = videoFacadeService.getMemberVideos(memberId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(videoDetailService, times(1)).findVideosByMemberId(memberId);
        verify(videoDetailService, never()).mapToResponse(any(Video.class));
    }
    
    @Test
    @DisplayName("원본 비디오에 기반한 편집 비디오가 없을 때 빈 목록 반환 테스트")
    void getEditedVideos_NoEditedVideos_ReturnsEmptyList() {
        // Given
        when(videoDetailService.findVideoById(originalVideoId)).thenReturn(mockVideo);
        when(videoDetailService.findVideosByOriginalVideoId(originalVideoId)).thenReturn(Collections.emptyList());
        
        // When
        List<VideoResponse> result = videoFacadeService.getEditedVideos(originalVideoId);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(videoDetailService, times(1)).findVideoById(originalVideoId);
        verify(videoDetailService, times(1)).findVideosByOriginalVideoId(originalVideoId);
        verify(videoDetailService, never()).mapToResponse(any(Video.class));
    }
}
