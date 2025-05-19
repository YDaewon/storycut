package com.storycut.domain.room.service;

import static com.storycut.global.model.dto.BaseResponseStatus.*;

import com.storycut.domain.room.dto.request.RoomCreateRequest;
import com.storycut.domain.room.dto.response.RoomResponse;
import com.storycut.domain.room.entity.Room;
import com.storycut.domain.room.repository.RoomRepository;
import com.storycut.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomDetailService {
    
    private final RoomRepository roomRepository;
    

    @Transactional
    public Room createRoom(Long memberId, RoomCreateRequest request) {
        Room room = request.toEntity(memberId);
        return roomRepository.save(room);
    }
    

    public Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_ROOM));
    }
    

    public Room findRoomByIdAndHostId(Long roomId, Long hostMemberId) {
        return roomRepository.findByIdAndHostId(roomId, hostMemberId)
                .orElseThrow(() ->  new BusinessException(NOT_VALID_HOST));
    }
    

    public List<Room> findRoomsByMemberId(Long memberId) {
        return roomRepository.findRoomsByMemberId(memberId);
    }

    @Transactional
    public void updateRoom(Room room, String title, String password, String context) {
        room.updateRoom(title, password, context);// 엔티티끌어쓰고
        roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Room room) {
        roomRepository.delete(room);
    }
    

    public boolean validatePassword(Room room, String password) {
        if (room.getPassword() != null && !room.getPassword().isEmpty()) {
            if (password == null || !room.getPassword().equals(password)) {
                return false;
            }
        }
        return true;
    }
    

    public RoomResponse mapToResponse(Room room, int memberCount) {
        return RoomResponse.from(room, memberCount);
    }
}
