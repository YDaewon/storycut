package com.storycut.domain.mediachat.controller;

import com.storycut.domain.auth.model.CustomUserDetails;
import com.storycut.domain.mediachat.dto.request.ChatMessageRequest;
import com.storycut.domain.mediachat.dto.response.ChatMessageResponse;
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
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 채팅 API 인터페이스 - MCP(Model Context Protocol) 구현
 */
@RequestMapping("/chat")
@Tag(name = "Chat", description = "채팅 메시지 API")
public interface ChatAPI {

    /**
     * 비디오 메시지 전송 API
     */
    @Operation(
        summary = "비디오 채팅 메시지 전송",
        description = "비디오 채팅 메시지를 전송하고 MongoDB에 저장합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "입력값을 확인해주세요. (400)"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)"),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)")
    })
    @PostMapping("/")
    ResponseEntity<BaseResponse<ChatMessageResponse>> sendVideoMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
            @Parameter(description = "메시지를 보낼 방 ID", required = true) @RequestParam Long roomId,
            @Valid @RequestBody ChatMessageRequest request);

    /**
     * 채팅 메시지 목록 조회 API (페이징)
     */
    @Operation(
        summary = "공유방 비디오 목록 조회",
        description = "특정 방의 공유된 비디오를 페이징하여 조회합니다. 10개 단위로 불러오며 페이지 번호로 더 많은 메시지를 불러올 수 있습니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)")
    })
    @GetMapping("/{roomId}")
    ResponseEntity<BaseResponse<List<ChatMessageResponse>>> getChatMessages(
            @Parameter(description = "메시지를 조회할 방 ID", required = true) @PathVariable Long roomId,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size);

    /**
     * 채팅 메시지 목록 조회 API (페이징)
     */
    @Operation(
        summary = "공유방 비디오 상세 조회",
        description = "특정 방의 공유된 비디오 메시지를 조회합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "404", description = "해당 메시지가 존재하지 않습니다. (5000)")
    })
    @GetMapping("/detail/{chatId}")
    ResponseEntity<BaseResponse<ChatMessageResponse>> getMessage(
        @Parameter(description = "조회할 메시지 ID", required = true) @PathVariable Long chatId);

    /**
     * 단일 채팅 메시지 삭제 API
     */
    @Operation(
        summary = "공유방 비디오 삭제",
        description = "공유방 비디오를 삭제합니다. 방장과 쇼츠 작성자만 삭제할 수 있습니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)"),
        @ApiResponse(responseCode = "403", description = "권한이 없습니다. (403)"),
        @ApiResponse(responseCode = "404", description = "해당 방이 존재하지 않습니다. (2000)"),
        @ApiResponse(responseCode = "404", description = "해당 메시지를 찾을 수 없습니다. (5000)"),
        @ApiResponse(responseCode = "404", description = "메시지에 대한 권한이 없습니다. (5002)")
    })
    @DeleteMapping("/{chatId}")
    ResponseEntity<BaseResponse<Boolean>> deleteMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
            @Parameter(description = "삭제할 채팅 메시지 ID", required = true) @PathVariable String chatId);
}
