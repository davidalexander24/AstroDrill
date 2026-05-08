package com.david.astrodrill;

import com.badlogic.gdx.Game;
import com.david.astrodrill.screen.LoadingScreen;
import com.david.astrodrill.screen.MainMenuScreen;
import com.david.astrodrill.screen.PlayScreen;

public class GameManager {
    private static final GameManager instance = new GameManager();

    private Game game;

    public enum ScreenType {
        LOADING, MAIN_MENU, PLAY
    }

    private GameManager() {}

    public static GameManager getInstance() {
        return instance;
    }

    public void initialize(Game game) {
        this.game = game;
    }

    public void changeScreen(ScreenType screenType) {
        if (game == null) return;

        switch (screenType) {
            case LOADING:
                game.setScreen(new LoadingScreen());
                break;
            case MAIN_MENU:
                game.setScreen(new MainMenuScreen());
                break;
            case PLAY:
                game.setScreen(new PlayScreen());
                break;
        }
    }
}

