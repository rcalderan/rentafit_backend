package br.com.rentafit.common.exception;

import br.com.rentafit.common.util.EnvironmentUtil;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(buildMessage(resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public static ResourceNotFoundException forId(String resourceName, UUID id) {
        return new ResourceNotFoundException(resourceName, "id", id);
    }

    private static String buildMessage(String resourceName, String fieldName, Object fieldValue) {
        if (EnvironmentUtil.isProduction()) {
            return "Not found";
        }
        return String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue);
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }
}

