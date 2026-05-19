package com.david.backend.dto;

import java.time.Instant;

public record SaveResponse(
        Instant savedAt
) {}
