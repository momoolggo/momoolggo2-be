package com.green.mmg.common.jwt;

import com.green.mmg.common.constants.ConstJwt;
import com.green.mmg.common.model.JwtUser;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.common.util.MyCookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnClass(SecurityFilterChain.class)
@RequiredArgsConstructor
public class JwtTokenManager { //인증 처리 총괄
    private final ConstJwt constJwt;
    private final MyCookieUtil myCookieUtil;
    private final JwtTokenProvider jwtTokenProvider;

    public void issue(HttpServletResponse res, JwtUser jwtUser) {
        setAccessTokenInCookie(res, jwtUser);
        setRefreshTokenInCookie(res, jwtUser);
    }

    public void setAccessTokenInCookie(HttpServletResponse res, JwtUser jwtUser) {
        String accessToken = jwtTokenProvider.generateAccessToken(jwtUser);
        setAccessTokenInCookie(res, accessToken);
    }

    public void setRefreshTokenInCookie(HttpServletResponse res, JwtUser jwtUser) {
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser);
        setRefreshTokenInCookie(res, refreshToken);
    }

    //AT를 쿠키에 담는다.
    public void setAccessTokenInCookie(HttpServletResponse res, String accessToken) {
        myCookieUtil.setCookie(res
                , constJwt.getAccessTokenCookieName()
                , accessToken
                , constJwt.getAccessTokenCookieValiditySeconds()
                , constJwt.getAccessTokenCookiePath()
        );
    }

    //RT를 쿠키에 담는다.
    public void setRefreshTokenInCookie(HttpServletResponse res, String refreshToken) {
        myCookieUtil.setCookie(res
                , constJwt.getRefreshTokenCookieName()
                , refreshToken
                , constJwt.getRefreshTokenCookieValiditySeconds()
                , constJwt.getRefreshTokenCookiePath()
        );
    }

    //AT를 쿠키에서 꺼낸다.
    public String getAccessTokenFromCookie(HttpServletRequest req) {
        return myCookieUtil.getValue(req, constJwt.getAccessTokenCookieName());
    }

    //시큐리티에서 로그인 인정을 할 때 이 객체를 Security Context Holder(공간)에 담으면
    //시큐리티는 인증이 되었다고 처리한다.
    public Authentication getAuthentication(HttpServletRequest req) {
        String accessToken = getAccessTokenFromCookie(req); //AT를 쿠키에서 빼낸다.
        if(accessToken == null) { return null; }
        //쿠키에 AT이 있다. JWT에 담았던 JwtUser객체를 다시 빼낸다.
        JwtUser jwtUser = jwtTokenProvider.getJwtUserFromToken(accessToken);
        UserPrincipal userPrincipal = new UserPrincipal(jwtUser);

        return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }

    public void deleteAccessTokenInCookie(HttpServletResponse res) {
        myCookieUtil.deleteCookie(res, constJwt.getAccessTokenCookieName(), constJwt.getAccessTokenCookiePath());
    }

    public void deleteRefreshTokenInCookie(HttpServletResponse res) {
        myCookieUtil.deleteCookie(res, constJwt.getRefreshTokenCookieName(), constJwt.getRefreshTokenCookiePath());
    }

    public void signOut(HttpServletResponse res) {
        deleteAccessTokenInCookie(res);
        deleteRefreshTokenInCookie(res);
    }
}
