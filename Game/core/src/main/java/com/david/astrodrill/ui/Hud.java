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
import com.badlogic.gdx.utils.Align;
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
        ItemType.RAW_IRON, ItemType.RAW_COPPER, ItemType.RAW_COAL,
        ItemType.DIRT, ItemType.STONE,
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

    // ── Hotbar UI ────────────────────────────────────────────────────────
    private Table hotbarWrapper;
    private Table hotbarTable;
    private Label hotbarItemNameLabel;
    private final Label[] slotLabels = new Label[Player.HOTBAR_SLOTS];
    private final Label[] slotNumberLabels = new Label[Player.HOTBAR_SLOTS];
    private final Table[] slotCells = new Table[Player.HOTBAR_SLOTS];

    private Texture slotBgTexture;
    private Texture slotActiveBgTexture;
    private BitmapFont hotbarFont;
    private BitmapFont hotbarSmallFont;
    private BitmapFont hotbarNameFont;

    // ── Banking Popup ────────────────────────────────────────────────────
    private Label bankingPopupLabel;
    private float bankingPopupTimer = 0f;
    private BitmapFont bankingFont;

    public Hud(SpriteBatch batch) {
        stage = new Stage(new ScreenViewport(), batch);

        table = new Table();
        table.top().left();
        table.setFillParent(true);

        generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 20;
        parameter.color = Color.WHITE;
        parameter.borderWidth = 1f;
        parameter.borderColor = Color.BLACK;
        font = generator.generateFont(parameter);

        labelStyle = new Label.LabelStyle(font, Color.WHITE);
        warningStyle = new Label.LabelStyle(font, Color.RED);

        batteryLabel = new Label("Battery: 100%", labelStyle);
        batteryWarningLabel = new Label("", warningStyle);

        table.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly);
        stage.addActor(table);
        rebuildTable();
        buildHubPanel();
        buildHotbarUI();
        buildBankingPopup();
    }

    private void rebuildTable() {
        table.clear();
        table.top().left();

        table.add(batteryLabel).pad(10).row();
        table.add(batteryWarningLabel).padLeft(10).padBottom(5).row();

        boolean hasInventory = false;
        for (Integer count : currentInventory.values()) {
            if (count > 0) { hasInventory = true; break; }
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

        boolean hasVault = false;
        for (Integer count : currentVault.values()) {
            if (count > 0) { hasVault = true; break; }
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

    private void buildHubPanel() {
        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0.08f, 0.08f, 0.15f, 0.85f);
        bgPixmap.fill();
        panelBgTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        buttonUpTexture = createSolidTexture(0.15f, 0.25f, 0.45f, 0.9f);
        buttonOverTexture = createSolidTexture(0.2f, 0.35f, 0.6f, 0.95f);
        buttonDownTexture = createSolidTexture(0.1f, 0.18f, 0.35f, 1f);

        FreeTypeFontGenerator.FreeTypeFontParameter titleParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        titleParam.size = 24;
        titleParam.color = new Color(0.6f, 0.85f, 1f, 1f);
        titleParam.borderWidth = 1.5f;
        titleParam.borderColor = Color.BLACK;
        hubTitleFont = generator.generateFont(titleParam);

        FreeTypeFontGenerator.FreeTypeFontParameter btnParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        btnParam.size = 18;
        btnParam.color = Color.WHITE;
        btnParam.borderWidth = 1f;
        btnParam.borderColor = Color.BLACK;
        hubButtonFont = generator.generateFont(btnParam);

        hubPanel = new Table();
        hubPanel.setFillParent(true);
        hubPanel.top().right();
        hubPanel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly);
        hubPanel.setVisible(false);

        Table innerPanel = new Table();
        innerPanel.setBackground(new TextureRegionDrawable(new TextureRegion(panelBgTexture)));
        innerPanel.pad(20);
        innerPanel.defaults().pad(8).fillX().uniformX();

        Label.LabelStyle titleStyle = new Label.LabelStyle(hubTitleFont, hubTitleFont.getColor());
        innerPanel.add(new Label("Lander Hub Terminal", titleStyle)).padBottom(16).center().row();

        Label.LabelStyle separatorStyle = new Label.LabelStyle(hubButtonFont, new Color(0.4f, 0.5f, 0.6f, 1f));
        innerPanel.add(new Label("━━━━━━━━━━━━━━━━━━━━", separatorStyle)).center().row();

        Label.LabelStyle statusStyle = new Label.LabelStyle(hubButtonFont, new Color(0.3f, 1f, 0.5f, 1f));
        innerPanel.add(new Label("⚡ Recharging Battery...", statusStyle)).padBottom(12).center().row();

        TextButton.TextButtonStyle tbs = new TextButton.TextButtonStyle();
        tbs.font = hubButtonFont;
        tbs.fontColor = Color.WHITE;
        tbs.overFontColor = new Color(0.6f, 0.85f, 1f, 1f);
        tbs.downFontColor = new Color(0.3f, 0.5f, 0.8f, 1f);
        tbs.up = new TextureRegionDrawable(new TextureRegion(buttonUpTexture));
        tbs.over = new TextureRegionDrawable(new TextureRegion(buttonOverTexture));
        tbs.down = new TextureRegionDrawable(new TextureRegion(buttonDownTexture));

        String[] buttonLabels = {
            "Upgrade Drill", "Upgrade Battery", "Upgrade Jetpack", "Sell Resources", "Repair Hull"
        };
        for (String label : buttonLabels) {
            TextButton btn = new TextButton(label, tbs);
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("HubTerminal", "[ACTION] " + label + " clicked!");
                }
            });
            innerPanel.add(btn).height(42).padBottom(4).row();
        }

        Label.LabelStyle hintStyle = new Label.LabelStyle(hubButtonFont, new Color(0.5f, 0.5f, 0.5f, 1f));
        innerPanel.add(new Label("Walk away to close", hintStyle)).padTop(12).center().row();

        hubPanel.add(innerPanel).width(280).padTop(60).padRight(20);
        stage.addActor(hubPanel);
    }

    // ── Hotbar UI ────────────────────────────────────────────────────────

    private void buildHotbarUI() {
        slotBgTexture = createSolidTexture(0.12f, 0.12f, 0.18f, 0.8f);
        slotActiveBgTexture = createSolidTexture(0.25f, 0.55f, 0.9f, 0.9f);

        FreeTypeFontGenerator.FreeTypeFontParameter slotParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        slotParam.size = 16;
        slotParam.color = Color.WHITE;
        slotParam.borderWidth = 1f;
        slotParam.borderColor = Color.BLACK;
        hotbarFont = generator.generateFont(slotParam);

        FreeTypeFontGenerator.FreeTypeFontParameter smallParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        smallParam.size = 11;
        smallParam.color = new Color(0.7f, 0.7f, 0.7f, 1f);
        smallParam.borderWidth = 0.5f;
        smallParam.borderColor = Color.BLACK;
        hotbarSmallFont = generator.generateFont(smallParam);

        FreeTypeFontGenerator.FreeTypeFontParameter nameParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        nameParam.size = 16;
        nameParam.color = new Color(0.85f, 0.9f, 1f, 1f);
        nameParam.borderWidth = 1f;
        nameParam.borderColor = Color.BLACK;
        hotbarNameFont = generator.generateFont(nameParam);

        hotbarWrapper = new Table();
        hotbarWrapper.setFillParent(true);
        hotbarWrapper.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly);
        hotbarWrapper.add().expandY(); // Push everything down
        hotbarWrapper.row();

        Label.LabelStyle nameStyle = new Label.LabelStyle(hotbarNameFont, hotbarNameFont.getColor());
        hotbarItemNameLabel = new Label("", nameStyle);
        hotbarItemNameLabel.setAlignment(Align.center);
        hotbarItemNameLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        hotbarWrapper.add(hotbarItemNameLabel).padBottom(4).row();

        hotbarTable = new Table();
        hotbarTable.setBackground(new TextureRegionDrawable(new TextureRegion(
            createSolidTexture(0.05f, 0.05f, 0.1f, 0.7f)
        )));
        hotbarTable.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly);
        hotbarTable.pad(4);

        Label.LabelStyle slotStyle = new Label.LabelStyle(hotbarFont, Color.WHITE);
        Label.LabelStyle numStyle = new Label.LabelStyle(hotbarSmallFont, hotbarSmallFont.getColor());

        for (int i = 0; i < Player.HOTBAR_SLOTS; i++) {
            final int slotIndex = i;
            Table slotCell = new Table();
            slotCell.setBackground(new TextureRegionDrawable(new TextureRegion(slotBgTexture)));

            Label numLabel = new Label(String.valueOf(i + 1), numStyle);
            numLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            slotNumberLabels[i] = numLabel;

            Label itemLabel = new Label("", slotStyle);
            itemLabel.setAlignment(Align.center);
            itemLabel.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
            slotLabels[i] = itemLabel;

            slotCell.add(numLabel).top().left().expandX().padLeft(3).padTop(1).row();
            slotCell.add(itemLabel).expand().center().row();

            slotCells[i] = slotCell;

            // Instant slot selection on touchDown
            slotCell.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
            slotCell.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
                @Override
                public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int button) {
                    Gdx.app.log("Hotbar", "Slot " + slotIndex + " clicked at " + x + ", " + y);
                    if (player != null) {
                        player.setActiveSlot(slotIndex);
                    }
                    return true;
                }
            });

            hotbarTable.add(slotCell).size(54, 54).pad(1);
        }

        hotbarWrapper.add(hotbarTable);
        stage.addActor(hotbarWrapper);
    }

    // ── Banking Popup ────────────────────────────────────────────────────

    private void buildBankingPopup() {
        FreeTypeFontGenerator.FreeTypeFontParameter bpParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        bpParam.size = 18;
        bpParam.color = new Color(0.3f, 1f, 0.5f, 1f);
        bpParam.borderWidth = 1.5f;
        bpParam.borderColor = Color.BLACK;
        bankingFont = generator.generateFont(bpParam);

        Table popupWrapper = new Table();
        popupWrapper.setFillParent(true);
        popupWrapper.center().center();
        popupWrapper.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);

        Label.LabelStyle popupStyle = new Label.LabelStyle(bankingFont, bankingFont.getColor());
        bankingPopupLabel = new Label("", popupStyle);
        bankingPopupLabel.setAlignment(Align.center);
        bankingPopupLabel.setVisible(false);

        popupWrapper.add(bankingPopupLabel);
        stage.addActor(popupWrapper);
    }

    /** Shows a banking notification popup that fades after a few seconds. */
    public void showBankingPopup(String text) {
        bankingPopupLabel.setText(text);
        bankingPopupLabel.setVisible(true);
        bankingPopupTimer = 2.5f;
    }

    /**
     * Called every frame. Updates hotbar visuals, banking popup timer.
     */
    public void updateHotbar(float delta) {
        if (player == null) return;

        // Update banking popup fade
        if (bankingPopupTimer > 0) {
            bankingPopupTimer -= delta;
            if (bankingPopupTimer <= 0) {
                bankingPopupLabel.setVisible(false);
            }
        }

        // Rebuild player hotbar from inventory state
        player.rebuildHotbar();

        int active = player.activeSlot;
        for (int i = 0; i < Player.HOTBAR_SLOTS; i++) {
            ItemType item = player.hotbar[i];
            String symbol = getSlotSymbol(item);

            // Show item count for placeable blocks
            if (item != null && item != ItemType.DECONSTRUCT_TOOL) {
                Block.BlockType bt = Player.itemTypeToBlockType(item);
                if (bt != null) {
                    int count = player.getTotalResourceCount(bt);
                    if (count > 0) {
                        symbol += "\n" + count;
                    }
                }
            }

            slotLabels[i].setText(symbol);

            if (i == active) {
                slotCells[i].setBackground(new TextureRegionDrawable(new TextureRegion(slotActiveBgTexture)));
            } else {
                slotCells[i].setBackground(new TextureRegionDrawable(new TextureRegion(slotBgTexture)));
            }
        }

        ItemType activeItem = player.getActiveHotbarItem();
        hotbarItemNameLabel.setText(activeItem != null ? formatItemName(activeItem) : "Empty");
    }

    private String getSlotSymbol(ItemType item) {
        if (item == null) return "";
        switch (item) {
            case DECONSTRUCT_TOOL: return "X";
            case DIRT:             return "DT";
            case STONE:            return "ST";
            default:
                String name = item.name();
                return name.length() > 2 ? name.substring(0, 2) : name;
        }
    }

    // ── Hub Panel Visibility ─────────────────────────────────────────────

    public void setHubPanelVisible(boolean visible) {
        if (visible == hubPanelVisible) return;
        hubPanelVisible = visible;
        hubPanel.setVisible(visible);
    }

    public boolean isHubPanelVisible() { return hubPanelVisible; }

    // ── Utility ──────────────────────────────────────────────────────────

    private Texture createSolidTexture(float r, float g, float b, float a) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }

    private String formatBlockName(Block.BlockType type) {
        String name = type.name().replace("_ORE", "").replace("_", " ").toLowerCase();
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

    public void setPlayer(Player player) { this.player = player; }

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
        if (hotbarFont != null) hotbarFont.dispose();
        if (hotbarSmallFont != null) hotbarSmallFont.dispose();
        if (hotbarNameFont != null) hotbarNameFont.dispose();
        if (bankingFont != null) bankingFont.dispose();
        if (generator != null) generator.dispose();
        if (panelBgTexture != null) panelBgTexture.dispose();
        if (buttonUpTexture != null) buttonUpTexture.dispose();
        if (buttonOverTexture != null) buttonOverTexture.dispose();
        if (buttonDownTexture != null) buttonDownTexture.dispose();
        if (slotBgTexture != null) slotBgTexture.dispose();
        if (slotActiveBgTexture != null) slotActiveBgTexture.dispose();
        stage.dispose();
    }
}
