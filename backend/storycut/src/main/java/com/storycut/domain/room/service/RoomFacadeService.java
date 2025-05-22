package com.storycut.domain.room.service;

import static com.storycut.global.model.dto.BaseResponseStatus.*;

import com.storycut.domain.mediachat.service.ChatMessageService;
import com.storycut.domain.room.dto.request.RoomCreateRequest;
import com.storycut.domain.room.dto.request.RoomUpdateRequest;
import com.storycut.domain.room.dto.response.RoomMemberResponse;
import com.storycut.domain.room.dto.response.RoomResponse;
import com.storycut.domain.room.entity.Room;
import com.storycut.domain.room.entity.RoomMember;
import com.storycut.global.exception.BusinessException;
import java.util.Comparator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomFacadeService implements RoomService {
    
    private final RoomDetailService roomDetailService;
    private final RoomMemberService roomMemberService;
    private final RoomInviteService roomInviteService;
    private final ChatMessageService chatMessageService;

    @Override
    @Transactional
    public RoomResponse createRoom(Long memberId, RoomCreateRequest request) {
        // Room 테이블에 공유방 생성
        Room savedRoom = roomDetailService.createRoom(memberId, request);
        
        // 방장을 RoomMember 테이블에 추가
        roomMemberService.addMember(memberId, savedRoom);
        
        // 응답 생성 (참여자 수는 1, 방장만 존재)
        return roomDetailService.mapToResponse(memberId, savedRoom, 1);
    }

    @Override
    public List<RoomResponse> getMyRooms(Long memberId) {
        // 회원이 참여 중인 공유방 목록 조회
        List<Room> rooms = roomDetailService.findRoomsByMemberId(memberId);
        
        // 각 공유방에 대해 참여자 수를 조회하여 응답 객체로 변환
        return rooms.stream()
                .map(room -> {
                    int memberCount = roomMemberService.countMembersByRoomId(room.getId());
                    return roomDetailService.mapToResponse(memberId, room, memberCount);
                })
                .toList();
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(Long memberId, Long roomId, RoomUpdateRequest request) {
        // 방장 권한 확인과 함께 공유방 조회
        Room room = roomDetailService.findRoomByIdAndHostId(roomId, memberId);
        
        // 공유방 정보 수정
        room.updateRoom(request.getRoomTitle(), request.getRoomPassword(), request.getRoomContext());

        // 현재 참여자 수 조회
        int memberCount = roomMemberService.countMembersByRoomId(roomId);

        // 응답 생성
        return roomDetailService.mapToResponse(memberId, room, memberCount);
    }

    @Override
    @Transactional
    public RoomResponse updateThumbnail(Long memberId, Long roomId, String thumbnail) {
        // 방장 권한 확인과 함께 공유방 조회
        Room room = roomDetailService.findRoomByIdAndHostId(roomId, memberId);

        // 공유방 정보 수정
        room.updateThumbnail(thumbnail);

        // 현재 참여자 수 조회
        int memberCount = roomMemberService.countMembersByRoomId(roomId);

        // 응답 생성
        return roomDetailService.mapToResponse(memberId, room, memberCount);
    }

    @Override
    @Transactional
    public void deleteRoom(Long memberId, Long roomId) {
        // 방장 권한 확인과 함께 공유방 조회
        Room room = roomDetailService.findRoomByIdAndHostId(roomId, memberId);
        
        // MongoDB에서 채팅 로그 삭제
        chatMessageService.deleteAllByRoomId(roomId);
        
        // 공유방 삭제 (cascade로 멤버도 함께 삭제됨)
        roomDetailService.deleteRoom(room);
    }

    @Override
    @Transactional
    public String generateInviteCode(Long hostMemberId, Long roomId) {
        // 방장 권한 확인
        Room room = roomDetailService.findRoomByIdAndHostId(roomId, hostMemberId);
        return roomInviteService.generateInviteCode(room.getId());
    }

    @Override
    @Transactional
    public Long enterByCode(String inviteCode) {
        if(inviteCode.length() != 6) {
            throw new BusinessException(LENGTH_INVITE_CODE);
        }
        return roomInviteService.decodeInviteCode(inviteCode);
    }

    @Override
    @Transactional
    public RoomResponse enterRoom(Long memberId, Long roomId, String password) {
        // 공유방 조회
        Room room = roomDetailService.findRoomById(roomId);

        // 비밀번호 검증
        if (!roomDetailService.validatePassword(room, password)) {
            throw new BusinessException(NOT_VALID_PASSWORD);
        }

        // 이미 참여 중인지 확인
        if (roomMemberService.isMemberExists(roomId, memberId)) {
            throw new BusinessException(ALREADY_MEMBER_ROOM);
        }
        roomMemberService.addMember(memberId, room);

        // 현재 참여자 수 조회
        int memberCount = roomMemberService.countMembersByRoomId(roomId);

        // 응답 생성
        return roomDetailService.mapToResponse(memberId, room, memberCount);
    }

    @Override
    @Transactional
    public void leaveRoom(Long memberId, Long roomId) {
        // 공유방 조회
        Room room = roomDetailService.findRoomById(roomId);
        
        // 방장인 경우 특별 처리
        if (room.getHostId().equals(memberId)) {
            // 방에 남은 다른 멤버들 조회
            List<RoomMember> remainingMembers = roomMemberService.findMembersByRoomId(roomId)
                    .stream()
                    .filter(member -> !member.getMemberId().equals(memberId))
                    .toList();
            
            if (remainingMembers.isEmpty() && Objects.equals(room.getHostId(), memberId)) {
                // 남은 멤버가 없으면 방과 관련 데이터 삭제
                log.info("방장 {}가 방을 삭제합니다.", memberId);
                
                // MongoDB에서 채팅 로그 삭제
                chatMessageService.deleteAllByRoomId(roomId);
                
                // 공유방 삭제
                roomDetailService.deleteRoom(room);
                return;
            } else {
                // 생성 시간 기준으로 오름차순 정렬하여 가장 오래된 멤버를 새 방장으로 지정
                RoomMember oldestMember = remainingMembers.stream()
                        .min(Comparator.comparing(RoomMember::getCreatedAt))
                        .orElseThrow();
                
                // 새로운 방장 설정
                room.updateHostId(oldestMember.getMemberId());
                log.info("방장 {}가 방을 떠났습니다. 새로운 방장: {}", memberId, oldestMember.getMemberId());
                // 기존 방장 멤버 제거
                roomMemberService.removeMember(roomId, memberId);
            }
        } else {
            // 일반 멤버인 경우 제거
            roomMemberService.removeMember(roomId, memberId);
        }
    }

    @Override
    public RoomResponse getRoomDetail(Long memberId, Long roomId) {
        // 공유방 조회
        Room room = roomDetailService.findRoomById(roomId);
        
        // 현재 참여자 수 조회
        int memberCount = roomMemberService.countMembersByRoomId(roomId);
        
        // 응답 생성
        return roomDetailService.mapToResponse(memberId, room, memberCount);
    }

    @Override
    public List<RoomMemberResponse> getRoomMembers(Long roomId) {
        // 공유방 참여자 목록 조회
        List<RoomMember> members = roomMemberService.findMembersByRoomId(roomId);
        
        // 응답 생성
        return roomMemberService.mapToResponseList(members);
    }
}
