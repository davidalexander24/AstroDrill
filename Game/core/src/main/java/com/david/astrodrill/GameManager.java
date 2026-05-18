package com.david.astrodrill;

import com.badlogic.gdx.Game;
import com.david.astrodrill.screen.LoadingScreen;
import com.david.astrodrill.screen.MainMenuScreen;
import com.david.astrodrill.screen.PlayScreen;
import com.david.astrodrill.screen.FlightScreen;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.observer.VaultObserver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameManager {
    private static final GameManager instance = new GameManager();

    // Dev toggle: set true to seed the vault for testing the launch path quickly.
    private static final boolean DEV_UNLIMITED_RESOURCES = false;
    private static final int DEV_RESOURCE_COUNT = 99999;

    private Game game;
    private Map<ItemType, Integer> globalVault = new HashMap<>();
    private List<VaultObserver> observers = new ArrayList<>();

    public enum ScreenType {
        LOADING, MAIN_MENU, PLAY, FLIGHT
    }

    private GameManager() {
        if (DEV_UNLIMITED_RESOURCES) {
            seedDevResources();
        }
    }

    public static GameManager getInstance() {
        return instance;
    }

    public void addObserver(VaultObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            observer.onVaultUpdated(globalVault);
        }
    }

    private void notifyObservers() {
        for (VaultObserver observer : observers) {
            observer.onVaultUpdated(globalVault);
        }
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
            case FLIGHT:
                game.setScreen(new FlightScreen());
                break;
        }
    }

    public void addItems(ItemType type, int amount) {
        if (DEV_UNLIMITED_RESOURCES) {
            globalVault.put(type, DEV_RESOURCE_COUNT);
            notifyObservers();
            return;
        }
        globalVault.put(type, globalVault.getOrDefault(type, 0) + amount);
        notifyObservers();
    }

    public boolean hasItems(ItemType type, int amount) {
        if (DEV_UNLIMITED_RESOURCES) return true;
        return globalVault.getOrDefault(type, 0) >= amount;
    }

    public void consumeItems(ItemType type, int amount) {
        if (DEV_UNLIMITED_RESOURCES) return;
        if (hasItems(type, amount)) {
            globalVault.put(type, globalVault.get(type) - amount);
            notifyObservers();
        }
    }

    public int getItemCount(ItemType type) {
        if (DEV_UNLIMITED_RESOURCES) return DEV_RESOURCE_COUNT;
        return globalVault.getOrDefault(type, 0);
    }

    private void seedDevResources() {
        for (ItemType type : ItemType.values()) {
            if (type != ItemType.DECONSTRUCT_TOOL) {
                globalVault.put(type, DEV_RESOURCE_COUNT);
            }
        }
    }
}
