package com.david.astrodrill.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.david.astrodrill.entity.Rocket;
import com.david.astrodrill.strategy.ChemicalEngine;
import com.david.astrodrill.strategy.IonEngine;

public class FlightScreen implements Screen {

    private OrthographicCamera camera;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private Rocket rocket;
    private Body launchpad;
    private boolean isChemicalEngine = true;

    @Override
    public void show() {
        camera = new OrthographicCamera();
        world = new World(new Vector2(0, -9.8f), true);
        debugRenderer = new Box2DDebugRenderer();

        // Create Launchpad
        BodyDef padDef = new BodyDef();
        padDef.type = BodyDef.BodyType.StaticBody;
        padDef.position.set(0, -2);
        launchpad = world.createBody(padDef);
        
        PolygonShape padShape = new PolygonShape();
        padShape.setAsBox(10f, 1f);
        launchpad.createFixture(padShape, 0.0f);
        padShape.dispose();

        // Create Rocket
        BodyDef rocketDef = new BodyDef();
        rocketDef.type = BodyDef.BodyType.DynamicBody;
        rocketDef.position.set(0, 2);
        Body rocketBody = world.createBody(rocketDef);

        PolygonShape rocketShape = new PolygonShape();
        rocketShape.setAsBox(0.5f, 2f);
        FixtureDef fixDef = new FixtureDef();
        fixDef.shape = rocketShape;
        fixDef.density = 1f;
        fixDef.friction = 0.5f;
        rocketBody.createFixture(fixDef);
        rocketShape.dispose();

        rocket = new Rocket(rocketBody, new ChemicalEngine());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            rocket.applyThrust();
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            rocket.body.applyTorque(15f, true); // Rotate left
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            rocket.body.applyTorque(-15f, true); // Rotate right
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            isChemicalEngine = !isChemicalEngine;
            if (isChemicalEngine) {
                rocket.setEngine(new ChemicalEngine());
                System.out.println("Engine swapped: Chemical Engine (High Thrust)");
            } else {
                rocket.setEngine(new IonEngine());
                System.out.println("Engine swapped: Ion Engine (Low Thrust)");
            }
        }

        world.step(delta, 6, 2);

        camera.position.set(rocket.body.getPosition().x, rocket.body.getPosition().y, 0);
        camera.update();

        debugRenderer.render(world, camera.combined);
    }

    @Override
    public void resize(int width, int height) {
        float viewportWidth = 30f;
        float viewportHeight = viewportWidth * ((float) height / width);
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        camera.position.set(0, 0, 0);
        camera.update();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
    }
}
