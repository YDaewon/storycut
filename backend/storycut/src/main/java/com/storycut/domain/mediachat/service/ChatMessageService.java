package com.storycut.domain.mediachat.service;

import com.storycut.domain.mediachat.dto.request.ChatMessageRequest;
import com.storycut.domain.mediachat.dto.response.ChatMessageResponse;
import com.storycut.domain.mediachat.model.ChatMessage;
import com.storycut.domain.mediachat.repository.ChatMessageRepository;
import com.storycut.global.exception.BusinessException;
import com.storycut.global.model.dto.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageResponse getById(Long chatId) {
        // 채팅 메시지 조회
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(chatId.toString());
        if (messageOpt.isEmpty()) {
            throw new BusinessException(BaseResponseStatus.NOT_FOUND_MESSAGE);
        }
        return ChatMessageResponse.from(messageOpt.get());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getAllByRoomId(Long roomId) {
        // 채팅 메시지 조회 (삭제되지 않은 메시지만)
        List<ChatMessage> messages = chatMessageRepository.findByRoomId(roomId);

        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getByRoomIdWithPaging(Long roomId, int page, int size) {
        // 채팅 메시지 페이징 조회
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<ChatMessage> messagePage = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        return messagePage.getContent().stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getByRoomIdAfterTimestamp(Long roomId, LocalDateTime createdAt) {
        // 특정 시간 이후의 채팅 메시지 조회
        List<ChatMessage> messages = chatMessageRepository.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, createdAt);

        return messages.stream()
                .map(ChatMessageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isMemberAuthorized(String chatId, Long memberId, Long roomHostId) {
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(chatId);
        if(messageOpt.isEmpty()) {
            throw new BusinessException(BaseResponseStatus.NOT_FOUND_MESSAGE);
        }

        ChatMessage message = messageOpt.get();

        // 메시지 작성자인지 확인
        boolean isMessageSender = message.getSenderId().equals(memberId);

        // 방장인지 확인 (Facade에서 제공한 roomHostId 활용)
        boolean isRoomOwner = roomHostId.equals(memberId);

        return isMessageSender || isRoomOwner;
    }

    public Long getRoomIdByChatId(String chatId) {
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(chatId);
        if (messageOpt.isEmpty()) {
            throw new BusinessException(BaseResponseStatus.NOT_FOUND_MESSAGE);
        }

        return messageOpt.get().getRoomId();
    }

    public ChatMessageResponse saveMessage(Long roomId, Long memberId, ChatMessageRequest request) {
        // 비디오 채팅 메시지 생성
        ChatMessage chatMessage = request.toEntity(roomId, memberId);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        return ChatMessageResponse.from(savedMessage);
    }

    public boolean deleteById(String chatId) {
        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(chatId);
        if(messageOpt.isEmpty()) {
            throw new BusinessException(BaseResponseStatus.NOT_FOUND_MESSAGE);
        }
        
        // 메시지 삭제 처리
        chatMessageRepository.delete(messageOpt.get());
        return true;
    }

    public void deleteAllByRoomId(Long roomId) {
        chatMessageRepository.deleteByRoomId(roomId);
    }


}
