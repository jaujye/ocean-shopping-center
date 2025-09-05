package com.ocean.shopping.exception;

/**
 * Exception thrown for conflict scenarios (e.g., duplicate resources)
 */
public class ConflictException extends RuntimeException {
    
    public ConflictException(String message) {
        super(message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}