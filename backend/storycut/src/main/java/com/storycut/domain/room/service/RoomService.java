package com.storycut.domain.room.service;

import com.storycut.domain.room.dto.request.RoomCreateRequest;
import com.storycut.domain.room.dto.request.RoomUpdateRequest;
import com.storycut.domain.room.dto.response.RoomMemberResponse;
import com.storycut.domain.room.dto.response.RoomResponse;

import java.util.List;

/**
 * 공유방 관련 서비스 인터페이스
 * <p>
 * 공유방의 생성, 조회, 수정, 삭제 및 멤버 관리 기능을 정의합니다.
 * 이 인터페이스는 MCP(Model Context Protocol)에 따라 설계되었습니다.
 * </p>
 */
public interface RoomService {
    
    /**
     * 공유방을 생성합니다.
     * 
     * @param memberId 공유방을 생성하는 회원의 ID (방장)
     * @param request 공유방 생성에 필요한 정보를 담은 요청 객체
     * @return 생성된 공유방 정보를 담은 응답 객체
     * @throws com.storycut.global.exception.BusinessException 유효하지 않은 요청 값이 있는 경우
     */
    RoomResponse createRoom(Long memberId, RoomCreateRequest request);
    
    /**
     * 회원이 참여 중인 모든 공유방 목록을 조회합니다.
     * 
     * @param memberId 조회하려는 회원의 ID
     * @return 회원이 참여 중인 공유방 목록
     */
    List<RoomResponse> getMyRooms(Long memberId);
    
    /**
     * 공유방 정보를 수정합니다.
     * 방장만 공유방 정보를 수정할 수 있습니다.
     * 
     * @param memberId 수정을 요청하는 회원의 ID (방장)
     * @param roomId 수정할 공유방의 ID
     * @param request 수정할 정보를 담은 요청 객체
     * @return 수정된 공유방 정보를 담은 응답 객체
     * @throws com.storycut.global.exception.BusinessException 해당 공유방이 존재하지 않거나 방장이 아닌 경우
     */
    RoomResponse updateRoom(Long memberId, Long roomId, RoomUpdateRequest request);

    /**
     * 공유방 썸네일를 수정합니다.
     * 방장만 썸네일를 수정할 수 있습니다.
     *
     * @param memberId 수정을 요청하는 회원의 ID (방장)
     * @param roomId 수정할 공유방의 ID
     * @param thumbnail 수정할 썸네일 이미지 URL
     * @return 수정된 공유방 정보를 담은 응답 객체
     * @throws com.storycut.global.exception.BusinessException 해당 공유방이 존재하지 않거나 방장이 아닌 경우
     */
    RoomResponse updateThumbnail(Long memberId, Long roomId, String thumbnail);
    
    /**
     * 공유방을 삭제합니다.
     * 방장만 공유방을 삭제할 수 있습니다.
     * 
     * @param memberId 삭제를 요청하는 회원의 ID (방장)
     * @param roomId 삭제할 공유방의 ID
     * @throws com.storycut.global.exception.BusinessException 해당 공유방이 존재하지 않거나 방장이 아닌 경우
     */
    void deleteRoom(Long memberId, Long roomId);

    /**
     * 공유방 초대코드를 생성합니다.
     *
     * 방장이 초대 버튼을 눌렀을 때 호출되며, 랜덤 6자리 코드를 Redis에 10분간 저장합니다.
     * 해당 코드는 다른 사용자가 방에 입장할 때 사용됩니다.
     *
     * @param hostMemberId 초대코드를 생성할 방의 방장 ID
     * @param roomId 초대코드를 생성할 공유방 ID
     * @return 6자리 초대코드 문자열
     * @throws com.storycut.global.exception.BusinessException 방장 권한이 없을 경우
     */
    String generateInviteCode(Long hostMemberId, Long roomId);

    /**
     * 초대코드를 사용해 공유방 ID를 조회합니다.
     *
     * Redis에 저장된 초대코드로부터 공유방 ID를 반환합니다.
     * 유효하지 않거나 만료된 초대코드일 경우 예외를 발생시킵니다.
     *
     * @param inviteCode 초대코드 (6자리)
     * @return 공유방 ID
     * @throws com.storycut.global.exception.BusinessException 초대코드가 유효하지 않을 경우
     */
    Long enterByCode(String inviteCode);

    /**
     * 비밀번호를 입력하여 공유방에 입장합니다.
     *
     * 방에 참여 중이지 않은 사용자가 유효한 비밀번호를 입력하면 공유방에 입장되며,
     * RoomMember로 추가됩니다. 이미 참여 중이면 예외를 발생시킵니다.
     *
     * @param memberId 입장하려는 사용자 ID
     * @param roomId 입장하려는 공유방 ID
     * @param password 공유방 비밀번호
     * @return 입장 후 공유방 정보를 포함한 응답 객체
     * @throws com.storycut.global.exception.BusinessException 비밀번호가 유효하지 않거나 이미 참여 중일 경우
     */
    RoomResponse enterRoom(Long memberId, Long roomId, String password);
    
    /**
     * 공유방에서 나갑니다.
     * 방장이 나갈 경우 다음 규칙이 적용됩니다:
     * 1. 방에 남은 다른 멤버가 있을 경우, 가입 시간이 가장 오래된(생성 일자가 가장 빠른) 멤버가 새로운 방장으로 지정됩니다.
     * 2. 방에 남은 다른 멤버가 없을 경우, 방은 자동으로 삭제됩니다.
     * 일반 멤버가 나갈 경우에는 단순히 공유방 멤버에서 제외됩니다.
     * 
     * @param memberId 나가기를 요청하는 회원의 ID
     * @param roomId 나갈 공유방의 ID
     * @throws com.storycut.global.exception.BusinessException 해당 공유방이 존재하지 않는 경우
     * @throws java.util.NoSuchElementException 방장이 나가는데 남은 멤버가 있지만 가장 오래된 멤버를 찾을 수 없는 경우
     */
    void leaveRoom(Long memberId, Long roomId);
    
    /**
     * 공유방의 상세 정보를 조회합니다.
     * 
     * @param roomId 조회할 공유방의 ID
     * @return 공유방 상세 정보를 담은 응답 객체
     * @throws com.storycut.global.exception.BusinessException 해당 공유방이 존재하지 않는 경우
     */
    RoomResponse getRoomDetail(Long roomId);
    
    /**
     * 공유방 참여자 목록을 조회합니다.
     * 
     * @param roomId 조회할 공유방의 ID
     * @return 공유방 참여자 목록을 담은 응답 객체 리스트
     */
    List<RoomMemberResponse> getRoomMembers(Long roomId);
}
