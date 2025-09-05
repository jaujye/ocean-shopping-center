package com.ocean.shopping.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static ResourceNotFoundException forEntity(String entityName, Object id) {
        return new ResourceNotFoundException(String.format("%s with id '%s' not found", entityName, id));
    }
    
    public static ResourceNotFoundException forField(String entityName, String fieldName, Object value) {
        return new ResourceNotFoundException(String.format("%s with %s '%s' not found", entityName, fieldName, value));
    }
}