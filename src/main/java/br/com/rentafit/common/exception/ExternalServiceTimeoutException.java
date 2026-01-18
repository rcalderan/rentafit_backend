package br.com.rentafit.common.exception;

import br.com.rentafit.common.util.EnvironmentUtil;

public class ExternalServiceTimeoutException extends RuntimeException {

    private final String serviceName;
    private final long timeoutMillis;

    public ExternalServiceTimeoutException(String serviceName, long timeoutMillis) {
        super(buildMessage(serviceName, timeoutMillis));
        this.serviceName = serviceName;
        this.timeoutMillis = timeoutMillis;
    }

    public ExternalServiceTimeoutException(String serviceName, long timeoutMillis, Throwable cause) {
        super(buildMessage(serviceName, timeoutMillis), cause);
        this.serviceName = serviceName;
        this.timeoutMillis = timeoutMillis;
    }

    private static String buildMessage(String serviceName, long timeoutMillis) {
        if (EnvironmentUtil.isProduction()) {
            return "Service unavailable due to timeout";
        }
        return String.format("Timeout calling service %s after %d ms", serviceName, timeoutMillis);
    }

    public String getServiceName() {
        return serviceName;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }
}
