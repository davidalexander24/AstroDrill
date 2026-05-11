package com.david.astrodrill.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.entity.Player;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.observer.InventoryObserver;
import com.david.astrodrill.observer.VaultObserver;

import java.util.HashMap;
import java.util.Map;

public class Hud implements InventoryObserver, VaultObserver, Disposable {
    public Stage stage;
    private Table table;
    private Label.LabelStyle labelStyle;
    private Label.LabelStyle warningStyle;
    private BitmapFont font;
    private FreeTypeFontGenerator generator;

    // Battery
    private Label batteryLabel;
    private Label batteryWarningLabel;
    private Player player;

    private Map<Block.BlockType, Integer> currentInventory = new HashMap<>();
    private Map<ItemType, Integer> currentVault = new HashMap<>();

    private static final float LOW_BATTERY_THRESHOLD = 20f;

    private static final Block.BlockType[] INVENTORY_ORDER = {
        Block.BlockType.DIRT, Block.BlockType.STONE, Block.BlockType.COAL_ORE,
        Block.BlockType.COPPER_ORE, Block.BlockType.IRON_ORE, Block.BlockType.BASALT,
        Block.BlockType.GOLD_ORE, Block.BlockType.SILICON_ORE, Block.BlockType.URANIUM_ORE,
        Block.BlockType.OBSIDIAN
    };

    private static final ItemType[] VAULT_ORDER = {
        ItemType.IRON_INGOT, ItemType.COPPER_INGOT, ItemType.IRON_GEAR, ItemType.COPPER_WIRE
    };

    // ── Hub Terminal UI ──────────────────────────────────────────────────
    private Table hubPanel;
    private boolean hubPanelVisible = false;
    private Texture panelBgTexture;
    private Texture buttonUpTexture;
    private Texture buttonOverTexture;
    private Texture buttonDownTexture;
    private BitmapFont hubTitleFont;
    private BitmapFont hubButtonFont;

    public Hud(SpriteBatch batch) {
        stage = new Stage(new ScreenViewport(), batch);

        table = new Table();
        table.top().left();
        table.setFillParent(true);

        // Use FreeType for high-quality font
        generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 20; // Increased size for better readability
        parameter.color = Color.WHITE;
        parameter.borderWidth = 1f; // Subtle border for better contrast
        parameter.borderColor = Color.BLACK;

        font = generator.generateFont(parameter);

        labelStyle = new Label.LabelStyle(font, Color.WHITE);
        warningStyle = new Label.LabelStyle(font, Color.RED);

        batteryLabel = new Label("Battery: 100%", labelStyle);
        batteryWarningLabel = new Label("", warningStyle);

        stage.addActor(table);
        rebuildTable();

        // Build the Hub Terminal panel (initially hidden)
        buildHubPanel();
    }

    private void rebuildTable() {
        table.clear();
        table.top().left();

        table.add(batteryLabel).pad(10).row();
        table.add(batteryWarningLabel).padLeft(10).padBottom(5).row();

        // Inventory
        boolean hasInventory = false;
        for (Integer count : currentInventory.values()) {
            if (count > 0) {
                hasInventory = true;
                break;
            }
        }

        if (hasInventory) {
            table.add(new Label("--- Inventory ---", labelStyle)).padLeft(10).padBottom(5).row();
            for (Block.BlockType type : INVENTORY_ORDER) {
                int count = currentInventory.getOrDefault(type, 0);
                if (count > 0) {
                    table.add(new Label(formatBlockName(type) + ": " + count, labelStyle)).padLeft(10).padBottom(5).row();
                }
            }
        }

        // Vault
        boolean hasVault = false;
        for (Integer count : currentVault.values()) {
            if (count > 0) {
                hasVault = true;
                break;
            }
        }

        if (hasVault) {
            table.add(new Label("--- Vault ---", labelStyle)).padLeft(10).padBottom(5).row();
            for (ItemType type : VAULT_ORDER) {
                int count = currentVault.getOrDefault(type, 0);
                if (count > 0) {
                    table.add(new Label(formatItemName(type) + ": " + count, labelStyle)).padLeft(10).padBottom(5).row();
                }
            }
        }
    }

    // ── Hub Terminal Panel ────────────────────────────────────────────────

    /**
     * Programmatically builds the LanderHub Terminal panel using Pixmap-generated
     * textures. The panel is a semi-transparent overlay on the right side of the
     * screen with clickable upgrade buttons.
     */
    private void buildHubPanel() {
        // Generate semi-transparent dark panel background
        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0.08f, 0.08f, 0.15f, 0.85f);
        bgPixmap.fill();
        panelBgTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        // Generate button textures (up / over / down states)
        buttonUpTexture = createSolidTexture(0.15f, 0.25f, 0.45f, 0.9f);
        buttonOverTexture = createSolidTexture(0.2f, 0.35f, 0.6f, 0.95f);
        buttonDownTexture = createSolidTexture(0.1f, 0.18f, 0.35f, 1f);

        // Fonts for the panel
        FreeTypeFontGenerator.FreeTypeFontParameter titleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParam.size = 24;
        titleParam.color = new Color(0.6f, 0.85f, 1f, 1f); // Cyan-ish
        titleParam.borderWidth = 1.5f;
        titleParam.borderColor = Color.BLACK;
        hubTitleFont = generator.generateFont(titleParam);

