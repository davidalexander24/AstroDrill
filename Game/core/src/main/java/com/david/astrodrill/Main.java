package com.david.astrodrill;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {

    @Override
    public void create() {
        GameManager.getInstance().initialize(this);
        GameManager.getInstance().changeScreen(GameManager.ScreenType.LOGIN);
    }

    @Override
    public void render() {
        super.render();
    }
}
