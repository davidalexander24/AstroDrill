package com.david.astrodrill;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.audio.Sound;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.machine.AutoMiner;
import com.david.astrodrill.machine.CircuitFab;
import com.david.astrodrill.machine.CoalGenerator;
import com.david.astrodrill.machine.CopperSmelter;
import com.david.astrodrill.machine.FuelMixer;
import com.david.astrodrill.machine.GearAssembler;
import com.david.astrodrill.machine.GoldSmelter;
import com.david.astrodrill.machine.HullPress;
import com.david.astrodrill.machine.IronSmelter;
import com.david.astrodrill.machine.Machine;
import com.david.astrodrill.machine.Refinery;
import com.david.astrodrill.machine.WireAssembler;
import com.david.astrodrill.screen.LeaderboardScreen;
import com.david.astrodrill.screen.LoadingScreen;
import com.david.astrodrill.screen.LoginScreen;
import com.david.astrodrill.screen.MainMenuScreen;
import com.david.astrodrill.screen.PlayScreen;
import com.david.astrodrill.screen.FlightScreen;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.observer.VaultObserver;
import java.util.ArrayList;
import java.util.EnumMap;
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

    // ── Texture cache (lazy-loaded once, persists for app lifetime) ────────
    private boolean texturesLoaded = false;
    private final Map<Block.BlockType, TextureRegion> blockTex = new EnumMap<>(Block.BlockType.class);
    private final Map<ItemType, TextureRegion> itemTex = new EnumMap<>(ItemType.class);
    private Animation<TextureRegion> playerAnimRight, playerAnimLeft;
    private TextureRegion hubTex;
    private final Map<Class<? extends Machine>, TextureRegion> machineTex = new HashMap<>();
    private final Map<Class<? extends Machine>, TextureRegion> machineOffTex = new HashMap<>();
    private final Map<Class<? extends Machine>, Animation<TextureRegion>> machineAnim = new HashMap<>();
    private final List<Texture> ownedTextures = new ArrayList<>();
    private Sound menuSound;

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

    private PlayScreen currentPlayScreen;

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
                if (currentPlayScreen != null) {
                    currentPlayScreen.dispose();
                    currentPlayScreen = null;
                }
                game.setScreen(new MainMenuScreen());
                break;
            case LEADERBOARD:
                game.setScreen(new LeaderboardScreen());
                break;
            case PLAY:
                if (currentPlayScreen == null) {
                    currentPlayScreen = new PlayScreen();
                }
                game.setScreen(currentPlayScreen);
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

    // ── Texture loading ────────────────────────────────────────────────────

    public void ensureTexturesLoaded() {
        if (texturesLoaded) return;
        texturesLoaded = true;

        // Blocks
        loadBlock(Block.BlockType.DIRT,         "textures/blocks/dirt.png");
        loadBlock(Block.BlockType.STONE,        "textures/blocks/stone.png");
        loadBlock(Block.BlockType.COAL_ORE,     "textures/blocks/coal_ore.png");
        loadBlock(Block.BlockType.COPPER_ORE,   "textures/blocks/copper_ore.png");
        loadBlock(Block.BlockType.IRON_ORE,     "textures/blocks/iron_ore.png");
        loadBlock(Block.BlockType.BASALT,       "textures/blocks/basalt.png");
        loadBlock(Block.BlockType.GOLD_ORE,     "textures/blocks/gold_ore.png");
        loadBlock(Block.BlockType.SILICON_ORE,  "textures/blocks/silicon_ore.png");
        loadBlock(Block.BlockType.OBSIDIAN,     "textures/blocks/obsidian.png");
        loadBlock(Block.BlockType.URANIUM_ORE,  "textures/blocks/uranium_ore.png");
        loadBlock(Block.BlockType.BEDROCK,      "textures/blocks/bedrock.png");

        // Refined items
        loadItem(ItemType.COPPER_INGOT,   "textures/items/copper_ingot.png");
        loadItem(ItemType.IRON_INGOT,     "textures/items/iron_ingot.png");
        loadItem(ItemType.GOLD_INGOT,     "textures/items/gold_ingot.png");
        loadItem(ItemType.SILICON_WAFER,  "textures/items/silicon_wafer.png");
        loadItem(ItemType.IRON_GEAR,      "textures/items/iron_gear.png");
        loadItem(ItemType.COPPER_WIRE,    "textures/items/copper_wire.png");
        loadItem(ItemType.CIRCUIT_BOARD,  "textures/items/circuit_board.png");
        loadItem(ItemType.HULL_PLATING,   "textures/items/hull_plating.png");
        loadItem(ItemType.ROCKET_FUEL,    "textures/items/rocket_fuel.png");

        // Reuse aliases: raw ores share their block texture, placeable blocks too
        aliasItem(ItemType.RAW_COAL,     Block.BlockType.COAL_ORE);
        aliasItem(ItemType.RAW_COPPER,   Block.BlockType.COPPER_ORE);
        aliasItem(ItemType.RAW_IRON,     Block.BlockType.IRON_ORE);
        aliasItem(ItemType.RAW_GOLD,     Block.BlockType.GOLD_ORE);
        aliasItem(ItemType.RAW_SILICON,  Block.BlockType.SILICON_ORE);
        aliasItem(ItemType.RAW_URANIUM,  Block.BlockType.URANIUM_ORE);
        aliasItem(ItemType.RAW_OBSIDIAN, Block.BlockType.OBSIDIAN);
        aliasItem(ItemType.DIRT,         Block.BlockType.DIRT);
        aliasItem(ItemType.STONE,        Block.BlockType.STONE);

        // Player rover animation (faces right; flipped variant for facing left)
        if (Gdx.files.internal("textures/entities/player_anim.png").exists()) {
            Texture t = new Texture(Gdx.files.internal("textures/entities/player_anim.png"));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            ownedTextures.add(t);
            TextureRegion[][] tmp = TextureRegion.split(t, t.getWidth() / 2, t.getHeight() / 2);
            TextureRegion[] framesRight = new TextureRegion[4];
            TextureRegion[] framesLeft = new TextureRegion[4];
            int index = 0;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    framesRight[index] = new TextureRegion(tmp[i][j]);
                    framesLeft[index] = new TextureRegion(tmp[i][j]);
                    framesLeft[index].flip(true, false);
                    index++;
                }
            }
            playerAnimRight = new Animation<TextureRegion>(0.15f, framesRight);
            playerAnimRight.setPlayMode(Animation.PlayMode.LOOP);
            playerAnimLeft = new Animation<TextureRegion>(0.15f, framesLeft);
            playerAnimLeft.setPlayMode(Animation.PlayMode.LOOP);
        } else if (Gdx.files.internal("textures/entities/player.png").exists()) {
            // Fallback for older non-animated player
            Texture t = new Texture(Gdx.files.internal("textures/entities/player.png"));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            ownedTextures.add(t);
            TextureRegion pr = new TextureRegion(t);
            TextureRegion pl = new TextureRegion(t);
            pl.flip(true, false);
            playerAnimRight = new Animation<TextureRegion>(1f, new TextureRegion[]{pr});
            playerAnimLeft = new Animation<TextureRegion>(1f, new TextureRegion[]{pl});
        }

        // LanderHub sprite (falls back to gray rect in PlayScreen if missing).
        if (Gdx.files.internal("textures/entities/lander_hub.png").exists()) {
            Texture t = new Texture(Gdx.files.internal("textures/entities/lander_hub.png"));
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            ownedTextures.add(t);
            hubTex = new TextureRegion(t);
        }

        // Machine sprites (keyed by class; PlayScreen falls back to colored rect + symbol if missing).
        loadMachine(AutoMiner.class,      "textures/machines/auto_miner.png");
        loadMachine(CoalGenerator.class,  "textures/machines/coal_generator.png");
        loadMachine(IronSmelter.class,    "textures/machines/iron_smelter.png");
        loadMachine(CopperSmelter.class,  "textures/machines/copper_smelter.png");
        loadMachine(GoldSmelter.class,    "textures/machines/gold_smelter.png");
        loadMachine(GearAssembler.class,  "textures/machines/gear_assembler.png");
        loadMachine(WireAssembler.class,  "textures/machines/wire_assembler.png");
        loadMachine(Refinery.class,       "textures/machines/refinery.png");
        loadMachine(CircuitFab.class,     "textures/machines/circuit_fab.png");
        loadMachine(FuelMixer.class,      "textures/machines/fuel_mixer.png");
        loadMachine(HullPress.class,      "textures/machines/hull_press.png");

        // Machine "off" sprites — derived from the on-state PNGs via make_off_variants.py.
        // Identical silhouette, glow features dimmed.
        loadMachineOff(AutoMiner.class,      "textures/machines/auto_miner_off.png");
        loadMachineOff(CoalGenerator.class,  "textures/machines/coal_generator_off.png");
        loadMachineOff(IronSmelter.class,    "textures/machines/iron_smelter_off.png");
        loadMachineOff(CopperSmelter.class,  "textures/machines/copper_smelter_off.png");
        loadMachineOff(GoldSmelter.class,    "textures/machines/gold_smelter_off.png");
        loadMachineOff(GearAssembler.class,  "textures/machines/gear_assembler_off.png");
        loadMachineOff(WireAssembler.class,  "textures/machines/wire_assembler_off.png");
        loadMachineOff(Refinery.class,       "textures/machines/refinery_off.png");
        loadMachineOff(CircuitFab.class,     "textures/machines/circuit_fab_off.png");
        loadMachineOff(HullPress.class,      "textures/machines/hull_press_off.png");

        // Alias machine items to their placed textures so they appear in inventory/hotbar
        aliasMachineItem(ItemType.AUTO_MINER,      AutoMiner.class);
        aliasMachineItem(ItemType.COAL_GENERATOR,  CoalGenerator.class);
        aliasMachineItem(ItemType.IRON_SMELTER,    IronSmelter.class);
        aliasMachineItem(ItemType.COPPER_SMELTER,  CopperSmelter.class);
        aliasMachineItem(ItemType.GOLD_SMELTER,    GoldSmelter.class);
        aliasMachineItem(ItemType.GEAR_ASSEMBLER,  GearAssembler.class);
        aliasMachineItem(ItemType.WIRE_ASSEMBLER,  WireAssembler.class);
        aliasMachineItem(ItemType.REFINERY,        Refinery.class);
        aliasMachineItem(ItemType.CIRCUIT_FAB,     CircuitFab.class);
        aliasMachineItem(ItemType.FUEL_MIXER,      FuelMixer.class);
        aliasMachineItem(ItemType.HULL_PRESS,      HullPress.class);

        if (Gdx.files.internal("sounds/Menu_Pressed.wav").exists()) {
            menuSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Menu_Pressed.wav"));
        }
    }

    private void loadBlock(Block.BlockType type, String path) {
        if (!Gdx.files.internal(path).exists()) return;
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        ownedTextures.add(t);
        blockTex.put(type, new TextureRegion(t));
    }

    private void loadItem(ItemType type, String path) {
        if (!Gdx.files.internal(path).exists()) return;
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        ownedTextures.add(t);
        itemTex.put(type, new TextureRegion(t));
    }

    private void aliasItem(ItemType item, Block.BlockType block) {
        TextureRegion src = blockTex.get(block);
        if (src != null) itemTex.put(item, src);
    }

    private void aliasMachineItem(ItemType item, Class<? extends Machine> cls) {
        TextureRegion src = machineTex.get(cls);
        if (src != null) itemTex.put(item, src);
    }

    private void loadMachine(Class<? extends Machine> cls, String path) {
        if (!Gdx.files.internal(path).exists()) return;
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        ownedTextures.add(t);
        
        String animPath = path.replace(".png", "_anim.png");
        if (Gdx.files.internal(animPath).exists()) {
            Texture at = new Texture(Gdx.files.internal(animPath));
            at.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            ownedTextures.add(at);
            TextureRegion[][] tmp = TextureRegion.split(at, at.getWidth() / 4, at.getHeight());
            TextureRegion[] frames = new TextureRegion[4];
            for (int i = 0; i < 4; i++) frames[i] = tmp[0][i];
            Animation<TextureRegion> anim = new Animation<>(0.25f, frames);
            anim.setPlayMode(Animation.PlayMode.LOOP);
            machineAnim.put(cls, anim);
            machineTex.put(cls, frames[0]);
        } else {
            machineTex.put(cls, new TextureRegion(t));
        }
    }

    private void loadMachineOff(Class<? extends Machine> cls, String path) {
        if (!Gdx.files.internal(path).exists()) return;
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        ownedTextures.add(t);
        machineOffTex.put(cls, new TextureRegion(t));
    }

    public TextureRegion getBlockTex(Block.BlockType type) { return blockTex.get(type); }
    public TextureRegion getItemTex(ItemType type) { return itemTex.get(type); }
    public TextureRegion getPlayerFrame(boolean facingLeft, float stateTime, boolean isMoving) {
        Animation<TextureRegion> anim = facingLeft ? playerAnimLeft : playerAnimRight;
        if (anim == null) return null;
        if (isMoving) {
            return anim.getKeyFrame(stateTime, true);
        } else {
            return anim.getKeyFrame(0f);
        }
    }
    public TextureRegion getHubTex() { return hubTex; }
    public TextureRegion getMachineTex(Machine m) {
        return m == null ? null : machineTex.get(m.getClass());
    }
    public TextureRegion getMachineFrame(Machine m, float stateTime) {
        if (m == null) return null;
        Animation<TextureRegion> anim = machineAnim.get(m.getClass());
        if (anim != null) {
            return anim.getKeyFrame(stateTime);
        }
        return machineTex.get(m.getClass());
    }
    public TextureRegion getMachineOffTex(Machine m) {
        return m == null ? null : machineOffTex.get(m.getClass());
    }

    public void playMenuSound() {
        if (menuSound != null) {
            menuSound.play(sfxVolume);
        }
    }

    public void disposeTextures() {
        for (Texture t : ownedTextures) t.dispose();
        ownedTextures.clear();
        blockTex.clear();
        itemTex.clear();
        machineTex.clear();
        machineOffTex.clear();
        playerAnimRight = null;
        playerAnimLeft = null;
        hubTex = null;
        if (menuSound != null) {
            menuSound.dispose();
            menuSound = null;
        }
        texturesLoaded = false;
    }
}
