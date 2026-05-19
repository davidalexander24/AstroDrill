package com.david.backend.exception;

public class SaveNotFoundException extends RuntimeException {
    public SaveNotFoundException(Long playerId) {
        super("No save found for player: " + playerId);
    }
}
