package com.storycut.domain.room.service;

import com.storycut.domain.room.dto.request.RoomCreateRequest;
import com.storycut.domain.room.dto.request.RoomUpdateRequest;
import com.storycut.domain.room.dto.response.RoomMemberResponse;
import com.storycut.domain.room.dto.response.RoomResponse;
import com.storycut.domain.room.entity.Room;
import com.storycut.domain.room.entity.RoomMember;
import com.storycut.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomFacadeServiceTest {

    @Mock
    private RoomDetailService roomDetailService;

    @Mock
    private RoomMemberService roomMemberService;

    @Mock
    private RoomInviteService roomInviteService;

    @InjectMocks
    private RoomFacadeService roomFacadeService;

    private Long memberId;
    private Long roomId;
    private Room mockRoom;
    private RoomMember mockRoomMember;
    private List<RoomMember> mockRoomMembers;
    private RoomResponse mockRoomResponse;
    private RoomCreateRequest mockCreateRequest;
    private RoomUpdateRequest mockUpdateRequest;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        memberId = 1L;
        roomId = 100L;
        
        // Room 엔티티 초기화 (실제 엔티티 구조에 맞춤)
        mockRoom = Room.builder()
                .hostMemberId(memberId)
                .title("테스트 방")
                .password("password123")
                .context("테스트 컨텍스트")
                .build();
        // Reflection을 통해 ID 설정 (ID는 생성자에서 설정할 수 없음)
        try {
            java.lang.reflect.Field idField = Room.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(mockRoom, roomId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // RoomMember 엔티티 초기화
        mockRoomMember = RoomMember.builder()
                .memberId(memberId)
                .room(mockRoom)
                .build();
        // Reflection을 통해 createdAt 설정
        try {
            java.lang.reflect.Field createdAtField = RoomMember.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(mockRoomMember, LocalDateTime.now());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        mockRoomMembers = Collections.singletonList(mockRoomMember);
        
        // RoomResponse 객체 초기화 (실제 DTO 구조에 맞춤)
        mockRoomResponse = RoomResponse.builder()
                .roomId(roomId)
                .hostId(memberId)
                .roomTitle("테스트 방")
                .hasPassword(true)
                .memberCount(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        mockCreateRequest = new RoomCreateRequest(
                "테스트 방",
                "password123",
                "테스트 컨텍스트"
                ,null
        );
        
        mockUpdateRequest = new RoomUpdateRequest(
                "업데이트된 방",
                "newPassword",
                "업데이트된 컨텍스트"
        );
    }

    @Test
    @DisplayName("공유방 생성 성공 테스트")
    void createRoom_Success() {
        // Given
        when(roomDetailService.createRoom(eq(memberId), any(RoomCreateRequest.class))).thenReturn(mockRoom);
        when(roomDetailService.mapToResponse(any(Room.class), anyInt())).thenReturn(mockRoomResponse);
        
        // When
        RoomResponse result = roomFacadeService.createRoom(memberId, mockCreateRequest);
        
        // Then
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        assertEquals(memberId, result.getHostId());
        verify(roomDetailService, times(1)).createRoom(eq(memberId), any(RoomCreateRequest.class));
        verify(roomMemberService, times(1)).addMember(eq(memberId), any(Room.class));
    }
    
    @Test
    @DisplayName("내 공유방 목록 조회 성공 테스트")
    void getMyRooms_Success() {
        // Given
        List<Room> rooms = Collections.singletonList(mockRoom);
        when(roomDetailService.findRoomsByMemberId(memberId)).thenReturn(rooms);
        when(roomMemberService.countMembersByRoomId(roomId)).thenReturn(1);
        when(roomDetailService.mapToResponse(any(Room.class), anyInt())).thenReturn(mockRoomResponse);
        
        // When
        List<RoomResponse> result = roomFacadeService.getMyRooms(memberId);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(roomId, result.get(0).getRoomId());
        verify(roomDetailService, times(1)).findRoomsByMemberId(memberId);
        verify(roomMemberService, times(1)).countMembersByRoomId(roomId);
    }
    
    @Test
    @DisplayName("공유방 정보 업데이트 성공 테스트")
    void updateRoom_Success() {
        // Given
        when(roomDetailService.findRoomByIdAndHostId(roomId, memberId)).thenReturn(mockRoom);
        when(roomMemberService.countMembersByRoomId(roomId)).thenReturn(1);
        when(roomDetailService.mapToResponse(mockRoom, 1)).thenReturn(mockRoomResponse);

        // When
        RoomResponse result = roomFacadeService.updateRoom(memberId, roomId, mockUpdateRequest);

        // Then
        assertNotNull(result);
        verify(roomDetailService, times(1)).findRoomByIdAndHostId(roomId, memberId);
        verify(roomMemberService, times(1)).countMembersByRoomId(roomId);
        verify(roomDetailService, times(1)).mapToResponse(mockRoom, 1);

        // 내부 도메인 객체 update 호출은 실제 객체에 대한 상태 변화이므로 verify 불가.
        // 필요 시, Room을 spy로 만들어 updateRoom 호출 여부 확인 가능.
    }
    
    @Test
    @DisplayName("공유방 삭제 성공 테스트")
    void deleteRoom_Success() {
        // Given
        when(roomDetailService.findRoomByIdAndHostId(roomId, memberId)).thenReturn(mockRoom);
        
        // When
        roomFacadeService.deleteRoom(memberId, roomId);
        
        // Then
        verify(roomDetailService, times(1)).findRoomByIdAndHostId(roomId, memberId);
        verify(roomDetailService, times(1)).deleteRoom(mockRoom);
    }
    
    @Test
    @DisplayName("초대 코드 생성 성공 테스트")
    void generateInviteCode_Success() {
        // Given
        String expectedCode = "123456";
        when(roomDetailService.findRoomByIdAndHostId(roomId, memberId)).thenReturn(mockRoom);
        when(roomInviteService.generateInviteCode(roomId)).thenReturn(expectedCode);
        
        // When
        String result = roomFacadeService.generateInviteCode(memberId, roomId);
        
        // Then
        assertEquals(expectedCode, result);
        verify(roomDetailService, times(1)).findRoomByIdAndHostId(roomId, memberId);
        verify(roomInviteService, times(1)).generateInviteCode(roomId);
    }
    
    @Test
    @DisplayName("초대 코드로 방 입장 성공 테스트")
    void enterByCode_Success() {
        // Given
        String inviteCode = "123456";
        when(roomInviteService.decodeInviteCode(inviteCode)).thenReturn(roomId);
        
        // When
        Long result = roomFacadeService.enterByCode(inviteCode);
        
        // Then
        assertEquals(roomId, result);
        verify(roomInviteService, times(1)).decodeInviteCode(inviteCode);
    }
    
    @Test
    @DisplayName("비밀번호로 방 입장 성공 테스트")
    void enterRoom_Success() {
        // Given
        String password = "password123";
        when(roomDetailService.findRoomById(roomId)).thenReturn(mockRoom);
        when(roomDetailService.validatePassword(mockRoom, password)).thenReturn(true);
        when(roomMemberService.isMemberExists(roomId, memberId)).thenReturn(false);
        when(roomMemberService.countMembersByRoomId(roomId)).thenReturn(2); // 입장 후 2명
        when(roomDetailService.mapToResponse(any(Room.class), anyInt())).thenReturn(mockRoomResponse);
        
        // When
        RoomResponse result = roomFacadeService.enterRoom(memberId, roomId, password);
        
        // Then
        assertNotNull(result);
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(roomDetailService, times(1)).validatePassword(mockRoom, password);
        verify(roomMemberService, times(1)).isMemberExists(roomId, memberId);
        verify(roomMemberService, times(1)).addMember(memberId, mockRoom);
    }
    
    @Test
    @DisplayName("비밀번호 불일치로 방 입장 실패 테스트")
    void enterRoom_WrongPassword_ThrowsException() {
        // Given
        String wrongPassword = "wrongPassword";
        when(roomDetailService.findRoomById(roomId)).thenReturn(mockRoom);
        when(roomDetailService.validatePassword(mockRoom, wrongPassword)).thenReturn(false);
        
        // When & Then
        assertThrows(BusinessException.class, () -> 
                roomFacadeService.enterRoom(memberId, roomId, wrongPassword));
        
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(roomDetailService, times(1)).validatePassword(mockRoom, wrongPassword);
        verify(roomMemberService, never()).addMember(anyLong(), any(Room.class));
    }
    
    @Test
    @DisplayName("이미 참여 중인 회원의 방 입장 실패 테스트")
    void enterRoom_AlreadyMember_ThrowsException() {
        // Given
        String password = "password123";
        when(roomDetailService.findRoomById(roomId)).thenReturn(mockRoom);
        when(roomDetailService.validatePassword(mockRoom, password)).thenReturn(true);
        when(roomMemberService.isMemberExists(roomId, memberId)).thenReturn(true);
        
        // When & Then
        assertThrows(BusinessException.class, () -> 
                roomFacadeService.enterRoom(memberId, roomId, password));
        
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(roomDetailService, times(1)).validatePassword(mockRoom, password);
        verify(roomMemberService, never()).addMember(anyLong(), any(Room.class));
    }
    
    @Test
    @DisplayName("일반 회원의 방 나가기 성공 테스트")
    void leaveRoom_NormalMember_Success() {
        // Given
        Long normalMemberId = 2L;
        when(roomDetailService.findRoomById(roomId)).thenReturn(mockRoom);
        
        // When
        roomFacadeService.leaveRoom(normalMemberId, roomId);
        
        // Then
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(roomMemberService, times(1)).removeMember(roomId, normalMemberId);
        verify(roomDetailService, never()).deleteRoom(any(Room.class));
    }
    
    @Test
    @DisplayName("방장이 나가고 남은 멤버가 없을 때 방 삭제 테스트")
    void leaveRoom_HostNoRemainingMembers_DeletesRoom() {
        // Given
        when(roomDetailService.findRoomById(roomId)).thenReturn(mockRoom);
        when(roomMemberService.findMembersByRoomId(roomId)).thenReturn(
                Collections.singletonList(mockRoomMember) // 방장만 있는 상황
        );
        
        // When
        roomFacadeService.leaveRoom(memberId, roomId);
        
        // Then
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(roomMemberService, times(1)).findMembersByRoomId(roomId);
        verify(roomDetailService, times(1)).deleteRoom(mockRoom);
        verify(roomMemberService, never()).removeMember(anyLong(), anyLong());
    }
    
    @Test
    @DisplayName("방장이 나가고 남은 멤버가 있을 때 방장 위임 테스트")
    void leaveRoom_HostWithRemainingMembers_TransfersOwnership() {
        // Given
        Long newHostId = 2L;
        RoomMember anotherMember = RoomMember.builder()
                .memberId(newHostId)
                .room(mockRoom)
                .build();
        
        // Reflection을 통해 createdAt 설정
        try {
            java.lang.reflect.Field createdAtField = RoomMember.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(anotherMember, LocalDateTime.now().minusDays(1)); // 더 오래된 멤버
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        when(roomDetailService.findRoomById(roomId)).thenReturn(mockRoom);
        when(roomMemberService.findMembersByRoomId(roomId)).thenReturn(
                Arrays.asList(mockRoomMember, anotherMember)
        );
        
        // When
        roomFacadeService.leaveRoom(memberId, roomId);
        
        // Then
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(roomMemberService, times(1)).findMembersByRoomId(roomId);
        verify(roomMemberService, times(1)).removeMember(roomId, memberId);
        verify(roomDetailService, never()).deleteRoom(any(Room.class));
    }
    
    @Test
    @DisplayName("공유방 상세 정보 조회 성공 테스트")
    void getRoomDetail_Success() {
        // Given
        when(roomDetailService.findRoomById(roomId)).thenReturn(mockRoom);
        when(roomMemberService.countMembersByRoomId(roomId)).thenReturn(1);
        when(roomDetailService.mapToResponse(any(Room.class), anyInt())).thenReturn(mockRoomResponse);
        
        // When
        RoomResponse result = roomFacadeService.getRoomDetail(roomId);
        
        // Then
        assertNotNull(result);
        assertEquals(roomId, result.getRoomId());
        verify(roomDetailService, times(1)).findRoomById(roomId);
        verify(roomMemberService, times(1)).countMembersByRoomId(roomId);
    }
    
    @Test
    @DisplayName("공유방 멤버 목록 조회 성공 테스트")
    void getRoomMembers_Success() {
        // Given
        List<RoomMemberResponse> memberResponses = Collections.singletonList(
                RoomMemberResponse.builder()
                .memberId(memberId)
                .roomId(roomId)
                .joinedAt(LocalDateTime.now())
                .build()
        );
        
        when(roomMemberService.findMembersByRoomId(roomId)).thenReturn(mockRoomMembers);
        when(roomMemberService.mapToResponseList(mockRoomMembers)).thenReturn(memberResponses);
        
        // When
        List<RoomMemberResponse> result = roomFacadeService.getRoomMembers(roomId);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(roomMemberService, times(1)).findMembersByRoomId(roomId);
        verify(roomMemberService, times(1)).mapToResponseList(mockRoomMembers);
    }
}
