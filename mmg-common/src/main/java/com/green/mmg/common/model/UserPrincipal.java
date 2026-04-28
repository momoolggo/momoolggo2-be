package com.green.mmg.common.model;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Spring Security가 인증 처리할 때 사용하는 객체
// TokenAuthenticationFilter에서 JWT를 파싱한 뒤 이 객체를 만들어
// SecurityContextHolder에 저장함
// Controller에서 @AuthenticationPrincipal 로 꺼내 쓸 수 있음
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final JwtUser jwtUser;

    // 편의 메서드 — Controller에서 principal.getSignedUserNo() 처럼 바로 꺼내 쓰기 위함
    public long getSignedUserNo() {
        return jwtUser.getSignedUserNo();
    }

    public String getRole() {
        return jwtUser.getRole();
    }

    public String getName() {
        return jwtUser.getName();
    }

    // 권한 목록 반환
    // Spring Security의 hasRole("OWNER") 같은 검사에서 여기 값을 사용함
    // "ROLE_" 접두사를 붙여야 hasRole() 이 정상 동작함
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + jwtUser.getRole()));
    }

    @Override
    public @Nullable String getPassword() { return ""; }

    @Override
    public String getUsername() { return String.valueOf(jwtUser.getSignedUserNo()); }
}
