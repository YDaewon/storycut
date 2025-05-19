package com.storycut.domain.member.service;

import com.storycut.domain.member.model.dto.MemberDto;
import com.storycut.domain.member.model.entity.Member;
import com.storycut.domain.member.repository.MemberRepository;
import com.storycut.global.exception.BusinessException;
import com.storycut.global.model.dto.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(readOnly = true)
    public MemberDto.Response getMemberInfo(Long memberId) {
        // memberId로 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BaseResponseStatus.USER_NOT_FOUND));
        
        return MemberDto.Response.fromEntity(member);
    }

    @Transactional
    public MemberDto.Response updateMember(Long memberId, MemberDto.UpdateRequest updateRequest) {
        // memberId로 회원 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BaseResponseStatus.USER_NOT_FOUND));
        
        // 변경 사항 적용
        if (updateRequest.getNickname() != null) {
            member.updateNickname(updateRequest.getNickname());
        }

        // 프로필 이미지 변경은 추후
//        if (updateRequest.getProfileImg() != null) {
//            member.updateProfileImg(updateRequest.getProfileImg());
//        }

        // 변경된 정보 저장
        Member updatedMember = memberRepository.save(member);
        log.info("회원 정보 업데이트 완료 - ID: {}", memberId);
        
        return MemberDto.Response.fromEntity(updatedMember);
    }

    @Transactional
    public void deleteMember(Long memberId) {
        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BaseResponseStatus.USER_NOT_FOUND));

        // Redis에서 리프레시 토큰 삭제
        redisTemplate.delete("RT:" + memberId);

        // 회원 정보 삭제
        memberRepository.delete(member);

        log.info("계정 탈퇴 처리 완료 - 사용자 ID: {}", memberId);
    }
}