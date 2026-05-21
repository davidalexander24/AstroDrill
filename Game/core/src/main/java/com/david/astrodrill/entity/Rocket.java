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

    /** How long thrust has been held continuously (seconds). */
    private float thrustBuildupTime = 0f;
    /** Seconds to reach full thrust — spool-up duration. */
    private static final float THRUST_BUILDUP_DURATION = 1f;
    /** Minimum thrust multiplier at t=0 (0 = no thrust at ignition). */
    private static final float MIN_THRUST_FACTOR = 0f;

    public Rocket(Body body, EngineStrategy engine) {
        this.body = body;
        this.engine = engine;
    }

    public EngineStrategy getEngine() { return engine; }
    public void setEngine(EngineStrategy engine) { this.engine = engine; }

    /** Returns current thrust multiplier (0..1) for HUD display. */
    public float getThrustFactor() {
        float t = Math.min(thrustBuildupTime / THRUST_BUILDUP_DURATION, 1f);
        // Quadratic ease-in: starts at 0, gentle ramp, then accelerates
        return MIN_THRUST_FACTOR + (1f - MIN_THRUST_FACTOR) * t;
    }

    /** Call each frame when NOT thrusting to let the engine spool down. */
    public void decayThrust(float delta) {
        thrustBuildupTime = Math.max(0f, thrustBuildupTime - delta * 2f);
    }

    /**
     * Applies thrust and burns fuel proportional to elapsed time.
     * Thrust builds up gradually — very slow at ignition, then ramps up.
     * Returns true if thrust was actually applied (engine present, fuel available).
     */
    public boolean applyThrust(float delta) {
        if (engine == null || body == null) return false;
        if (fuel <= 0f) return false;

        // Accumulate buildup time
        thrustBuildupTime = Math.min(thrustBuildupTime + delta, THRUST_BUILDUP_DURATION);

        float factor = getThrustFactor();
        float thrust = engine.getThrust() * factor;
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
