package inu.codin.codinticketingapi.security.exception;

import inu.codin.codinticketingapi.common.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum SecurityErrorCode implements GlobalErrorCode {

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", Level.INFO),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다.", Level.INFO),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 없습니다.", Level.INFO),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", Level.INFO);

    private final HttpStatus httpStatus;
    private final String message;
    private final Level logLevel;

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Level logEvent() {
        return logLevel;
    }
}
