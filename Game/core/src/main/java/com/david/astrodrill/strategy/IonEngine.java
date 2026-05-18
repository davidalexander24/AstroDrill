package com.david.astrodrill.strategy;

public class IonEngine implements EngineStrategy {
    @Override public float getThrust() { return 150f; }
    @Override public float getFuelBurnRate() { return 3f; }
    @Override public String getName() { return "Ion"; }
}
