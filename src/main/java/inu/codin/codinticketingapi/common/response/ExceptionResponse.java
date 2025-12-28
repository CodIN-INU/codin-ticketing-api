package inu.codin.codinticketingapi.common.response;

import lombok.Getter;

@Getter
public class ExceptionResponse extends CommonResponse {
    private final int status;
    private final String message;

    public ExceptionResponse(int status, String message) {
        super(true, status, message);
        this.status = status;
        this.message = message;
    }
}