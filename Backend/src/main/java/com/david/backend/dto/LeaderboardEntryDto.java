package com.david.backend.dto;

public record LeaderboardEntryDto(
        String username,
        int maxDepthMined,
        long fastestLaunchTime
) {}
