package com.david.astrodrill.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.entity.Rocket;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.strategy.ChemicalEngine;
import com.david.astrodrill.strategy.IonEngine;

public class FlightScreen implements Screen {

    private static final int REQUIRED_FUEL = 20;
    private static final int REQUIRED_PLATING = 10;
    private static final int REQUIRED_CIRCUITS = 5;
    private static final float ESCAPE_ALTITUDE = 1000f;

    private enum FlightState { ACTIVE, WIN, CRASH }
    private FlightState state = FlightState.ACTIVE;

    private OrthographicCamera worldCamera;
    private OrthographicCamera hudCamera;
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private Rocket rocket;
    private Body launchpad;
    private boolean isChemicalEngine = true;

    private SpriteBatch batch;
    private BitmapFont hudFont;
    private BitmapFont bigFont;
    private FreeTypeFontGenerator generator;
    private ShapeRenderer shapeRenderer;
    
    private Texture rocketTexture;
    private TextureRegion[] rocketFrames;
    private Texture bgTexture;
    private float engineAnimationTime = 0f;
    private static final float ANIM_FRAME_DURATION = 0.1f;

    @Override
    public void show() {
        worldCamera = new OrthographicCamera();
        hudCamera = new OrthographicCamera();
        world = new World(new Vector2(0, -9.8f), true);
        debugRenderer = new Box2DDebugRenderer();
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter hp = new FreeTypeFontGenerator.FreeTypeFontParameter();
        hp.size = 22; hp.color = Color.WHITE; hp.borderWidth = 1.5f; hp.borderColor = Color.BLACK;
        hudFont = generator.generateFont(hp);

        FreeTypeFontGenerator.FreeTypeFontParameter bp = new FreeTypeFontGenerator.FreeTypeFontParameter();
        bp.size = 48; bp.color = Color.WHITE; bp.borderWidth = 2.5f; bp.borderColor = Color.BLACK;
        bigFont = generator.generateFont(bp);

        // Launchpad
        BodyDef padDef = new BodyDef();
        padDef.type = BodyDef.BodyType.StaticBody;
        padDef.position.set(0, -2);
        launchpad = world.createBody(padDef);
        PolygonShape padShape = new PolygonShape();
        padShape.setAsBox(10f, 1f);
        launchpad.createFixture(padShape, 0.0f);
        padShape.dispose();

        // Rocket
        BodyDef rocketDef = new BodyDef();
        rocketDef.type = BodyDef.BodyType.DynamicBody;
        rocketDef.position.set(0, 2);
        Body rocketBody = world.createBody(rocketDef);
        PolygonShape rocketShape = new PolygonShape();
        rocketShape.setAsBox(1.0f, 3.0f, new Vector2(0f, 0.9f), 0f);
        FixtureDef fixDef = new FixtureDef();
        fixDef.shape = rocketShape;
        fixDef.density = 1f;
        fixDef.friction = 0.5f;
        rocketBody.createFixture(fixDef);
        rocketShape.dispose();

        rocket = new Rocket(rocketBody, new ChemicalEngine());

        // Consume launch cargo from vault and fuel the rocket
        GameManager gm = GameManager.getInstance();
        gm.consumeItems(ItemType.ROCKET_FUEL, REQUIRED_FUEL);
        gm.consumeItems(ItemType.HULL_PLATING, REQUIRED_PLATING);
        gm.consumeItems(ItemType.CIRCUIT_BOARD, REQUIRED_CIRCUITS);
        rocket.fuel = rocket.maxFuel;
        
        rocketTexture = new Texture(Gdx.files.internal("textures/rocket_anim.png"));
        TextureRegion[][] tmp = TextureRegion.split(rocketTexture, rocketTexture.getWidth() / 4, rocketTexture.getHeight());
        rocketFrames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) {
            rocketFrames[i] = tmp[0][i];
        }

        bgTexture = new Texture(Gdx.files.internal("textures/space_bg.png"));
        bgTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── Input + Physics (only when active) ───────────────────────────
        boolean thrusting = false;
        if (state == FlightState.ACTIVE) {
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
                if (rocket.applyThrust(delta)) {
                    thrusting = true;
                }
            } else {
                // Spool down engine when not thrusting
                rocket.decayThrust(delta);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                rocket.body.applyTorque(15f, true);
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                rocket.body.applyTorque(-15f, true);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                isChemicalEngine = !isChemicalEngine;
                rocket.setEngine(isChemicalEngine ? new ChemicalEngine() : new IonEngine());
            }

            world.step(delta, 6, 2);

            // Track max altitude
            float y = rocket.body.getPosition().y;
            if (y > rocket.maxAltitude) rocket.maxAltitude = y;

