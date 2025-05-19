package com.storycut.domain.room.service;

import static com.storycut.global.model.dto.BaseResponseStatus.NOT_FOUND_ROOM;

import com.storycut.domain.room.dto.response.RoomMemberResponse;
import com.storycut.domain.room.entity.Room;
import com.storycut.domain.room.entity.RoomMember;
import com.storycut.domain.room.repository.RoomMemberRepository;
import com.storycut.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomMemberService {
    
    private final RoomMemberRepository roomMemberRepository;
    

    @Transactional
    public RoomMember addMember(Long memberId, Room room) {
        RoomMember member = RoomMember.builder()
                .memberId(memberId)
                .room(room)
                .build();
        return roomMemberRepository.save(member);
    }
    

    @Transactional
    public void removeMember(Long roomId, Long memberId) {
        roomMemberRepository.deleteByRoomIdAndMemberId(roomId, memberId);
    }


    public List<RoomMember> findMembersByRoomId(Long roomId) {
        List<RoomMember> members = roomMemberRepository.findByRoomId(roomId)
            .orElseThrow(() -> new BusinessException(NOT_FOUND_ROOM));

        if (members.isEmpty()) {
            throw new BusinessException(NOT_FOUND_ROOM); // 원하는 예외로 던짐
        }

        return members;
    }


    public boolean isMemberExists(Long roomId, Long memberId) {
        return roomMemberRepository.existsByRoomIdAndMemberId(roomId, memberId);
    }


    public int countMembersByRoomId(Long roomId) {
        return roomMemberRepository.findByRoomId(roomId)
            .orElseThrow(() -> new BusinessException(NOT_FOUND_ROOM)).size();
    }


    public RoomMemberResponse mapToResponse(RoomMember member) {
        return RoomMemberResponse.from(member);
    }


    public List<RoomMemberResponse> mapToResponseList(List<RoomMember> members) {
        return members.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}
