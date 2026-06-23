package com.coolonlineshop.user.exception;

public class UserProfileAlreadyExistsException extends RuntimeException {

    public UserProfileAlreadyExistsException(Long authUserId) {
        super("Profile for auth user " + authUserId + " already exists");
    }
}
