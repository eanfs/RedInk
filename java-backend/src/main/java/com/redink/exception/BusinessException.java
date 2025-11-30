package com.redink.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常类
 */
public class BusinessException extends RuntimeException {
    
    private final HttpStatus status;
    
    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }
    
    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
}