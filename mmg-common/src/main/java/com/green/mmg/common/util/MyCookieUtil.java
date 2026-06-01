package com.green.mmg.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@Slf4j
@Component //빈등록
@ConditionalOnClass(SecurityFilterChain.class)
public class MyCookieUtil {
    //(보안)쿠키 데이터를 담아라 Client한테 명령
    public void setCookie(HttpServletResponse res, String key, String value, int maxAge, String path) {
        setCookie(res, key, value, maxAge, path, false, null);
    }

    public void setCookie(HttpServletResponse res, String key, String value, int maxAge, String path, boolean secure, String sameSite) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        if (sameSite != null) {
            cookie.setAttribute("SameSite", sameSite);
        }
        if (path != null) {
            cookie.setPath(path);
        }
        res.addCookie(cookie);
    }

    public String getValue(HttpServletRequest req, String key) {
        Cookie cookie = getCookie(req, key);
        return cookie == null ? null : cookie.getValue();
    }

    public Cookie getCookie(HttpServletRequest req, String key) {
        Cookie[] cookies = req.getCookies();
        if( cookies != null && cookies.length > 0 ) { //쿠키에 뭔가 담겨져 있다면
//            for( Cookie c : cookies ) {
//                if(c.getName().equals(key)) { //key이름으로 담겨진 쿠키가 있니?
//                    return c;
//                }
//            }
            for(int i=0; i<cookies.length; i++) {
                Cookie c = cookies[i];
                if(c.getName().equals(key)) { //key이름으로 담겨진 쿠키가 있니?
                    return c;
                }
            }
        }
        return null;
    }

    public void deleteCookie(HttpServletResponse res, String key, String path) {
        setCookie(res, key, null, 0, path);
    }
}
