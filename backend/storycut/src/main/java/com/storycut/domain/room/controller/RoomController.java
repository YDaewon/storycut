package com.storycut.domain.room.controller;

import com.storycut.domain.auth.model.CustomUserDetails;
import com.storycut.domain.room.dto.request.RoomCreateRequest;
import com.storycut.domain.room.dto.request.RoomInviteRequest;
import com.storycut.domain.room.dto.request.RoomUpdateRequest;
import com.storycut.domain.room.dto.response.RoomMemberResponse;
import com.storycut.domain.room.dto.response.RoomResponse;
import com.storycut.domain.room.service.RoomService;
import com.storycut.global.model.dto.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RoomController implements RoomAPI {

    private final RoomService roomService;
    
    @Override
    public ResponseEntity<BaseResponse<RoomResponse>> createRoom(
            CustomUserDetails authUser, RoomCreateRequest request) {
        
        RoomResponse response = roomService.createRoom(authUser.getMemberId(), request);
        return ResponseEntity.ok(new BaseResponse<> (response));
    }

    @Override
    public ResponseEntity<BaseResponse<List<RoomResponse>>> getMyRooms(
            CustomUserDetails authUser) {
        
        List<RoomResponse> response = roomService.getMyRooms(authUser.getMemberId());
        return ResponseEntity.ok(new BaseResponse<> (response));
    }

    @Override
    public ResponseEntity<BaseResponse<RoomResponse>> updateRoom(
            CustomUserDetails authUser, Long roomId, RoomUpdateRequest request) {
        
        RoomResponse response = roomService.updateRoom(authUser.getMemberId(), roomId, request);
        return ResponseEntity.ok(new BaseResponse<> (response));
    }

    @Override
    public ResponseEntity<BaseResponse<RoomResponse>> updateThumbnail(
        CustomUserDetails authUser, Long roomId, String thumbnail) {

        RoomResponse response = roomService.updateThumbnail(authUser.getMemberId(), roomId, thumbnail);
        return ResponseEntity.ok(new BaseResponse<> (response));
    }

    @Override
    public ResponseEntity<BaseResponse<String>> deleteRoom(
            CustomUserDetails authUser, Long roomId) {
        
        roomService.deleteRoom(authUser.getMemberId(), roomId);
        return ResponseEntity.ok(new BaseResponse<> (roomId + "방이 삭제되었습니다."));
    }

    @Override
    public ResponseEntity<BaseResponse<String>> makeInviteCode(
            CustomUserDetails authUser, Long roomId) {
        return ResponseEntity.ok(new BaseResponse<> (roomService.generateInviteCode(authUser.getMemberId(), roomId)));
    }

    @Override
    public ResponseEntity<BaseResponse<Long>> decodeInviteCode(
        String inviteCode) {
        return ResponseEntity.ok(new BaseResponse<> (roomService.enterByCode(inviteCode)));
    }


    @Override
    public ResponseEntity<BaseResponse<RoomResponse>> enterRoom(
            CustomUserDetails authUser, Long roomId, String password) {
        
        RoomResponse response = roomService.enterRoom(authUser.getMemberId(), roomId, password);
        return ResponseEntity.ok(new BaseResponse<> (response));
    }

    @Override
    public ResponseEntity<BaseResponse<String>> leaveRoom(
            CustomUserDetails authUser, Long roomId) {
        
        roomService.leaveRoom(authUser.getMemberId(), roomId);
        return ResponseEntity.ok(new BaseResponse<> (roomId + "번 방에서 나갔습니다."));
    }

    @Override
    public ResponseEntity<BaseResponse<RoomResponse>> getRoomDetail(
            Long roomId) {
        
        RoomResponse response = roomService.getRoomDetail(roomId);
        return ResponseEntity.ok(new BaseResponse<> (response));
    }

    @Override
    public ResponseEntity<BaseResponse<List<RoomMemberResponse>>> getRoomMembers(
            Long roomId) {
        
        List<RoomMemberResponse> response = roomService.getRoomMembers(roomId);
        return ResponseEntity.ok(new BaseResponse<> (response));
    }
}