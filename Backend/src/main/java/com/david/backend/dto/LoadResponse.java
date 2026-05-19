package com.david.backend.dto;

import java.time.Instant;

public record LoadResponse(
        Long playerId,
        String username,
        String data,
        Instant updatedAt
) {}
