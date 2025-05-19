package com.storycut.domain.mediachat.service;

import com.storycut.domain.mediachat.dto.response.ChatMessageResponse;
import com.storycut.domain.mediachat.dto.request.ChatMessageRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 채팅 메시지 서비스 인터페이스
 * <p>
 * MongoDB를 사용하여 채팅 메시지를 저장하고 조회하는 기능을 정의합니다.
 * </p>
 */
public interface ChatService {

    /**
     * 비디오 채팅 메시지를 저장합니다.
     *
     * @param roomId Room ID
     * @param memberId 작성자 ID
     * @param request 비디오 채팅 메시지 생성 요청 객체
     * @return 저장된 비디오 채팅 메시지 응답 객체
     */
    ChatMessageResponse saveVideoMessage(Long roomId, Long memberId, ChatMessageRequest request);

    /**
     * Room의 채팅 메시지를 페이징하여 조회합니다.
     *
     * @param roomId Room ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 채팅 메시지 페이지
     */
    List<ChatMessageResponse> getChatMessages(Long roomId, int page, int size);

    /**
     * Room의 채팅 메시지를 조회합니다.
     *
     * @param chatId 채팅 ID
     * @return 채팅 메시지 페이지
     */
    ChatMessageResponse getChatMessage(String chatId);

    /**
     * 특정 시간 이후의 채팅 메시지를 조회합니다.
     * 실시간 채팅에서 새로운 메시지만 가져올 때 사용할 수 있습니다.
     *
     * @param roomId Room ID
     * @param timestamp 조회 시작 시간
     * @return 해당 시간 이후의 채팅 메시지 목록
     */
    List<ChatMessageResponse> getMessagesAfter(Long roomId, LocalDateTime timestamp);

    /**
     * 채팅 메시지를 삭제합니다. (소프트 딜리트)
     * 방장과 메시지 작성자만 삭제할 수 있습니다.
     *
     * @param chatId 채팅 메시지 ID
     * @param memberId 삭제 요청자 ID
     * @return 삭제 성공 여부
     */
    boolean deleteMessage(String chatId, Long memberId);

}
