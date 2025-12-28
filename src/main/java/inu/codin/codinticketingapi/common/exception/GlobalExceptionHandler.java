package inu.codin.codinticketingapi.common.exception;

import inu.codin.codinticketingapi.common.response.ExceptionResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ExceptionResponse> handleException(Exception e) {
        log.warn("[Exception] Class: {}, Error Message : {}, Stack Trace: {}",
                e.getClass().getSimpleName(),
                e.getMessage(),
                e.getStackTrace()[0].toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExceptionResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
    }

    @ExceptionHandler(GlobalException.class)
    protected ResponseEntity<ExceptionResponse> handleGlobalException(GlobalException e) {
        GlobalErrorCode code = e.getErrorCode();

        try (MDC.MDCCloseable httpStatus = MDC.putCloseable("httpStatus", String.valueOf(code.httpStatus().value()));
             MDC.MDCCloseable customMessage = MDC.putCloseable("customMessage", code.message())) {

            StackTraceElement[] stackTrace = e.getStackTrace();

            // 2. 예외 발생 위치 정보가 있을 경우, 중첩된 try-with-resources로 추가 정보를 관리합니다.
            if (stackTrace.length > 0) {
                StackTraceElement topElement = stackTrace[0];
                try (MDC.MDCCloseable className = MDC.putCloseable("className", topElement.getClassName());
                     MDC.MDCCloseable methodName = MDC.putCloseable("methodName", topElement.getMethodName());
                     MDC.MDCCloseable lineNumber = MDC.putCloseable("lineNumber", String.valueOf(topElement.getLineNumber()))) {

                    // 모든 MDC 정보가 포함된 상태에서 로그를 기록합니다.
                    logBasedOnLevel(code, e);
                }
            } else {
                // 기본 MDC 정보만 포함된 상태에서 로그를 기록합니다.
                logBasedOnLevel(code, e);
            }

        } // 이 블록을 벗어나는 순간, 자동으로 모든 MDC.putCloseable 리소스가 close (remove) 됩니다.

        // 5. 클라이언트에게 보낼 응답 생성
        return ResponseEntity.status(code.httpStatus())
                .body(new ExceptionResponse(code.httpStatus().value(), code.message()));
    }

    private void logBasedOnLevel(GlobalErrorCode code, Exception e) {
        String logMessage = "Custom Exception Occurred";
        Level logLevel = code.logEvent();

        if (logLevel == Level.ERROR) {
            log.error(logMessage, e);
        } else if (logLevel == Level.WARN) {
            log.warn(logMessage);
        } else {
            log.info(logMessage);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("[MethodArgumentNotValidException] Error Message : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("[ConstraintViolationException] Error Message : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[AccessDeniedException] Error Message : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ExceptionResponse(HttpStatus.FORBIDDEN.value(), e.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ExceptionResponse> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("[MissingServletRequestPartException] Error Message : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ExceptionResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("[MethodArgumentTypeMismatchException] Error Message : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }

    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<ExceptionResponse> handleConversionFailedException(ConversionFailedException e) {
        log.warn("[ConversionFailedException] Error Message : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
    }
}
