package com.storycut.domain.mediachat.service;

import com.storycut.domain.mediachat.dto.request.ChatMessageRequest;
import com.storycut.domain.mediachat.dto.response.ChatMessageResponse;
import com.storycut.domain.room.entity.Room;
import com.storycut.domain.room.service.RoomDetailService;
import com.storycut.global.exception.BusinessException;
import com.storycut.global.model.dto.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 서비스 파사드 구현체
 * <p>
 * 채팅 관련 다양한 책임을 효과적으로 조율하고 관리하는 파사드 서비스입니다.
 * 실제 채팅 메시지 CRUD 작업은 하위 서비스에 위임하며, 비즈니스 로직과 세부 구현을 분리합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatFacadeService implements ChatService {

    private final ChatMessageService chatMessageService;
    private final RoomDetailService roomDetailService;

    @Override
    @Transactional
    public ChatMessageResponse saveVideoMessage(Long roomId, Long memberId, ChatMessageRequest request) {
        // Room 존재 확인
        roomDetailService.findRoomById(roomId);
        
        // 채팅 메시지 저장 작업을 ChatMessageService에 위임
        return chatMessageService.saveMessage(roomId, memberId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatMessages(Long roomId, int page, int size) {
        // Room 존재 확인
        roomDetailService.findRoomById(roomId);
        
        // 페이징된 채팅 메시지 조회 작업을 ChatMessageService에 위임
        return chatMessageService.getByRoomIdWithPaging(roomId, page, size);
    }

    @Override
    public ChatMessageResponse getChatMessage(String chatId){
        // 메시지 존재 여부 확인
        ChatMessageResponse message = chatMessageService.getById(chatId);
        if (message == null) {
            throw new BusinessException(BaseResponseStatus.NOT_FOUND_MESSAGE);
        }
        return message;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesAfter(Long roomId, LocalDateTime timestamp) {
        // Room 존재 확인
        roomDetailService.findRoomById(roomId);
        
        // 특정 시간 이후의 메시지 조회 작업을 ChatMessageService에 위임
        return chatMessageService.getByRoomIdAfterTimestamp(roomId, timestamp);
    }

    @Override
    @Transactional
    public boolean deleteMessage(String chatId, Long memberId) {
        // 메시지 존재 여부 및 권한 확인을 위해 Room 정보 조회
        // 메시지가 어떤 Room에 속하는지 먼저 확인해야 함
        Long roomId = getRoomIdFromMessage(chatId);
        Room room = roomDetailService.findRoomById(roomId);
        Long roomHostId = room.getHostId();
        
        // 권한 확인 (메시지 작성자 또는 방장)
        if (!chatMessageService.isMemberAuthorized(chatId, memberId, roomHostId)) {
            throw new BusinessException(BaseResponseStatus.UNAUTHORIZED_MESSAGE);
        }
        
        // 메시지 삭제 작업을 ChatMessageService에 위임
        return chatMessageService.deleteById(chatId);
    }

    private Long getRoomIdFromMessage(String chatId) {
        return chatMessageService.getRoomIdByChatId(chatId);
    }
}
