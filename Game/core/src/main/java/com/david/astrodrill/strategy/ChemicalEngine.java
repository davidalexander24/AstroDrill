package com.david.astrodrill.strategy;

public class ChemicalEngine implements EngineStrategy {
    @Override
    public float getThrust() {
        return 500f;
    }
}