        FreeTypeFontGenerator.FreeTypeFontParameter btnParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        btnParam.size = 18;
        btnParam.color = Color.WHITE;
        btnParam.borderWidth = 1f;
        btnParam.borderColor = Color.BLACK;
        hubButtonFont = generator.generateFont(btnParam);

        // Panel table — anchored to the right
        hubPanel = new Table();
        hubPanel.setFillParent(true);
        hubPanel.top().right();
        hubPanel.setVisible(false);

        // Inner container with background
        Table innerPanel = new Table();
        innerPanel.setBackground(new TextureRegionDrawable(new TextureRegion(panelBgTexture)));
        innerPanel.pad(20);
        innerPanel.defaults().pad(8).fillX().uniformX();

        // Title
        Label.LabelStyle titleStyle = new Label.LabelStyle(hubTitleFont, hubTitleFont.getColor());
        Label titleLabel = new Label("Lander Hub Terminal", titleStyle);
        innerPanel.add(titleLabel).padBottom(16).center().row();

        // Separator
        Label.LabelStyle separatorStyle = new Label.LabelStyle(hubButtonFont, new Color(0.4f, 0.5f, 0.6f, 1f));
        innerPanel.add(new Label("━━━━━━━━━━━━━━━━━━━━", separatorStyle)).center().row();

        // Recharge status label
        Label.LabelStyle statusStyle = new Label.LabelStyle(hubButtonFont, new Color(0.3f, 1f, 0.5f, 1f));
        innerPanel.add(new Label("⚡ Recharging Battery...", statusStyle)).padBottom(12).center().row();

        // Build buttons
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.font = hubButtonFont;
        btnStyle.fontColor = Color.WHITE;
        btnStyle.overFontColor = new Color(0.6f, 0.85f, 1f, 1f);
        btnStyle.downFontColor = new Color(0.3f, 0.5f, 0.8f, 1f);
        btnStyle.up = new TextureRegionDrawable(new TextureRegion(buttonUpTexture));
        btnStyle.over = new TextureRegionDrawable(new TextureRegion(buttonOverTexture));
        btnStyle.down = new TextureRegionDrawable(new TextureRegion(buttonDownTexture));

        String[] buttonLabels = {
            "Upgrade Drill",
            "Upgrade Battery",
            "Upgrade Jetpack",
            "Sell Resources",
            "Repair Hull"
        };

        for (String label : buttonLabels) {
            TextButton btn = new TextButton(label, btnStyle);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("HubTerminal", "[ACTION] " + label + " clicked!");
                }
            });
            innerPanel.add(btn).height(42).padBottom(4).row();
        }

        // Footer hint
        Label.LabelStyle hintStyle = new Label.LabelStyle(hubButtonFont, new Color(0.5f, 0.5f, 0.5f, 1f));
        innerPanel.add(new Label("Walk away to close", hintStyle)).padTop(12).center().row();

        hubPanel.add(innerPanel).width(280).padTop(60).padRight(20);
        stage.addActor(hubPanel);
    }

    /** Creates a 1×1 solid-color Texture from RGBA float values. */
    private Texture createSolidTexture(float r, float g, float b, float a) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }

    /**
     * Shows or hides the Hub Terminal panel.
     * Called each frame by PlayScreen based on proximity.
     */
    public void setHubPanelVisible(boolean visible) {
        if (visible == hubPanelVisible) return;
        hubPanelVisible = visible;
        hubPanel.setVisible(visible);
    }

    /** Returns true if the Hub Terminal panel is currently visible. */
    public boolean isHubPanelVisible() {
        return hubPanelVisible;
    }

    // ── Standard Hud Methods ─────────────────────────────────────────────

    private String formatBlockName(Block.BlockType type) {
        String name = type.name().replace("_ORE", "").replace("_", " ").toLowerCase();
        // Capitalize each word
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatItemName(ItemType type) {
        String[] parts = type.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void updateBattery() {
        if (player == null) return;
        float pct = (player.currentBattery / player.maxBattery) * 100f;
        batteryLabel.setText(String.format("Battery: %.0f%%", pct));

        if (pct <= LOW_BATTERY_THRESHOLD) {
            batteryLabel.getStyle().fontColor = Color.RED;
            batteryWarningLabel.setText("!! LOW BATTERY !!");
        } else if (pct <= 50f) {
            batteryLabel.getStyle().fontColor = Color.YELLOW;
            batteryWarningLabel.setText("");
        } else {
            batteryLabel.getStyle().fontColor = Color.GREEN;
            batteryWarningLabel.setText("");
        }
    }

    @Override
    public void onInventoryUpdated(Map<Block.BlockType, Integer> inventory) {
        this.currentInventory.clear();
        this.currentInventory.putAll(inventory);
        rebuildTable();
    }

    @Override
    public void onVaultUpdated(Map<ItemType, Integer> vault) {
        this.currentVault.clear();
        this.currentVault.putAll(vault);
        rebuildTable();
    }

    @Override
    public void dispose() {
        if (font != null) font.dispose();
        if (hubTitleFont != null) hubTitleFont.dispose();
        if (hubButtonFont != null) hubButtonFont.dispose();
        if (generator != null) generator.dispose();
        if (panelBgTexture != null) panelBgTexture.dispose();
        if (buttonUpTexture != null) buttonUpTexture.dispose();
        if (buttonOverTexture != null) buttonOverTexture.dispose();
        if (buttonDownTexture != null) buttonDownTexture.dispose();
        stage.dispose();
    }
}
