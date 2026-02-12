package br.com.rentafit.common.exception;

public class ExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final int statusCode;

    public ExternalServiceException(String serviceName, int statusCode, String message) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public ExternalServiceException(String serviceName, int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getStatusCode() {
        return statusCode;
    }
}


