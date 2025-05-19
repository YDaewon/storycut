package com.storycut.domain.room.repository;

import com.storycut.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    
    List<Room> findAllByOrderByCreatedAtDesc();
    
    Optional<Room> findByIdAndHostId(Long id, Long hostMemberId);
    
    @Query("SELECT r FROM Room r WHERE r.id IN " +
           "(SELECT rm.room.id FROM RoomMember rm WHERE rm.memberId = :memberId)")
    List<Room> findRoomsByMemberId(@Param("memberId") Long memberId);
}
