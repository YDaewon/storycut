package com.storycut.domain.member.controller;

import com.storycut.domain.member.model.dto.MemberDto;
import com.storycut.domain.member.service.MemberService;
import com.storycut.global.model.dto.BaseResponse;
import com.storycut.domain.auth.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MemberController implements MemberAPI {

    private final MemberService memberService;

    @Override
    public ResponseEntity<BaseResponse<MemberDto.Response>> getMyInfo(CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        log.info("내 정보 요청 - 사용자 ID: {}, 이메일: {}", memberId, userDetails.getEmail());
        MemberDto.Response response = memberService.getMemberInfo(memberId);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }

    @Override
    public ResponseEntity<BaseResponse<MemberDto.Response>> updateMyInfo(CustomUserDetails userDetails, MemberDto.UpdateRequest updateRequest) {
        Long memberId = userDetails.getMemberId();
        log.info("내 정보 업데이트 요청 - 사용자 ID: {}, 닉네임: {}", memberId, updateRequest.getNickname());
        MemberDto.Response response = memberService.updateMember(memberId, updateRequest);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }

    @Override
    public ResponseEntity<BaseResponse<Void>> deleteMyAccount(CustomUserDetails userDetails) {
        Long memberId = userDetails.getMemberId();
        log.info("계정 탈퇴 요청 - 사용자 ID: {}, 이메일: {}", memberId, userDetails.getEmail());
        memberService.deleteMember(memberId);
        return ResponseEntity.ok(new BaseResponse<>());
    }

    @Override
    public ResponseEntity<BaseResponse<MemberDto.Response>> getMemberById(Long memberId) {
        log.info("특정 사용자 정보 요청 - 사용자 ID: {}", memberId);
        MemberDto.Response response = memberService.getMemberInfo(memberId);
        return ResponseEntity.ok(new BaseResponse<>(response));
    }
}
