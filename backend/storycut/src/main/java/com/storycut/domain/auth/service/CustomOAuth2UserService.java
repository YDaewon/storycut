package com.storycut.domain.auth.service;

import com.storycut.domain.auth.model.GoogleOAuth2UserInfo;
import com.storycut.domain.auth.model.OAuth2UserInfo;
import com.storycut.domain.member.model.entity.Member;
import com.storycut.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    // 인증이 성공하면 Spring Security는 이 OAuth2User 객체를 Authentication 객체의 principal 필드에 저장함
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 현재 로그인 진행 중인 서비스를 구분하는 코드 (구글만 지원하므로 여기서는 항상 "google")
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        // OAuth2 로그인 진행 시 키가 되는 필드값 (구글의 경우 "sub")
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // OAuth2UserService를 통해 가져온 OAuth2User의 attributes
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        // 구글 응답 데이터 전체 출력 (디버깅용)
        log.info("===== 구글 OAuth2 응답 데이터 (UserService) 시작 =====");
        log.info("OAuth 제공자: {}", registrationId);
        log.info("userNameAttributeName: {}", userNameAttributeName);
        attributes.forEach((key, value) -> {
            log.info("키: {}, 값: {}, 타입: {}", key, value, value != null ? value.getClass().getName() : "null");
        });
        log.info("===== 구글 OAuth2 응답 데이터 (UserService) 끝 =====");

        // 서비스 타입에 따른 OAuth2UserInfo 객체 생성
        OAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

        // 사용자 정보 업데이트 또는 생성
        saveOrUpdateMember(userInfo);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName
        );
    }

    // 사용자 정보를 저장하거나 업데이트하는 메소드
    @Transactional
    public Member saveOrUpdateMember(OAuth2UserInfo userInfo) {
        String providerId = userInfo.getId();
        log.info("OAuth2 사용자 정보 처리 - providerId: {}, email: {}", providerId, userInfo.getEmail());
        
        Optional<Member> memberOptional = memberRepository.findByProviderId(providerId);
        
        if (memberOptional.isPresent()) {
            // 기존 회원이 있으면 정보 업데이트
            Member existingMember = memberOptional.get();
            
            // 필요한 정보 업데이트 (이메일, 이름, 프로필 이미지 등이 변경되었을 수 있음)
            existingMember.updateEmail(userInfo.getEmail());
            existingMember.updateName(userInfo.getName());
            existingMember.updateProfileImg(userInfo.getImageUrl());
            
            log.info("기존 사용자 정보 업데이트 - providerId: {}, memberId: {}", providerId, existingMember.getId());
            return memberRepository.save(existingMember);
        } else {
            // 새 회원이면 생성
            Member newMember = Member.builder()
                    .email(userInfo.getEmail())
                    .name(userInfo.getName())
                    .nickname(userInfo.getName()) // 초기 닉네임은 이름과 동일하게 설정
                    .profileImg(userInfo.getImageUrl())
                    .providerId(providerId) // OAuth 제공자의 고유 ID 저장
                    .build();
            
            Member savedMember = memberRepository.save(newMember);
            log.info("새 사용자 등록 완료 - providerId: {}, memberId: {}", providerId, savedMember.getId());
            return savedMember;
        }
    }
}
