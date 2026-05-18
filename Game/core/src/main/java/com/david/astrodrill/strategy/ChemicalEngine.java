package com.david.astrodrill.strategy;

public class ChemicalEngine implements EngineStrategy {
    @Override public float getThrust() { return 500f; }
    @Override public float getFuelBurnRate() { return 10f; }
    @Override public String getName() { return "Chemical"; }
}
