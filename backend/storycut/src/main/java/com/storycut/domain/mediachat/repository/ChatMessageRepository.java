package com.storycut.domain.mediachat.repository;

import com.storycut.domain.mediachat.model.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 채팅 메시지 MongoDB 레포지토리
 * <p>
 * MongoDB에 저장된 채팅 메시지를 조회하는 레포지토리입니다.
 * </p>
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * Room ID로 채팅 메시지 목록을 조회합니다.
     *
     * @param roomId 조회할 Room ID
     * @return 해당 Room의 채팅 메시지 목록
     */
    List<ChatMessage> findByRoomId(Long roomId);

    /**
     * Room ID로 채팅 메시지 목록을 페이징하여 조회합니다.
     * 시간 역순으로 정렬됩니다.
     *
     * @param roomId 조회할 Room ID
     * @param pageable 페이징 정보
     * @return 해당 Room의 채팅 메시지 페이지
     */
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);


    /**
     * Room ID로 특정 시간 이후의 채팅 메시지 목록을 조회합니다.
     *
     * @param roomId 조회할 Room ID
     * @param createdAt 조회 시작 시간
     * @return 해당 Room의 특정 시간 이후 채팅 메시지 목록
     */
    List<ChatMessage> findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(Long roomId, LocalDateTime createdAt);

    /**
     * Room ID로 채팅 메시지를 삭제합니다.
     * Room이 삭제될 때 해당 Room의 모든 채팅 메시지도 함께 삭제됩니다.
     *
     * @param roomId 삭제할 Room ID
     */
    void deleteByRoomId(Long roomId);

    /**
     * 특정 발신자의 채팅 메시지 목록을 조회합니다.
     *
     * @param roomId 조회할 Room ID
     * @param senderId 조회할 발신자 ID
     * @param pageable 페이징 정보
     * @return 해당 발신자의 채팅 메시지 페이지
     */
    Page<ChatMessage> findByRoomIdAndSenderIdOrderByCreatedAtDesc(Long roomId, Long senderId, Pageable pageable);

    /**
     * 키워드가 포함된 채팅 메시지 목록을 조회합니다.
     *
     * @param roomId 조회할 Room ID
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 채팅 메시지 페이지
     */
    Page<ChatMessage> findByRoomIdAndTitleContainingOrderByCreatedAtDesc(Long roomId, String keyword, Pageable pageable);
}
