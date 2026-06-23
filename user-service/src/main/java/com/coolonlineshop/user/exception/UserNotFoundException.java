package com.coolonlineshop.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User with id " + id + " not found");
    }

    public UserNotFoundException(String email) {
        super("User with email " + email + " not found");
    }

    public static UserNotFoundException forAuthUserId(Long authUserId) {
        return new UserNotFoundException("Profile for auth user " + authUserId + " not found", true);
    }

    private UserNotFoundException(String message, boolean ignored) {
        super(message);
    }
}
