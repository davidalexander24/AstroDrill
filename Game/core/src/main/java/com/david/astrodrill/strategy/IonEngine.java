package com.david.astrodrill.strategy;

public class IonEngine implements EngineStrategy {
    @Override public float getThrust() { return 50f; }
    @Override public float getFuelBurnRate() { return 0.5f; }
    @Override public String getName() { return "Ion"; }
}
