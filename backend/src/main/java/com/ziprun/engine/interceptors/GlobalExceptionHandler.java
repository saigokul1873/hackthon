package com.ziprun.engine.interceptors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ziprun.engine.exception.ZycusException;
import com.ziprun.engine.exception.ZycusResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ZycusException.class)
    public ResponseEntity<ZycusResponse> handleZycusException(ZycusException ex) {
        ZycusResponse response = new ZycusResponse();
        response.setStatusCode(ex.getStatusCode());
        response.setStatusMessage(ex.getStatusMessage());
        log.error("ZycusException [{}]: {}", ex.getStatusCode(), ex.getStatusMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("details", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ZycusResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        ZycusResponse response = new ZycusResponse();
        response.setStatusCode("E9999");
        response.setStatusMessage("Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
