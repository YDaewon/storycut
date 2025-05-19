package com.storycut.domain.auth.model;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long memberId;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    
    public CustomUserDetails(Long memberId, String email) {
        this.memberId = memberId;
        this.email = email;
        this.authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // UserDetails 인터페이스 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return ""; // OAuth2 로그인이므로 비밀번호 없음
    }

    @Override
    public String getUsername() {
        return memberId.toString(); // 호환성을 위해 memberId를 문자열로 리턴
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}