package com.storycut.domain.room.controller;

import com.storycut.domain.auth.model.CustomUserDetails;
import com.storycut.domain.room.dto.request.RoomCreateRequest;
import com.storycut.domain.room.dto.request.RoomInviteRequest;
import com.storycut.domain.room.dto.request.RoomUpdateRequest;
import com.storycut.domain.room.dto.response.RoomMemberResponse;
import com.storycut.domain.room.dto.response.RoomResponse;
import com.storycut.global.model.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 공유방 관련 API 인터페이스
 */
@RequestMapping("/room")
@Tag(name = "Room", description = "공유방 API ")
public interface RoomAPI {

    /**
     * 공유방 생성 API
     */
    @Operation(
        summary = "공유방 생성",
        description = "새로운 공유방을 생성합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "입력값을 확인해주세요. (400)"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)")
    })
    @PostMapping
    ResponseEntity<BaseResponse<RoomResponse>> createRoom(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
        @Valid @RequestBody RoomCreateRequest request);

    /**
     * 내 공유방 목록 조회 API
     */
    @Operation(
        summary = "내 공유방 목록 조회",
        description = "회원이 참여 중인 모든 공유방 목록을 조회합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)")
    })
    @GetMapping
    ResponseEntity<BaseResponse<List<RoomResponse>>> getMyRooms(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser);

    /**
     * 공유방 수정 API
     */
    @Operation(
        summary = "공유방 수정",
        description = "공유방 정보를 수정합니다. 방장만 수정할 수 있습니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "입력값을 확인해주세요. (400)"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)"),
        @ApiResponse(responseCode = "403", description = "방의 호스트가 아니거나 이미 없는 방입니다. (2001)"),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)")
    })
    @PatchMapping
    ResponseEntity<BaseResponse<RoomResponse>> updateRoom(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
        @Parameter(description = "수정할 공유방 ID", required = true) @RequestParam Long roomId,
        @Valid @RequestBody RoomUpdateRequest request);

    /**
     * 공유방 썸네일 변경 API
     */
    @Operation(
        summary = "공유방 썸네일 변경",
        description = "공유방 썸네일을 변경합니다. 방장만 수정할 수 있습니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "입력값을 확인해주세요. (400)"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)"),
        @ApiResponse(responseCode = "403", description = "방의 호스트가 아니거나 이미 없는 방입니다. (2001)"),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)")
    })
    @PatchMapping("/thumbnail")
    ResponseEntity<BaseResponse<RoomResponse>> updateThumbnail(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
        @Parameter(description = "수정할 공유방 ID", required = true) @RequestParam Long roomId,
        @Valid @RequestBody String thumbnail);

    /**
     * 공유방 삭제 API
     */
    @Operation(
        summary = "공유방 삭제",
        description = "공유방을 삭제합니다. 방장만 삭제할 수 있습니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)"),
        @ApiResponse(responseCode = "403", description = "방의 호스트가 아니거나 이미 없는 방입니다. (2001)"),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)")
    })
    @DeleteMapping
    ResponseEntity<BaseResponse<String>> deleteRoom(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
        @Parameter(description = "삭제할 공유방 ID", required = true) @RequestParam Long roomId);

    /**
     * 공유방 초대코드 생성 API
     */
    @Operation(
        summary = "공유방 초대코드 생성",
        description = "방장이 초대 버튼을 누르면 6자리 초대코드를 생성하고 10분간 유효한 상태로 Redis에 저장합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "초대코드가 생성되었습니다.",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "403", description = "방의 호스트가 아닙니다."),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다.")
    })
    @PostMapping("/invite")
    ResponseEntity<BaseResponse<String>> makeInviteCode(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
        @Parameter(description = "초대코드를 생성할 공유방 ID", required = true) @RequestParam Long roomId);

    /**
     * 초대코드로 방 조회 (roomId 조회) API
     */
    @Operation(
        summary = "초대코드로 공유방 ID 조회",
        description = "초대코드를 사용해 입장할 공유방의 ID를 반환합니다. 초대코드는 Redis에 10분간 유효합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "공유방 ID 조회 성공",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "404", description = "유효하지 않거나 만료된 초대코드입니다. (2004)")
    })
    @GetMapping("/decode")
    ResponseEntity<BaseResponse<Long>> decodeInviteCode(
        @Parameter(description = "입력할 6자리 초대코드", required = true) @RequestParam String inviteCode);

    /**
     * 공유방 입장 API
     */
    @Operation(
        summary = "공유방 입장",
        description = "공유방에 입장합니다. 비밀번호가 설정된 공유방은 비밀번호 검증이 필요합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "비밀번호가 일치하지 않습니다. (2003)"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)"),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)")
    })
    @PostMapping("/enter")
    ResponseEntity<BaseResponse<RoomResponse>> enterRoom(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
        @Parameter(description = "입장할 공유방 ID", required = true) @RequestParam Long roomId,
        @Parameter(description = "공유방 비밀번호 (선택사항)") @RequestParam(required = false) String password);

    /**
     * 공유방 나가기 API
     */
    @Operation(
        summary = "공유방 나가기",
        description = "공유방에서 나갑니다. 방장이 나갈 경우: 1. 방에 남은 다른 멤버가 있을 경우, 가입 시간이 가장 오래된 멤버가 새로운 방장으로 지정됩니다. 2. 방에 남은 다른 멤버가 없을 경우, 방은 자동으로 삭제됩니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)"),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)")
    })
    @PostMapping("/leave")
    ResponseEntity<BaseResponse<String>> leaveRoom(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
        @Parameter(description = "나갈 공유방 ID", required = true) @RequestParam Long roomId);

    /**
     * 공유방 상세 정보 조회 API
     */
    @Operation(
        summary = "공유방 상세 정보 조회",
        description = "공유방의 상세 정보를 조회합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)")
    })
    @GetMapping("/detail/{roomId}")
    ResponseEntity<BaseResponse<RoomResponse>> getRoomDetail(
        @Parameter(description = "조회할 공유방 ID", required = true) @PathVariable Long roomId);

    /**
     * 공유방 참여자 목록 조회 API
     */
    @Operation(
        summary = "공유방 참여자 목록 조회",
        description = "공유방 참여자 목록을 조회합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)")
    })
    @GetMapping("/members/{roomId}")
    ResponseEntity<BaseResponse<List<RoomMemberResponse>>> getRoomMembers(
        @Parameter(description = "조회할 공유방 ID", required = true) @PathVariable Long roomId);
}