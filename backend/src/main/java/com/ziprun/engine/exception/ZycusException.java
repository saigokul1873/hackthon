package com.ziprun.engine.exception;

import org.springframework.http.HttpStatus;

public class ZycusException extends RuntimeException {

    private final String statusCode;
    private final String statusMessage;
    private final HttpStatus httpStatus;

    public ZycusException(String statusCode, String statusMessage) {
        this(statusCode, statusMessage, HttpStatus.BAD_REQUEST);
    }

    public ZycusException(String statusCode, String statusMessage, HttpStatus httpStatus) {
        super(statusMessage);
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.httpStatus = httpStatus;
    }

    public ZycusException(String statusCode, String statusMessage, Throwable cause) {
        this(statusCode, statusMessage, HttpStatus.BAD_REQUEST, cause);
    }

    public ZycusException(String statusCode, String statusMessage, HttpStatus httpStatus, Throwable cause) {
        super(statusMessage, cause);
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.httpStatus = httpStatus;
    }

    public ZycusException(ZycusErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus());
    }

    public ZycusException(ZycusErrorCode errorCode, String detail) {
        this(errorCode.getCode(), errorCode.getMessage() + ": " + detail, errorCode.getHttpStatus());
    }

    public ZycusException(ZycusErrorCode errorCode, Throwable cause) {
        this(errorCode.getCode(), errorCode.getMessage(), errorCode.getHttpStatus(), cause);
    }

    public String getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
