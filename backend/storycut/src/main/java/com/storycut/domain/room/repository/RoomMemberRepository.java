package com.storycut.domain.room.repository;

import com.storycut.domain.room.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {
    
    Optional<List<RoomMember>> findByRoomId(Long roomId);
    
    Optional<RoomMember> findByRoomIdAndMemberId(Long roomId, Long memberId);
    
    void deleteByRoomIdAndMemberId(Long roomId, Long memberId);
    
    boolean existsByRoomIdAndMemberId(Long roomId, Long memberId);
    
    List<RoomMember> findByMemberId(Long memberId);
}
