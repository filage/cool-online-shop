package com.coolonlineshop.catalog.exception;

import java.util.Map;

public class InvalidPageRequestException extends RuntimeException {

    private final Map<String, String> errors;

    public InvalidPageRequestException(Map<String, String> errors) {
        super("Pagination parameters are invalid");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
