package com.ziprun.engine.exception;

import org.springframework.http.HttpStatus;

public enum ZycusErrorCode {

    AGENT_NOT_FOUND("E1001", "Agent not found", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND("E1002", "Order not found", HttpStatus.NOT_FOUND),
    SUGGESTION_NOT_FOUND("E1003", "Suggestion not found", HttpStatus.NOT_FOUND),

    AGENT_OFFLINE("E2001", "Cannot assign order to offline agent", HttpStatus.BAD_REQUEST),
    ORDER_NOT_ELIGIBLE("E2002", "Order is not eligible for reassignment", HttpStatus.BAD_REQUEST),
    SUGGESTION_ALREADY_PROCESSED("E2003", "Suggestion already processed", HttpStatus.BAD_REQUEST),
    ORDER_INVALID_STATUS("E2004", "Invalid order status transition", HttpStatus.BAD_REQUEST),

    ROUTING_NO_AGENT("E3001", "Routing strategy could not find a suitable agent", HttpStatus.BAD_REQUEST),
    ROUTING_STRATEGY_NOT_FOUND("E3002", "Configured routing strategy not found", HttpStatus.INTERNAL_SERVER_ERROR),

    LLM_PROVIDER_UNKNOWN("E4001", "Unknown LLM provider", HttpStatus.INTERNAL_SERVER_ERROR),
    LLM_PARSE_FAILED("E4002", "LLM response parse failed", HttpStatus.INTERNAL_SERVER_ERROR),
    LLM_EMPTY_RESPONSE("E4003", "Empty LLM response", HttpStatus.BAD_REQUEST),
    LLM_INVALID_AGENT("E4004", "LLM response missing agent identifier", HttpStatus.BAD_REQUEST),
    LLM_HALLUCINATED_AGENT("E4005", "LLM returned unknown agent identifier", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ZycusErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ZycusException exception() {
        return new ZycusException(this);
    }

    public ZycusException exception(String detail) {
        return new ZycusException(this, detail);
    }

    public ZycusException exception(Throwable cause) {
        return new ZycusException(this, cause);
    }
}
