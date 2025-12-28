package inu.codin.codinticketingapi.domain.user.exception;

import inu.codin.codinticketingapi.common.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements GlobalErrorCode {

    USER_VALIDATION_FAILED(HttpStatus.NOT_FOUND, "User 정보를 가져올 수 없습니다.", Level.INFO),
    NOT_EXIST_PARTICIPATION_DATA(HttpStatus.BAD_REQUEST, "유저 참여자 정보가 존재하지 않습니다.", Level.INFO),

    FETCH_USER_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error: User 정보를 가져올 수 없습니다.", Level.ERROR);

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
