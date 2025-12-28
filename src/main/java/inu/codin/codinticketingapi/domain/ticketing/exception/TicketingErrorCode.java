package inu.codin.codinticketingapi.domain.ticketing.exception;

import inu.codin.codinticketingapi.common.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum TicketingErrorCode implements GlobalErrorCode {

    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다.", Level.INFO),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "수령자 정보를 찾을 수 없습니다.", Level.INFO),
    PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트 참여 기록을 찾을 수 없습니다.", Level.INFO),
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "재고 정보를 찾을 수 없습니다.", Level.INFO),
    ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, "옳바르지 않은 정보입니다.", Level.INFO),
    CANNOT_CHANGE_STOCK(HttpStatus.BAD_REQUEST, "재고를 변경할 수 없습니다.", Level.INFO),
    EVENT_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "이벤트 진행 기간이 아닙니다.", Level.INFO),
    SOLD_OUT(HttpStatus.BAD_REQUEST, "티켓팅이 마감되었습니다.", Level.INFO),
    PASSWORD_INVALID(HttpStatus.BAD_REQUEST, "관리자 비밀번호가 맞지 않습니다.", Level.INFO),
    UNAUTHORIZED_EVENT_UPDATE(HttpStatus.UNAUTHORIZED, "인증되지 않은 이벤트 업데이트 입니다.", Level.INFO),
    EVENT_ALREADY_STARTED(HttpStatus.BAD_REQUEST, "이미 이벤트가 시작했습니다.", Level.INFO),
    INSUFFICIENT_TOTAL_STOCK(HttpStatus.BAD_REQUEST, "변경하려는 재고는 남은 재고보다 적을 수 없습니다.", Level.INFO),

    CANNOT_CHANGE_STATUS(HttpStatus.BAD_REQUEST, "티켓팅 참여 상태를 변경할 수 없습니다.", Level.ERROR);

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
