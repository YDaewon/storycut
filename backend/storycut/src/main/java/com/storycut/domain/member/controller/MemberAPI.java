package com.storycut.domain.member.controller;

import com.storycut.domain.member.model.dto.MemberDto;
import com.storycut.domain.auth.model.CustomUserDetails;
import com.storycut.global.model.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "멤버 API")
@RequestMapping("/members")
public interface MemberAPI {

    @Operation(summary = "회원 정보 조회", description = "회원 정보를 조회합니다.")
    @GetMapping("/detail")
    ResponseEntity<BaseResponse<MemberDto.Response>> getMyInfo(
        @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "회원 정보 수정", description = "회원 정보를 수정합니다.")
    @PatchMapping("/detail")
    ResponseEntity<BaseResponse<MemberDto.Response>> updateMyInfo(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody MemberDto.UpdateRequest updateRequest);

    @Operation(summary = "회원 탈퇴", description = "회원을 탈퇴합니다.")
    @DeleteMapping
    ResponseEntity<BaseResponse<Void>> deleteMyAccount(
        @AuthenticationPrincipal CustomUserDetails userDetails);
}
