package com.coolonlineshop.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Auth user with email " + email + " already exists");
    }
}
