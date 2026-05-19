package com.david.backend.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(Long id) {
        super("Player not found: " + id);
    }
}
