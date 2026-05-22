package com.david.astrodrill.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.GameManager.ScreenType;
import com.david.astrodrill.network.BackendClient;
import com.david.astrodrill.network.BackendException;
import com.david.astrodrill.network.dto.LoadResponse;
import com.david.astrodrill.ui.MenuSkin;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
public class MainMenuScreen implements Screen {

    private Stage stage;
    private MenuSkin skin;
    private TextButton continueButton;
    private Label statusLabel;
    private String loadedSaveBlob;
    
    private SpriteBatch batch;
    private Texture bgTexture;
    private float bgOffset = 0f;
    private Texture rocketTexture;
    private Animation<TextureRegion> rocketAnim;
    private float stateTime = 0f;
    private Texture logoTexture;

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = new MenuSkin();
        
        batch = new SpriteBatch();
        
        bgTexture = new Texture(Gdx.files.internal("textures/space_bg.png"));
        bgTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        
        rocketTexture = new Texture(Gdx.files.internal("textures/rocket_anim.png"));
        TextureRegion[][] tmp = TextureRegion.split(rocketTexture, rocketTexture.getWidth() / 4, rocketTexture.getHeight());
        TextureRegion[] frames = new TextureRegion[4];
        for (int i = 0; i < 4; i++) frames[i] = tmp[0][i];
        rocketAnim = new Animation<>(0.25f, frames);
        rocketAnim.setPlayMode(Animation.PlayMode.LOOP);

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        root.padBottom(100);
        stage.addActor(root);

        GameManager gm = GameManager.getInstance();
        
        // Create the title image and ensure it maintains aspect ratio
        logoTexture = new Texture(Gdx.files.internal("textures/logo.png"));
        Image title = new Image(logoTexture);
        title.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        
        Label welcome = new Label(
                gm.isLoggedIn() ? "Logged in as " + gm.getCurrentUsername() : "Playing as Guest (offline)",
                skin.smallStyle);

        Table panel = new Table();
        panel.setBackground(skin.panelBackground);
        panel.pad(3);
        panel.defaults().pad(8).width(340).height(48);

        continueButton = new TextButton("Continue", skin.disabledButtonStyle);
        continueButton.setDisabled(true);
        TextButton newGame = new TextButton("New Game", skin.buttonStyle);
        TextButton leaderboard = new TextButton("Leaderboard", skin.buttonStyle);
        TextButton logout = new TextButton("Logout", skin.buttonStyle);
        TextButton quit = new TextButton("Quit", skin.buttonStyle);

        statusLabel = new Label("Checking for save...", skin.smallStyle);
        statusLabel.setAlignment(com.badlogic.gdx.utils.Align.center);

        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (continueButton.isDisabled() || loadedSaveBlob == null) return;
                GameManager.getInstance().playMenuSound();
                GameManager.getInstance().setPendingSaveBlob(loadedSaveBlob);
                GameManager.getInstance().changeScreen(ScreenType.PLAY);
            }
        });
        newGame.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameManager.getInstance().playMenuSound();
                GameManager.getInstance().setPendingSaveBlob(null);
                GameManager.getInstance().changeScreen(ScreenType.PLAY);
            }
        });
        leaderboard.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameManager.getInstance().playMenuSound();
                GameManager.getInstance().changeScreen(ScreenType.LEADERBOARD);
            }
        });
        logout.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameManager.getInstance().playMenuSound();
                GameManager.getInstance().clearCurrentUser();
                GameManager.getInstance().changeScreen(ScreenType.LOGIN);
            }
        });
        quit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameManager.getInstance().playMenuSound();
                Gdx.app.exit();
            }
        });

        panel.add(continueButton).row();
        panel.add(newGame).row();
        panel.add(leaderboard).row();
        panel.add(logout).row();
        panel.add(quit).row();
        panel.add(statusLabel).padTop(16).width(340).height(20).row();

        // Properly size the logo and remove the left padding on the panel so it's perfectly centered
        root.add(title).width(960).height(300).padBottom(-100).row();
        root.add(welcome).padBottom(10).row();
        root.add(panel);

        Gdx.input.setInputProcessor(stage);
        checkForSave();
    }

    private void checkForSave() {
        Long pid = GameManager.getInstance().getCurrentPlayerId();
        if (pid == null) {
            statusLabel.setText("Not logged in.");
            return;
        }
        BackendClient.load(pid, new BackendClient.Callback<LoadResponse>() {
            @Override
            public void onSuccess(LoadResponse result) {
                loadedSaveBlob = result.data;
                continueButton.setStyle(skin.buttonStyle);
                continueButton.setDisabled(false);
                statusLabel.setText("Save available (updated " + shortTime(result.updatedAt) + ")");
            }

            @Override
            public void onError(BackendException ex) {
                if (ex.statusCode == 404) {
                    statusLabel.setText("No save yet — start a new game.");
                } else if (ex.statusCode == 0) {
                    statusLabel.setStyle(skin.errorStyle);
                    statusLabel.setText("Backend unreachable.");
                } else {
                    statusLabel.setStyle(skin.errorStyle);
                    statusLabel.setText("Load failed: " + ex.getMessage());
                }
            }
        });
    }

    private String shortTime(String iso) {
        if (iso == null) return "?";
        int tIdx = iso.indexOf('T');
        if (tIdx < 0) return iso;
        int dotIdx = iso.indexOf('.', tIdx);
        return iso.substring(0, dotIdx > 0 ? dotIdx : Math.min(iso.length(), tIdx + 6));
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        bgOffset += delta * 25f; // scroll speed
        
        Gdx.gl.glClearColor(0.02f, 0.03f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        
        batch.begin();
        batch.draw(bgTexture, 0, 0, (int)bgOffset, (int)(-bgOffset), (int)screenW, (int)screenH);
        
        TextureRegion currentFrame = rocketAnim.getKeyFrame(stateTime);
        
        float rocketBaseW = currentFrame.getRegionWidth();
        float rocketBaseH = currentFrame.getRegionHeight();
        
        // Limit size to size of screen, always full rendered
        float scale = Math.min(screenW / rocketBaseW, screenH / rocketBaseH);
        
        float rocketW = rocketBaseW * scale;
        float rocketH = rocketBaseH * scale;
        
        // Change this value to adjust how far right or left the rocket is
        // 0 = perfectly centered. Positive = right, Negative = left.
        float horizontalOffset = -screenW * 0.25f; 
        
        // Fixed in the middle vertically, centered horizontally + offset
        float rX = (screenW - rocketW) / 2f + horizontalOffset;
        float rY = (screenH - rocketH) / 2f;
        
        batch.draw(currentFrame, rX, rY, rocketW, rocketH);
        batch.end();
        
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (batch != null) {
            batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (batch != null) batch.dispose();
        if (bgTexture != null) bgTexture.dispose();
        if (rocketTexture != null) rocketTexture.dispose();
        if (logoTexture != null) logoTexture.dispose();
    }
}
