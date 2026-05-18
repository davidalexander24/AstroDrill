package com.david.astrodrill.strategy;

public interface EngineStrategy {
    float getThrust();

    /** Units of fuel consumed per second of continuous thrust. */
    float getFuelBurnRate();

    /** Human-readable name shown in the flight HUD. */
    String getName();
}
