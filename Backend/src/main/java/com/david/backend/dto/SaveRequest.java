package com.david.backend.dto;

import jakarta.validation.constraints.NotNull;

public record SaveRequest(
        @NotNull Long playerId,
        String data,
        int credits,
        String currentPlanet,
        int maxDepthMined,
        long fastestLaunchTime
) {}
