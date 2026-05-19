package com.david.backend.dto;

public record ErrorResponse(
        String error,
        String message
) {}
