package com.david.astrodrill.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.david.astrodrill.strategy.EngineStrategy;

public class Rocket {
    public Body body;
    private EngineStrategy engine;

    public float fuel = 0f;
    public float maxFuel = 100f;
    public float maxAltitude = 0f;

    public Rocket(Body body, EngineStrategy engine) {
        this.body = body;
        this.engine = engine;
    }

    public EngineStrategy getEngine() { return engine; }
    public void setEngine(EngineStrategy engine) { this.engine = engine; }

    /**
     * Applies thrust and burns fuel proportional to elapsed time.
     * Returns true if thrust was actually applied (engine present, fuel available).
     */
    public boolean applyThrust(float delta) {
        if (engine == null || body == null) return false;
        if (fuel <= 0f) return false;

        float thrust = engine.getThrust();
        float angle = body.getAngle() + MathUtils.PI / 2f;
        float forceX = MathUtils.cos(angle) * thrust;
        float forceY = MathUtils.sin(angle) * thrust;
        body.applyForceToCenter(new Vector2(forceX, forceY), true);

        fuel -= engine.getFuelBurnRate() * delta;
        if (fuel < 0f) fuel = 0f;
        return true;
    }

    public void refuel(float amount) {
        fuel = Math.min(maxFuel, fuel + amount);
    }
}
