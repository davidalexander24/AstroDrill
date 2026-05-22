package com.david.astrodrill.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.GameManager.ScreenType;
import com.david.astrodrill.network.BackendClient;
import com.david.astrodrill.network.BackendException;
import com.david.astrodrill.network.dto.LoginRequest;
import com.david.astrodrill.network.dto.LoginResponse;
import com.david.astrodrill.network.dto.RegisterRequest;
import com.david.astrodrill.ui.MenuSkin;

public class LoginScreen implements Screen {

    public static final String PREFS_NAME = "astrodrill";
    public static final String PREFS_LAST_USERNAME = "lastUsername";

    private Stage stage;
    private MenuSkin skin;

    private TextField usernameField;
    private TextField passwordField;
    private CheckBox rememberMe;
    private Label statusLabel;
    private TextButton loginButton;
    private TextButton registerButton;
    private boolean busy = false;
    private Texture logoTexture;

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        skin = new MenuSkin();

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String lastUsername = prefs.getString(PREFS_LAST_USERNAME, "");

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        logoTexture = new Texture(Gdx.files.internal("textures/logo.png"));
        Image title = new Image(logoTexture);
        title.setScaling(Scaling.fit);
        Label subtitle = new Label("Sign in to your colony", skin.smallStyle);

        Table panel = new Table();
        panel.setBackground(skin.panelBackground);
        panel.pad(24);
        panel.defaults().pad(6).left();

        usernameField = new TextField(lastUsername, skin.textFieldStyle);
        usernameField.setMessageText("Username");

        passwordField = new TextField("", skin.textFieldStyle);
        passwordField.setMessageText("Password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');

        rememberMe = new CheckBox("  Remember username", skin.checkBoxStyle);
        rememberMe.setChecked(!lastUsername.isEmpty());

        statusLabel = new Label("", skin.labelStyle);
        statusLabel.setWrap(true);

        loginButton = new TextButton("Log In", skin.buttonStyle);
        registerButton = new TextButton("Register", skin.buttonStyle);

        loginButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { GameManager.getInstance().playMenuSound(); attemptLogin(); }
        });
        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) { GameManager.getInstance().playMenuSound(); attemptRegister(); }
        });

        panel.add(new Label("Username", skin.labelStyle)).row();
        panel.add(usernameField).width(320).height(36).row();
        panel.add(new Label("Password", skin.labelStyle)).padTop(10).row();
        panel.add(passwordField).width(320).height(36).row();
        panel.add(rememberMe).padTop(12).row();

        Table buttonRow = new Table();
        buttonRow.add(loginButton).width(150).height(40).padRight(8);
        buttonRow.add(registerButton).width(150).height(40);
        panel.add(buttonRow).padTop(16).row();

        panel.add(statusLabel).width(320).padTop(12).row();

        root.add(title).width(960).height(300).padBottom(4).row();
        root.add(subtitle).padBottom(20).row();
        root.add(panel);

        stage.setKeyboardFocus(lastUsername.isEmpty() ? usernameField : passwordField);
        Gdx.input.setInputProcessor(stage);
    }

    private void attemptLogin() {
        if (busy) return;
        if (!validate()) return;
        setBusy(true, "Logging in...");
        BackendClient.login(new LoginRequest(usernameField.getText().trim(), passwordField.getText()),
                authCallback());
    }

    private void attemptRegister() {
        if (busy) return;
        if (!validate()) return;
        if (passwordField.getText().length() < 6) {
            statusLabel.setStyle(skin.errorStyle);
            statusLabel.setText("Password must be at least 6 characters.");
            return;
        }
        setBusy(true, "Registering...");
        BackendClient.register(new RegisterRequest(usernameField.getText().trim(), passwordField.getText()),
                authCallback());
    }

    private boolean validate() {
        if (usernameField.getText().trim().isEmpty() || passwordField.getText().isEmpty()) {
            statusLabel.setStyle(skin.errorStyle);
            statusLabel.setText("Username and password are required.");
            return false;
        }
        return true;
    }

    private BackendClient.Callback<LoginResponse> authCallback() {
        return new BackendClient.Callback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse result) {
                GameManager.getInstance().setCurrentUser(result.playerId, result.username);
                Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
                if (rememberMe.isChecked()) {
                    prefs.putString(PREFS_LAST_USERNAME, result.username);
                } else {
                    prefs.remove(PREFS_LAST_USERNAME);
                }
                prefs.flush();
                GameManager.getInstance().changeScreen(ScreenType.MAIN_MENU);
            }

            @Override
            public void onError(BackendException ex) {
                setBusy(false, null);
                statusLabel.setStyle(skin.errorStyle);
                if (ex.statusCode == 0) {
                    statusLabel.setText("Cannot reach backend at " + BackendClient.baseUrl);
                } else {
                    statusLabel.setText(ex.getMessage());
                }
            }
        };
    }

    private void setBusy(boolean b, String message) {
        busy = b;
        loginButton.setDisabled(b);
        registerButton.setDisabled(b);
        if (message != null) {
            statusLabel.setStyle(skin.labelStyle);
            statusLabel.setText(message);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.03f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && !busy) {
            attemptLogin();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            if (stage.getKeyboardFocus() == usernameField) {
                stage.setKeyboardFocus(passwordField);
            } else {
                stage.setKeyboardFocus(usernameField);
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (logoTexture != null) logoTexture.dispose();
    }
}
