package com.storycut.domain.mediachat.service;

import com.storycut.domain.mediachat.dto.request.ChatMessageRequest;
import com.storycut.domain.mediachat.dto.response.ChatMessageResponse;
import com.storycut.domain.mediachat.model.ChatMessage;
import com.storycut.domain.mediachat.repository.ChatMessageRepository;
import com.storycut.domain.room.entity.Room;
import com.storycut.domain.room.service.RoomDetailService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatFacadeServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ChatFacadeServiceTest.class);

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private RoomDetailService roomDetailService;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatFacadeService chatFacadeService;

    private Long roomId;
    private Long memberId;
    private Long hostId;
    private String chatId;
    private ChatMessageRequest chatMessageRequest;
    private ChatMessage chatMessage;
    private ChatMessageResponse chatMessageResponse;
    private Room room;

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
        log.info("ChatMessageRequest 생성: {}", chatMessageRequest);

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
        log.info("ChatMessage 엔티티 생성: id={}, roomId={}, senderId={}, title={}, createdAt={}", 
                chatMessage.getId(), chatMessage.getRoomId(), chatMessage.getSenderId(), 
                chatMessage.getTitle(), chatMessage.getCreatedAt());

        // 채팅 메시지 응답 객체 생성
        chatMessageResponse = ChatMessageResponse.from(chatMessage);
        log.info("ChatMessageResponse 생성: id={}, roomId={}, senderId={}", 
                chatMessageResponse.getId(), chatMessageResponse.getRoomId(), chatMessageResponse.getSenderId());

        // Room 객체 생성
        room = Room.builder()
                .hostMemberId(hostId)
                .title("테스트 방")
                .password("비밀번호")
                .context("방 설명")
                .build();
        log.info("Room 객체 생성: hostId={}, title={}", hostId, "테스트 방");
        log.info("========== 테스트 데이터 초기화 완료 ==========");
    }

    @Test
    @DisplayName("비디오 메시지 저장 테스트 - 정상 케이스")
    void saveVideoMessage_Success() {
        log.info("========== 비디오 메시지 저장 테스트 (정상 케이스) 시작 ==========");
        // given
        log.info("Mock 설정: roomDetailService.findRoomById({})", roomId);
        when(roomDetailService.findRoomById(roomId)).thenReturn(room);
        
        log.info("Mock 설정: chatMessageService.saveMessage({}, {}, {})", roomId, memberId, chatMessageRequest);
        when(chatMessageService.saveMessage(eq(roomId), eq(memberId), any(ChatMessageRequest.class)))
                .thenReturn(chatMessageResponse);

        // when
        log.info("테스트 실행: chatFacadeService.saveVideoMessage({}, {}, {})", roomId, memberId, chatMessageRequest);
        ChatMessageResponse result = chatFacadeService.saveVideoMessage(roomId, memberId, chatMessageRequest);

        // then
        log.info("테스트 결과 검증 시작");
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(chatId);
        assertThat(result.getRoomId()).isEqualTo(roomId);
        assertThat(result.getSenderId()).isEqualTo(memberId);
        assertThat(result.getVideoId()).isEqualTo(chatMessageRequest.getVideoId());
        assertThat(result.getTitle()).isEqualTo(chatMessageRequest.getTitle());
        assertThat(result.getMediaUrl()).isEqualTo(chatMessageRequest.getMediaUrl());
        assertThat(result.getThumbnailUrl()).isEqualTo(chatMessageRequest.getThumbnailUrl());
        log.info("테스트 결과 검증 완료: id={}, roomId={}, senderId={}, title={}", 
                result.getId(), result.getRoomId(), result.getSenderId(), result.getTitle());

        // verify
        log.info("Mock 호출 검증 시작");
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(chatMessageService, times(1)).saveMessage(eq(roomId), eq(memberId), any(ChatMessageRequest.class));
        log.info("Mock 호출 검증 완료");
        log.info("========== 비디오 메시지 저장 테스트 (정상 케이스) 완료 ==========");
    }

    @Test
    @DisplayName("비디오 메시지 저장 테스트 - 방이 존재하지 않는 경우")
    void saveVideoMessage_RoomNotFound() {
        log.info("========== 비디오 메시지 저장 테스트 (방이 존재하지 않는 경우) 시작 ==========");
        // given
        log.info("Mock 설정: roomDetailService.findRoomById({}) - 예외 발생 설정", roomId);
        when(roomDetailService.findRoomById(roomId)).thenThrow(new BusinessException(BaseResponseStatus.NOT_FOUND_ROOM));

        // when & then
        log.info("테스트 실행 및 예외 검증 시작");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            log.info("예외 발생 예상 메서드 호출: chatFacadeService.saveVideoMessage({}, {}, {})", roomId, memberId, chatMessageRequest);
            chatFacadeService.saveVideoMessage(roomId, memberId, chatMessageRequest);
        });

        log.info("발생한 예외 정보: {}", exception.getMessage());
        assertThat(exception.getBaseResponseStatus()).isEqualTo(BaseResponseStatus.NOT_FOUND_ROOM);
        log.info("예외 타입 검증 완료: {}", exception.getBaseResponseStatus());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(chatMessageService, times(0)).saveMessage(anyLong(), anyLong(), any(ChatMessageRequest.class));
        log.info("Mock 호출 검증 완료: roomDetailService.findRoomById 1회 호출됨, chatMessageService.saveMessage 호출 안됨");
        log.info("========== 비디오 메시지 저장 테스트 (방이 존재하지 않는 경우) 완료 ==========");
    }

    @Test
    @DisplayName("채팅 메시지 페이징 조회 테스트")
    void getChatMessages_Success() {
        log.info("========== 채팅 메시지 페이징 조회 테스트 시작 ==========");
        // given
        int page = 0;
        int size = 10;
        List<ChatMessageResponse> responseList = List.of(chatMessageResponse);
        
        log.info("Mock 설정: roomDetailService.findRoomById({})", roomId);
        when(roomDetailService.findRoomById(roomId)).thenReturn(room);
        
        log.info("Mock 설정: chatMessageService.getByRoomIdWithPaging({}, {}, {})", roomId, page, size);
        when(chatMessageService.getByRoomIdWithPaging(roomId, page, size)).thenReturn(responseList);

        // when
        log.info("테스트 실행: chatFacadeService.getChatMessages({}, {}, {})", roomId, page, size);
        List<ChatMessageResponse> result = chatFacadeService.getChatMessages(roomId, page, size);

        // then
        log.info("테스트 결과 검증 시작");
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(chatId);
        log.info("테스트 결과 검증 완료: 결과 크기={}, 첫 번째 메시지 ID={}", result.size(), result.get(0).getId());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(chatMessageService, times(1)).getByRoomIdWithPaging(roomId, page, size);
        log.info("Mock 호출 검증 완료");
        log.info("========== 채팅 메시지 페이징 조회 테스트 완료 ==========");
    }

    @Test
    @DisplayName("특정 시간 이후 메시지 조회 테스트")
    void getMessagesAfter_Success() {
        log.info("========== 특정 시간 이후 메시지 조회 테스트 시작 ==========");
        // given
        LocalDateTime timestamp = LocalDateTime.now().minusHours(1);
        List<ChatMessageResponse> responseList = List.of(chatMessageResponse);
        
        log.info("Mock 설정: roomDetailService.findRoomById({})", roomId);
        when(roomDetailService.findRoomById(roomId)).thenReturn(room);
        
        log.info("Mock 설정: chatMessageService.getByRoomIdAfterTimestamp({}, {})", roomId, timestamp);
        when(chatMessageService.getByRoomIdAfterTimestamp(roomId, timestamp)).thenReturn(responseList);

        // when
        log.info("테스트 실행: chatFacadeService.getMessagesAfter({}, {})", roomId, timestamp);
        List<ChatMessageResponse> result = chatFacadeService.getMessagesAfter(roomId, timestamp);

        // then
        log.info("테스트 결과 검증 시작");
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(chatId);
        log.info("테스트 결과 검증 완료: 결과 크기={}, 첫 번째 메시지 ID={}", result.size(), result.get(0).getId());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(chatMessageService, times(1)).getByRoomIdAfterTimestamp(roomId, timestamp);
        log.info("Mock 호출 검증 완료");
        log.info("========== 특정 시간 이후 메시지 조회 테스트 완료 ==========");
    }

    @Test
    @DisplayName("메시지 삭제 테스트 - 권한 있는 사용자(메시지 작성자)")
    void deleteMessage_Success_MessageSender() {
        log.info("========== 메시지 삭제 테스트 (권한 있는 사용자) 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageService.getRoomIdByChatId({})", chatId);
        when(chatMessageService.getRoomIdByChatId(chatId)).thenReturn(roomId);
        
        log.info("Mock 설정: roomDetailService.findRoomById({})", roomId);
        when(roomDetailService.findRoomById(roomId)).thenReturn(room);
        
        log.info("Mock 설정: chatMessageService.isMemberAuthorized({}, {}, {})", chatId, memberId, hostId);
        when(chatMessageService.isMemberAuthorized(chatId, memberId, hostId)).thenReturn(true);
        
        log.info("Mock 설정: chatMessageService.deleteById({})", chatId);
        when(chatMessageService.deleteById(chatId)).thenReturn(true);

        // when
        log.info("테스트 실행: chatFacadeService.deleteMessage({}, {})", chatId, memberId);
        boolean result = chatFacadeService.deleteMessage(chatId, memberId);

        // then
        log.info("테스트 결과 검증: result={}", result);
        assertThat(result).isTrue();
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageService, times(1)).getRoomIdByChatId(chatId);
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(chatMessageService, times(1)).isMemberAuthorized(chatId, memberId, hostId);
        verify(chatMessageService, times(1)).deleteById(chatId);
        log.info("Mock 호출 검증 완료");
        log.info("========== 메시지 삭제 테스트 (권한 있는 사용자) 완료 ==========");
    }

    @Test
    @DisplayName("메시지 삭제 테스트 - 권한 없는 사용자")
    void deleteMessage_Fail_Unauthorized() {
        log.info("========== 메시지 삭제 테스트 (권한 없는 사용자) 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageService.getRoomIdByChatId({})", chatId);
        when(chatMessageService.getRoomIdByChatId(chatId)).thenReturn(roomId);
        
        log.info("Mock 설정: roomDetailService.findRoomById({})", roomId);
        when(roomDetailService.findRoomById(roomId)).thenReturn(room);
        
        log.info("Mock 설정: chatMessageService.isMemberAuthorized({}, {}, {}) -> false", chatId, memberId, hostId);
        when(chatMessageService.isMemberAuthorized(chatId, memberId, hostId)).thenReturn(false);

        // when & then
        log.info("테스트 실행 및 예외 검증 시작");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            log.info("예외 발생 예상 메서드 호출: chatFacadeService.deleteMessage({}, {})", chatId, memberId);
            chatFacadeService.deleteMessage(chatId, memberId);
        });

        log.info("발생한 예외 정보: {}", exception.getMessage());
        assertThat(exception.getBaseResponseStatus()).isEqualTo(BaseResponseStatus.UNAUTHORIZED_MESSAGE);
        log.info("예외 타입 검증 완료: {}", exception.getBaseResponseStatus());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageService, times(1)).getRoomIdByChatId(chatId);
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(chatMessageService, times(1)).isMemberAuthorized(chatId, memberId, hostId);
        verify(chatMessageService, times(0)).deleteById(anyString());
        log.info("Mock 호출 검증 완료: deleteById 메서드는 호출되지 않음");
        log.info("========== 메시지 삭제 테스트 (권한 없는 사용자) 완료 ==========");
    }

    @Test
    @DisplayName("메시지 삭제 테스트 - 메시지가 존재하지 않는 경우")
    void deleteMessage_Fail_MessageNotFound() {
        log.info("========== 메시지 삭제 테스트 (메시지가 존재하지 않는 경우) 시작 ==========");
        // given
        log.info("Mock 설정: chatMessageService.getRoomIdByChatId({}) - 예외 발생 설정", chatId);
        when(chatMessageService.getRoomIdByChatId(chatId)).thenThrow(new BusinessException(BaseResponseStatus.NOT_FOUND_MESSAGE));

        // when & then
        log.info("테스트 실행 및 예외 검증 시작");
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            log.info("예외 발생 예상 메서드 호출: chatFacadeService.deleteMessage({}, {})", chatId, memberId);
            chatFacadeService.deleteMessage(chatId, memberId);
        });

        log.info("발생한 예외 정보: {}", exception.getMessage());
        assertThat(exception.getBaseResponseStatus()).isEqualTo(BaseResponseStatus.NOT_FOUND_MESSAGE);
        log.info("예외 타입 검증 완료: {}", exception.getBaseResponseStatus());
        
        // verify
        log.info("Mock 호출 검증 시작");
        verify(chatMessageService, times(1)).getRoomIdByChatId(chatId);
        verify(roomDetailService, times(0)).findRoomById(anyLong());
        verify(chatMessageService, times(0)).isMemberAuthorized(anyString(), anyLong(), anyLong());
        verify(chatMessageService, times(0)).deleteById(anyString());
        log.info("Mock 호출 검증 완료: 첫 번째 메서드 호출 후 예외 발생으로 이후 메서드는 호출되지 않음");
        log.info("========== 메시지 삭제 테스트 (메시지가 존재하지 않는 경우) 완료 ==========");
    }
}
