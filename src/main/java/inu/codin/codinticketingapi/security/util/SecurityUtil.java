package inu.codin.codinticketingapi.security.util;

import inu.codin.codinticketingapi.security.exception.SecurityErrorCode;
import inu.codin.codinticketingapi.security.exception.SecurityException;
import inu.codin.codinticketingapi.security.jwt.TokenUserDetails;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext 관련 유틸리티 (토큰 검증 전용)
 */
public class SecurityUtil {

    /**
     * 현재 인증된 사용자의 유저 ID 반환
     */
    public static String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof TokenUserDetails userDetails)) {
            throw new SecurityException(SecurityErrorCode.ACCESS_DENIED);
        }

        return userDetails.getUserId();
    }

    /**
     * 현재 인증된 사용자의 이메일(유저이름) 반환
     */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof TokenUserDetails userDetails)) {
            throw new SecurityException(SecurityErrorCode.ACCESS_DENIED);
        }

        return userDetails.getUsername();
    }

    /**
     * 현재 인증된 사용자의 유저 토큰 반환
     */
    public static String getUserToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof TokenUserDetails userDetails)) {
            throw new SecurityException(SecurityErrorCode.ACCESS_DENIED);
        }

        return userDetails.getToken();
    }

    /**
     * 현재 인증된 사용자의 권한 반환
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof TokenUserDetails userDetails)) {
            throw new SecurityException(SecurityErrorCode.ACCESS_DENIED);
        }

        return userDetails.getRole();
    }

    /**
     * 현재 사용자와 주어진 사용자 ID가 같은지 검증
     */
    public static void validateUser(String username) {
        String currentUsername = getUserId();
        if (!currentUsername.equals(username)) {
            throw new SecurityException(SecurityErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * 현재 사용자가 인증되어 있는지 확인
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof TokenUserDetails;
    }

    /**
     * 현재 사용자가 특정 권한을 가지고 있는지 확인
     */
    public static boolean hasRole(String role) {
        try {
            String currentRole = getCurrentUserRole();
            return role.equals(currentRole);
        } catch (JwtException e) {
            return false;
        }
    }
}