package com.david.backend.exception;

public class UsernameTakenException extends RuntimeException {
    public UsernameTakenException(String username) {
        super("Username already taken: " + username);
    }
}
