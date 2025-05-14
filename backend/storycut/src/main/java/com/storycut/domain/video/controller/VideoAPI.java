package com.storycut.domain.video.controller;

import com.storycut.domain.auth.model.CustomUserDetails;
import com.storycut.domain.video.dto.request.UploadComplate;
import com.storycut.domain.video.dto.request.VideoUploadRequest;
import com.storycut.domain.video.dto.response.VideoResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 비디오 관련 API 인터페이스
 */
@RequestMapping("/video")
@Tag(name = "Video", description = "비디오 API")
public interface VideoAPI {

    /**
     * 비디오 업로드 API
     */
    @PostMapping
    @Operation(
        summary = "비디오 업로드",
        description = "비디오 정보를 DB에 저장합니다. FastAPI에서 S3에 업로드 시작 전 호출해야 합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "비디오가 유효하지 않습니다. (3001)"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)")
    })
    BaseResponse<Long> uploadVideo(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser,
        @Valid @RequestBody VideoUploadRequest request);

    /**
     * 비디오 업로드 API
     */
    @PatchMapping("/complete")
    @Operation(
        summary = "비디오 업로드 완료",
        description = "업로드 완료를 DB에 업데이트합니다. FastAPI에서 S3에 업로드한 후 호출해야 합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "400", description = "비디오가 유효하지 않습니다. (3001)"),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)")
    })
    BaseResponse<VideoResponse> completeUpload(@Valid @RequestBody UploadComplate request);

    /**
     * 비디오 상세 조회 API
     */
    @GetMapping("/{videoId}")
    @Operation(
        summary = "비디오 상세 조회",
        description = "비디오 ID로 비디오 정보를 조회합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "404", description = "해당 비디오가 존재하지 않습니다. (3000)")
    })
    BaseResponse<VideoResponse> getVideo(
        @Parameter(description = "조회할 비디오 ID", required = true) @PathVariable Long videoId);

    /**
     * 내 비디오 목록 조회 API
     */
    @GetMapping("/member")
    @Operation(
        summary = "내 비디오 목록 조회",
        description = "회원이 업로드한 비디오 목록을 조회합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증이 필요합니다. (401)")
    })
    BaseResponse<List<VideoResponse>> getMyVideos(
        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails authUser);

    /**
     * 편집된 비디오 목록 조회 API
     */
    @GetMapping("/edited/{originalVideoId}")
    @Operation(
        summary = "편집된 비디오 목록 조회",
        description = "원본 비디오를 기반으로 편집된 비디오 목록을 조회합니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "404", description = "해당 비디오가 존재하지 않습니다. (3000)")
    })
    BaseResponse<List<VideoResponse>> getEditedVideos(
        @Parameter(description = "원본 비디오 ID", required = true) @PathVariable Long originalVideoId);

    /**
     * 비디오 다운로드 정보 조회 API
     */
    @GetMapping("/download/{videoId}")
    @Operation(
        summary = "비디오 다운로드 정보 조회",
        description = "비디오 다운로드를 위한 정보를 조회합니다. 클라이언트는 반환된 URL을 사용하여 다운로드할 수 있습니다.",
        security = @SecurityRequirement(name = "JWT")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청에 성공하였습니다. (200)",
            content = @Content(schema = @Schema(implementation = BaseResponse.class))),
        @ApiResponse(responseCode = "404", description = "해당 비디오가 존재하지 않습니다. (3000)")
    })
    BaseResponse<VideoResponse> getVideoDownloadInfo(
        @Parameter(description = "다운로드할 비디오 ID", required = true) @PathVariable Long videoId);
}