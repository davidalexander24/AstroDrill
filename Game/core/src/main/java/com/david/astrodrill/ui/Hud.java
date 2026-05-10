package com.david.astrodrill.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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
        if (generator != null) generator.dispose();
        stage.dispose();
    }
}
