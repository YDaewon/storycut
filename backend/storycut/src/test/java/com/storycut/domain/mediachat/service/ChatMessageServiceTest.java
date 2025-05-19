package com.storycut.domain.mediachat.service;

import com.storycut.domain.mediachat.dto.request.ChatMessageRequest;
import com.storycut.domain.mediachat.dto.response.ChatMessageResponse;
import com.storycut.domain.mediachat.model.ChatMessage;
import com.storycut.domain.mediachat.repository.ChatMessageRepository;
import com.storycut.global.exception.BusinessException;
import com.storycut.global.model.dto.BaseResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageServiceTest.class);

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private Long roomId;
    private Long memberId;
    private Long hostId;
    private String chatId;
    private ChatMessageRequest chatMessageRequest;
    private ChatMessage chatMessage;
    private ChatMessageResponse chatMessageResponse;

    @BeforeEach
    void setUp() {
        log.info("========== 테스트 데이터 초기화 시작 ==========");
        // 테스트 데이터 초기화
        roomId = 1L;
        memberId = 2L;
        hostId = 3L;
        chatId = "chat123";

        // 채팅 메시지 요청 객체 생성
        chatMessageRequest = ChatMessageRequest.builder()
                .videoId("vid123")
                .title("테스트 비디오 타이틀")
                .mediaUrl("https://example.com/video.mp4")
                .thumbnailUrl("https://example.com/thumbnail.jpg")
                .build();
        log.info("ChatMessageRequest 생성: videoId={}, title={}", 
                chatMessageRequest.getVideoId(), chatMessageRequest.getTitle());

        // 채팅 메시지 엔티티 생성
        LocalDateTime now = LocalDateTime.now();
        chatMessage = ChatMessage.builder()
                .id(chatId)
                .roomId(roomId)
                .senderId(memberId)
                .videoId(chatMessageRequest.getVideoId())
                .title(chatMessageRequest.getTitle())
                .mediaUrl(chatMessageRequest.getMediaUrl())
                .thumbnailUrl(chatMessageRequest.getThumbnailUrl())
                .createdAt(now)
                .build();
        log.info("ChatMessage 엔티티 생성: id={}, roomId={}, senderId={}, createdAt={}", 
                chatMessage.getId(), chatMessage.getRoomId(), chatMessage.getSenderId(), chatMessage.getCreatedAt());

        // 채팅 메시지 응답 객체 생성
        chatMessageResponse = ChatMessageResponse.from(chatMessage);
        log.info("ChatMessageResponse 생성: id={}, roomId={}, senderId={}", 
                chatMessageResponse.getId(), chatMessageResponse.getRoomId(), chatMessageResponse.getSenderId());
        
        log.info("========== 테스트 데이터 초기화 완료 ==========");
    }

    @Test
    @DisplayName("Room ID로 모든 채팅 메시지 조회 테스트")
    void getAllByRoomId_Success() {
        log.info("========== Room ID로 모든 채팅 메시지 조회 테스트 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageRepository.findByRoomId({})", roomId);
        when(chatMessageRepository.findByRoomId(roomId)).thenReturn(List.of(chatMessage));

        // when
        log.info("테스트 실행: chatMessageService.getAllByRoomId({})", roomId);
        List<ChatMessageResponse> result = chatMessageService.getAllByRoomId(roomId);

        // then
        log.info("테스트 결과 검증 시작");
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(chatId);
        assertThat(result.get(0).getRoomId()).isEqualTo(roomId);
        assertThat(result.get(0).getSenderId()).isEqualTo(memberId);
        log.info("테스트 결과 검증 완료: 결과 크기={}, 첫 번째 메시지 ID={}", result.size(), result.get(0).getId());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findByRoomId(roomId);
        log.info("Mock 호출 검증 완료");
        log.info("========== Room ID로 모든 채팅 메시지 조회 테스트 완료 ==========");
    }

    @Test
    @DisplayName("Room ID로 페이징된 채팅 메시지 조회 테스트")
    void getByRoomIdWithPaging_Success() {
        log.info("========== Room ID로 페이징된 채팅 메시지 조회 테스트 시작 ==========");
        // given
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ChatMessage> messagePage = new PageImpl<>(List.of(chatMessage), pageable, 1);
        
        log.info("Mock 설정: chatMessageRepository.findByRoomIdOrderByCreatedAtDesc({}, {})", roomId, pageable);
        when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq(roomId), any(Pageable.class)))
                .thenReturn(messagePage);

        // when
        log.info("테스트 실행: chatMessageService.getByRoomIdWithPaging({}, {}, {})", roomId, page, size);
        List<ChatMessageResponse> result = chatMessageService.getByRoomIdWithPaging(roomId, page, size);

        // then
        log.info("테스트 결과 검증 시작");
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(chatId);
        log.info("테스트 결과 검증 완료: 결과 크기={}, 첫 번째 메시지 ID={}", result.size(), result.get(0).getId());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findByRoomIdOrderByCreatedAtDesc(eq(roomId), any(Pageable.class));
        log.info("Mock 호출 검증 완료");
        log.info("========== Room ID로 페이징된 채팅 메시지 조회 테스트 완료 ==========");
    }

    @Test
    @DisplayName("특정 시간 이후의 채팅 메시지 조회 테스트")
    void getByRoomIdAfterTimestamp_Success() {
        log.info("========== 특정 시간 이후의 채팅 메시지 조회 테스트 시작 ==========");
        // given
        LocalDateTime timestamp = LocalDateTime.now().minusHours(1);
        log.info("조회 기준 시간: {}", timestamp);
        
        log.info("Mock 설정: chatMessageRepository.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc({}, {})", roomId, timestamp);
        when(chatMessageRepository.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, timestamp))
                .thenReturn(List.of(chatMessage));

        // when
        log.info("테스트 실행: chatMessageService.getByRoomIdAfterTimestamp({}, {})", roomId, timestamp);
        List<ChatMessageResponse> result = chatMessageService.getByRoomIdAfterTimestamp(roomId, timestamp);

        // then
        log.info("테스트 결과 검증 시작");
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(chatId);
        log.info("테스트 결과 검증 완료: 결과 크기={}, 첫 번째 메시지 ID={}", result.size(), result.get(0).getId());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, timestamp);
        log.info("Mock 호출 검증 완료");
        log.info("========== 특정 시간 이후의 채팅 메시지 조회 테스트 완료 ==========");
    }

    @Test
    @DisplayName("메시지 작성자 권한 체크 테스트 - 메시지 작성자")
    void isMemberAuthorized_Success_MessageSender() {
        log.info("========== 메시지 작성자 권한 체크 테스트 (메시지 작성자) 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageRepository.findById({})", chatId);
        when(chatMessageRepository.findById(chatId)).thenReturn(Optional.of(chatMessage));

        // when
        log.info("테스트 실행: chatMessageService.isMemberAuthorized({}, {}, {})", chatId, memberId, hostId);
        boolean result = chatMessageService.isMemberAuthorized(chatId, memberId, hostId);

        // then
        log.info("테스트 결과 검증: 권한 있음 = {}", result);
        assertThat(result).isTrue();
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findById(chatId);
        log.info("Mock 호출 검증 완료");
        log.info("========== 메시지 작성자 권한 체크 테스트 (메시지 작성자) 완료 ==========");
    }

    @Test
    @DisplayName("메시지 작성자 권한 체크 테스트 - 방장")
    void isMemberAuthorized_Success_RoomOwner() {
        log.info("========== 메시지 작성자 권한 체크 테스트 (방장) 시작 ==========");
        // given
        ChatMessage messageWithDifferentSender = ChatMessage.builder()
                .id(chatId)
                .roomId(roomId)
                .senderId(999L) // 다른 발신자
                .videoId(chatMessageRequest.getVideoId())
                .title(chatMessageRequest.getTitle())
                .mediaUrl(chatMessageRequest.getMediaUrl())
                .thumbnailUrl(chatMessageRequest.getThumbnailUrl())
                .build();
        log.info("다른 발신자의 메시지 생성: id={}, senderId={}", chatId, 999L);
        
        log.info("Mock 설정: chatMessageRepository.findById({})", chatId);
        when(chatMessageRepository.findById(chatId)).thenReturn(Optional.of(messageWithDifferentSender));

        // when
        log.info("테스트 실행: chatMessageService.isMemberAuthorized({}, {}, {}) - 방장 ID로 호출", chatId, hostId, hostId);
        boolean result = chatMessageService.isMemberAuthorized(chatId, hostId, hostId);

        // then
        log.info("테스트 결과 검증: 권한 있음 = {}", result);
        assertThat(result).isTrue();
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findById(chatId);
        log.info("Mock 호출 검증 완료");
        log.info("========== 메시지 작성자 권한 체크 테스트 (방장) 완료 ==========");
    }

    @Test
    @DisplayName("메시지 작성자 권한 체크 테스트 - 권한 없음")
    void isMemberAuthorized_Fail_Unauthorized() {
        log.info("========== 메시지 작성자 권한 체크 테스트 (권한 없음) 시작 ==========");
        // given
        Long unauthorizedMemberId = 999L;
        log.info("권한 없는 사용자 ID: {}", unauthorizedMemberId);
        
        log.info("Mock 설정: chatMessageRepository.findById({})", chatId);
        when(chatMessageRepository.findById(chatId)).thenReturn(Optional.of(chatMessage));

        // when
        log.info("테스트 실행: chatMessageService.isMemberAuthorized({}, {}, {})", chatId, unauthorizedMemberId, hostId);
        boolean result = chatMessageService.isMemberAuthorized(chatId, unauthorizedMemberId, hostId);

        // then
        log.info("테스트 결과 검증: 권한 없음 = {}", !result);
        assertThat(result).isFalse();
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findById(chatId);
        log.info("Mock 호출 검증 완료");
        log.info("========== 메시지 작성자 권한 체크 테스트 (권한 없음) 완료 ==========");
    }

    @Test
    @DisplayName("메시지 작성자 권한 체크 테스트 - 메시지 없음")
    void isMemberAuthorized_Fail_MessageNotFound() {
        log.info("========== 메시지 작성자 권한 체크 테스트 (메시지 없음) 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageRepository.findById({}) - 빈 Optional 반환", chatId);
        when(chatMessageRepository.findById(chatId)).thenReturn(Optional.empty());

        // when & then
        log.info("테스트 실행 및 예외 검증 시작");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            log.info("예외 발생 예상 메서드 호출: chatMessageService.isMemberAuthorized({}, {}, {})", chatId, memberId, hostId);
            chatMessageService.isMemberAuthorized(chatId, memberId, hostId);
        });

        log.info("발생한 예외 정보: {}", exception.getMessage());
        assertThat(exception.getBaseResponseStatus()).isEqualTo(BaseResponseStatus.NOT_FOUND_MESSAGE);
        log.info("예외 타입 검증 완료: {}", exception.getBaseResponseStatus());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findById(chatId);
        log.info("Mock 호출 검증 완료");
        log.info("========== 메시지 작성자 권한 체크 테스트 (메시지 없음) 완료 ==========");
    }

    @Test
    @DisplayName("채팅 ID로 Room ID 조회 테스트")
    void getRoomIdByChatId_Success() {
        log.info("========== 채팅 ID로 Room ID 조회 테스트 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageRepository.findById({})", chatId);
        when(chatMessageRepository.findById(chatId)).thenReturn(Optional.of(chatMessage));

        // when
        log.info("테스트 실행: chatMessageService.getRoomIdByChatId({})", chatId);
        Long result = chatMessageService.getRoomIdByChatId(chatId);

        // then
        log.info("테스트 결과 검증: result={}, expected={}", result, roomId);
        assertThat(result).isEqualTo(roomId);
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findById(chatId);
        log.info("Mock 호출 검증 완료");
        log.info("========== 채팅 ID로 Room ID 조회 테스트 완료 ==========");
    }

    @Test
    @DisplayName("채팅 ID로 Room ID 조회 테스트 - 메시지 없음")
    void getRoomIdByChatId_Fail_MessageNotFound() {
        log.info("========== 채팅 ID로 Room ID 조회 테스트 (메시지 없음) 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageRepository.findById({}) - 빈 Optional 반환", chatId);
        when(chatMessageRepository.findById(chatId)).thenReturn(Optional.empty());

        // when & then
        log.info("테스트 실행 및 예외 검증 시작");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            log.info("예외 발생 예상 메서드 호출: chatMessageService.getRoomIdByChatId({})", chatId);
            chatMessageService.getRoomIdByChatId(chatId);
        });

        log.info("발생한 예외 정보: {}", exception.getMessage());
        assertThat(exception.getBaseResponseStatus()).isEqualTo(BaseResponseStatus.NOT_FOUND_MESSAGE);
        log.info("예외 타입 검증 완료: {}", exception.getBaseResponseStatus());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findById(chatId);
        log.info("Mock 호출 검증 완료");
        log.info("========== 채팅 ID로 Room ID 조회 테스트 (메시지 없음) 완료 ==========");
    }

    @Test
    @DisplayName("채팅 메시지 저장 테스트")
    void saveMessage_Success() {
        log.info("========== 채팅 메시지 저장 테스트 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageRepository.save()");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        // when
        log.info("테스트 실행: chatMessageService.saveMessage({}, {}, {})", roomId, memberId, chatMessageRequest);
        ChatMessageResponse result = chatMessageService.saveMessage(roomId, memberId, chatMessageRequest);

        // then
        log.info("테스트 결과 검증 시작");
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(chatId);
        assertThat(result.getRoomId()).isEqualTo(roomId);
        assertThat(result.getSenderId()).isEqualTo(memberId);
        assertThat(result.getVideoId()).isEqualTo(chatMessageRequest.getVideoId());
        log.info("테스트 결과 검증 완료: id={}, roomId={}, senderId={}", 
                result.getId(), result.getRoomId(), result.getSenderId());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        log.info("Mock 호출 검증 완료");
        log.info("========== 채팅 메시지 저장 테스트 완료 ==========");
    }

    @Test
    @DisplayName("채팅 메시지 삭제 테스트")
    void deleteById_Success() {
        log.info("========== 채팅 메시지 삭제 테스트 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageRepository.findById({})", chatId);
        when(chatMessageRepository.findById(chatId)).thenReturn(Optional.of(chatMessage));

        // when
        log.info("테스트 실행: chatMessageService.deleteById({})", chatId);
        boolean result = chatMessageService.deleteById(chatId);

        // then
        log.info("테스트 결과 검증: result={}", result);
        assertThat(result).isTrue();
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findById(chatId);
        verify(chatMessageRepository, times(1)).delete(chatMessage);
        log.info("Mock 호출 검증 완료");
        log.info("========== 채팅 메시지 삭제 테스트 완료 ==========");
    }

    @Test
    @DisplayName("채팅 메시지 삭제 테스트 - 메시지 없음")
    void deleteById_Fail_MessageNotFound() {
        log.info("========== 채팅 메시지 삭제 테스트 (메시지 없음) 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageRepository.findById({}) - 빈 Optional 반환", chatId);
        when(chatMessageRepository.findById(chatId)).thenReturn(Optional.empty());

        // when & then
        log.info("테스트 실행 및 예외 검증 시작");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            log.info("예외 발생 예상 메서드 호출: chatMessageService.deleteById({})", chatId);
            chatMessageService.deleteById(chatId);
        });

        log.info("발생한 예외 정보: {}", exception.getMessage());
        assertThat(exception.getBaseResponseStatus()).isEqualTo(BaseResponseStatus.NOT_FOUND_MESSAGE);
        log.info("예외 타입 검증 완료: {}", exception.getBaseResponseStatus());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).findById(chatId);
        verify(chatMessageRepository, times(0)).delete(any(ChatMessage.class));
        log.info("Mock 호출 검증 완료: delete 메서드는 호출되지 않음");
        log.info("========== 채팅 메시지 삭제 테스트 (메시지 없음) 완료 ==========");
    }

    @Test
    @DisplayName("Room ID로 모든 메시지 삭제 테스트")
    void deleteAllByRoomId_Success() {
        log.info("========== Room ID로 모든 메시지 삭제 테스트 시작 ==========");
        // when
        log.info("테스트 실행: chatMessageService.deleteAllByRoomId({})", roomId);
        chatMessageService.deleteAllByRoomId(roomId);

        // then & verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageRepository, times(1)).deleteByRoomId(roomId);
        log.info("Mock 호출 검증 완료");
        log.info("========== Room ID로 모든 메시지 삭제 테스트 완료 ==========");
    }
}
