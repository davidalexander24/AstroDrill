package com.david.astrodrill.entity;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.david.astrodrill.strategy.EngineStrategy;

public class Rocket {
    public Body body;
    private EngineStrategy engine;

    public Rocket(Body body, EngineStrategy engine) {
        this.body = body;
        this.engine = engine;
    }

    public void setEngine(EngineStrategy engine) {
        this.engine = engine;
    }

    public void applyThrust() {
        if (engine != null && body != null) {
            float thrust = engine.getThrust();
            // Add PI/2 so 0 radians (pointing right) acts as pointing up when applying thrust logic
            float angle = body.getAngle() + MathUtils.PI / 2f; 
            float forceX = MathUtils.cos(angle) * thrust;
            float forceY = MathUtils.sin(angle) * thrust;
            body.applyForceToCenter(new Vector2(forceX, forceY), true);
        }
    }
}