            // Win
            if (rocket.maxAltitude >= ESCAPE_ALTITUDE) {
                state = FlightState.WIN;
            }
            // Lose: out of fuel and falling
            else if (rocket.fuel <= 0f && rocket.body.getLinearVelocity().y <= 0f
                    && rocket.maxAltitude < ESCAPE_ALTITUDE) {
                state = FlightState.CRASH;
            }
        }
        
        if (thrusting) {
            engineAnimationTime += delta;
            if (engineAnimationTime > 3 * ANIM_FRAME_DURATION) {
                engineAnimationTime = 3 * ANIM_FRAME_DURATION;
            }
        } else {
            engineAnimationTime -= delta;
            if (engineAnimationTime < 0f) {
                engineAnimationTime = 0f;
            }
        }

        // Always allow R to return to mining phase
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            GameManager.getInstance().changeScreen(GameManager.ScreenType.PLAY);
            return;
        }
        // After winning, Enter returns to main menu
        if (state == FlightState.WIN && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            GameManager.getInstance().changeScreen(GameManager.ScreenType.MAIN_MENU);
            return;
        }

        // ── Render world ─────────────────────────────────────────────────
        worldCamera.position.set(rocket.body.getPosition().x, rocket.body.getPosition().y, 0);
        worldCamera.update();

        // Parallax space background
        float viewW = worldCamera.viewportWidth * worldCamera.zoom;
        float viewH = worldCamera.viewportHeight * worldCamera.zoom;
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        int srcX = (int)(worldCamera.position.x * 20f);
        int srcY = (int)(-worldCamera.position.y * 20f);
        batch.draw(bgTexture, worldCamera.position.x - viewW / 2f, worldCamera.position.y - viewH / 2f,
                   viewW, viewH, srcX, srcY, (int)(viewW * 20f), (int)(viewH * 20f), false, false);
        batch.end();

        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.3f, 0.3f, 0.35f, 1f);
        shapeRenderer.rect(-20f, -3f, 40f, 2f);
        shapeRenderer.end();
        
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        int frameIndex = (int)(engineAnimationTime / ANIM_FRAME_DURATION);
        if (frameIndex > 3) frameIndex = 3;
        if (frameIndex < 0) frameIndex = 0;
        
        float rx = rocket.body.getPosition().x;
        float ry = rocket.body.getPosition().y;
        float angle = rocket.body.getAngle() * com.badlogic.gdx.math.MathUtils.radiansToDegrees;
        batch.draw(rocketFrames[frameIndex], rx - 2.5f, ry - 10f, 2.5f, 10f, 5f, 20f, 1f, 1f, angle);
        batch.end();

        // ── Render HUD ───────────────────────────────────────────────────
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        float w = hudCamera.viewportWidth;
        float h = hudCamera.viewportHeight;

        hudFont.setColor(Color.WHITE);
        hudFont.draw(batch, String.format("Altitude: %.0f m  /  %.0f", rocket.maxAltitude, ESCAPE_ALTITUDE), 20, h - 20);
        float fuelPct = (rocket.fuel / rocket.maxFuel) * 100f;
        hudFont.setColor(fuelPct < 25f ? Color.RED : (fuelPct < 50f ? Color.YELLOW : Color.WHITE));
        hudFont.draw(batch, String.format("Fuel: %.0f%%", fuelPct), 20, h - 48);
        float thrustPct = rocket.getThrustFactor() * 100f;
        hudFont.setColor(thrustPct < 30f ? new Color(1f, 0.6f, 0.2f, 1f) : (thrustPct < 70f ? Color.YELLOW : new Color(0.4f, 1f, 0.5f, 1f)));
        hudFont.draw(batch, String.format("Thrust: %.0f%%", thrustPct), 20, h - 76);
        hudFont.setColor(new Color(0.6f, 0.85f, 1f, 1f));
        hudFont.draw(batch, "Engine: " + rocket.getEngine().getName() + "  (E to swap)", 20, h - 104);

        if (state == FlightState.ACTIVE) {
            hudFont.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
            hudFont.draw(batch, "W/Up: thrust   A/D: rotate   R: abort to mining", 20, 24);
        } else if (state == FlightState.WIN) {
            bigFont.setColor(new Color(0.4f, 1f, 0.5f, 1f));
            bigFont.draw(batch, "ESCAPE SUCCESSFUL", w / 2f - 280, h / 2f + 20);
            hudFont.setColor(Color.WHITE);
            hudFont.draw(batch, "Orbit Reached", w / 2f - 90, h / 2f - 30);
            hudFont.draw(batch, "Press ENTER to return to main menu", w / 2f - 200, h / 2f - 80);
        } else if (state == FlightState.CRASH) {
            bigFont.setColor(new Color(1f, 0.4f, 0.3f, 1f));
            bigFont.draw(batch, "CRASH - OUT OF FUEL", w / 2f - 280, h / 2f + 20);
            hudFont.setColor(Color.WHITE);
            hudFont.draw(batch, "Press R to retry from the surface", w / 2f - 200, h / 2f - 30);
        }
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        float viewportWidth = 90f;
        float viewportHeight = viewportWidth * ((float) height / width);
        worldCamera.setToOrtho(false, viewportWidth, viewportHeight);
        worldCamera.position.set(0, 0, 0);
        worldCamera.update();
        hudCamera.setToOrtho(false, width, height);
        hudCamera.update();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (batch != null) batch.dispose();
        if (hudFont != null) hudFont.dispose();
        if (bigFont != null) bigFont.dispose();
        if (generator != null) generator.dispose();
        if (rocketTexture != null) rocketTexture.dispose();
        if (bgTexture != null) bgTexture.dispose();
    }
}
