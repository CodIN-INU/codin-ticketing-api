package inu.codin.codinticketingapi.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 토큰 추출 및 파싱 유틸리티
 */
@Slf4j
public class TokenUtil {

    /**
     * 쿠키 또는 Authorization 헤더에서 Access Token 추출
     */
    public static String extractToken(HttpServletRequest request) {
        String bearerToken = null;
        // 1. 쿠키에서 토큰 추출 (우선순위 1)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    bearerToken = cookie.getValue();
                    break;
                }
            }
        }
        log.debug("[extractToken] Cookie에서 추출한 토큰: {}", bearerToken != null ? "존재" : "없음");
        
        // 2. Authorization 헤더에서 토큰 추출 (우선순위 2)
        if (!StringUtils.hasText(bearerToken)) {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                bearerToken = authHeader.substring(7);
                log.debug("[extractToken] Authorization 헤더에서 토큰 추출: 성공");
            } else {
                log.debug("[extractToken] Authorization 헤더: {}", authHeader != null ? "형식 오류" : "없음");
            }
        }
        
        return StringUtils.hasText(bearerToken) ? bearerToken : null;
    }
}