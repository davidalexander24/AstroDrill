package com.david.astrodrill;

import com.badlogic.gdx.Game;
import com.david.astrodrill.screen.LeaderboardScreen;
import com.david.astrodrill.screen.LoadingScreen;
import com.david.astrodrill.screen.LoginScreen;
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

    // Dev toggle: runtime-mutable so the HUD button can flip it.
    // When enabled, getItemCount/hasItems short-circuit to DEV_RESOURCE_COUNT and
    // consumeItems is a no-op.
    private boolean devUnlimited = false;
    private static final int DEV_RESOURCE_COUNT = 99999;

    private float sfxVolume = 1.0f;
    public float getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(float volume) { this.sfxVolume = volume; }

    private Game game;
    private Map<ItemType, Integer> globalVault = new HashMap<>();
    private List<VaultObserver> observers = new ArrayList<>();

    private Long currentPlayerId;
    private String currentUsername;

    public enum ScreenType {
        LOADING, LOGIN, MAIN_MENU, LEADERBOARD, PLAY, FLIGHT
    }

    private GameManager() { }

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
            case LOGIN:
                game.setScreen(new LoginScreen());
                break;
            case MAIN_MENU:
                game.setScreen(new MainMenuScreen());
                break;
            case LEADERBOARD:
                game.setScreen(new LeaderboardScreen());
                break;
            case PLAY:
                game.setScreen(new PlayScreen());
                break;
            case FLIGHT:
                game.setScreen(new FlightScreen());
                break;
        }
    }

    public Long getCurrentPlayerId() { return currentPlayerId; }
    public String getCurrentUsername() { return currentUsername; }
    public boolean isLoggedIn() { return currentPlayerId != null; }

    public void setCurrentUser(Long playerId, String username) {
        this.currentPlayerId = playerId;
        this.currentUsername = username;
    }

    public void clearCurrentUser() {
        this.currentPlayerId = null;
        this.currentUsername = null;
        this.globalVault.clear();
        notifyObservers();
    }

    public Game getGame() { return game; }
    public Map<ItemType, Integer> getGlobalVault() { return globalVault; }

    /**
     * One-shot slot for handing a loaded save blob from MainMenuScreen to a
     * freshly-constructed PlayScreen. Consumed (cleared) by PlayScreen.show().
     * Null means "start a new world".
     */
    private String pendingSaveBlob;
    public String consumePendingSaveBlob() {
        String s = pendingSaveBlob;
        pendingSaveBlob = null;
        return s;
    }
    public void setPendingSaveBlob(String blob) { this.pendingSaveBlob = blob; }
    public boolean hasPendingSaveBlob() { return pendingSaveBlob != null; }

    public void replaceGlobalVault(Map<ItemType, Integer> vault) {
        this.globalVault.clear();
        if (vault != null) this.globalVault.putAll(vault);
        notifyObservers();
    }

    public void addItems(ItemType type, int amount) {
        if (devUnlimited) {
            globalVault.put(type, DEV_RESOURCE_COUNT);
            notifyObservers();
            return;
        }
        globalVault.put(type, globalVault.getOrDefault(type, 0) + amount);
        notifyObservers();
    }

    public boolean hasItems(ItemType type, int amount) {
        if (devUnlimited) return true;
        return globalVault.getOrDefault(type, 0) >= amount;
    }

    public void consumeItems(ItemType type, int amount) {
        if (devUnlimited) return;
        if (hasItems(type, amount)) {
            globalVault.put(type, globalVault.get(type) - amount);
            notifyObservers();
        }
    }

    public int getItemCount(ItemType type) {
        if (devUnlimited) return DEV_RESOURCE_COUNT;
        return globalVault.getOrDefault(type, 0);
    }

    public boolean isDevUnlimited() { return devUnlimited; }

    /**
     * Toggles unlimited-resource dev mode. Turning it ON seeds the vault with a large
     * count of every item. Turning it OFF clears the vault so the previous dev seed
     * doesn't carry into real play.
     */
    public void setDevUnlimited(boolean enabled) {
        this.devUnlimited = enabled;
        if (enabled) {
            seedDevResources();
        } else {
            globalVault.clear();
        }
        notifyObservers();
    }

    private void seedDevResources() {
        for (ItemType type : ItemType.values()) {
            if (type != ItemType.DECONSTRUCT_TOOL) {
                globalVault.put(type, DEV_RESOURCE_COUNT);
            }
        }
    }
}
