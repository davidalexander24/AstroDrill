package com.david.astrodrill.strategy;

public class IonEngine implements EngineStrategy {
    @Override
    public float getThrust() {
        return 150f;
    }
}
