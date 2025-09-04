package com.ocean.shopping.service.logistics;

/**
 * Exception thrown when carrier operations fail
 */
public class CarrierException extends Exception {
    
    private final String carrierCode;
    private final String errorCode;
    
    public CarrierException(String message) {
        super(message);
        this.carrierCode = null;
        this.errorCode = null;
    }
    
    public CarrierException(String message, Throwable cause) {
        super(message, cause);
        this.carrierCode = null;
        this.errorCode = null;
    }
    
    public CarrierException(String carrierCode, String errorCode, String message) {
        super(message);
        this.carrierCode = carrierCode;
        this.errorCode = errorCode;
    }
    
    public CarrierException(String carrierCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.carrierCode = carrierCode;
        this.errorCode = errorCode;
    }
    
    public String getCarrierCode() {
        return carrierCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CarrierException");
        
        if (carrierCode != null) {
            sb.append(" [").append(carrierCode);
            if (errorCode != null) {
                sb.append(":").append(errorCode);
            }
            sb.append("]");
        }
        
        sb.append(": ").append(getMessage());
        return sb.toString();
    }
}