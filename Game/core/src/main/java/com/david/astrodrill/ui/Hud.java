package com.david.astrodrill.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

import java.util.Map;

public class Hud implements InventoryObserver, VaultObserver, Disposable {
    public Stage stage;
    private Label dirtLabel;
    private Label stoneLabel;
    private Label copperLabel;
    private Label ironLabel;
    private Label coalLabel;
    
    // Battery
    private Label batteryLabel;
    private Label batteryWarningLabel;
    private Player player;
    
    // Vault Labels
    private Label ironIngotLabel;
    private Label copperIngotLabel;
    private Label ironGearLabel;
    private Label copperWireLabel;

    private static final float LOW_BATTERY_THRESHOLD = 20f;

    public Hud(SpriteBatch batch) {
        stage = new Stage(new ScreenViewport(), batch);

        Table table = new Table();
        table.top().left();
        table.setFillParent(true);

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.25f);
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle warningStyle = new Label.LabelStyle(font, Color.RED);

        // Battery display (top of HUD)
        batteryLabel = new Label("Battery: 100%", labelStyle);
        batteryWarningLabel = new Label("", warningStyle);

        dirtLabel = new Label("Dirt: 0", labelStyle);
        stoneLabel = new Label("Stone: 0", labelStyle);
        copperLabel = new Label("Copper: 0", labelStyle);
        ironLabel = new Label("Iron: 0", labelStyle);
        coalLabel = new Label("Coal: 0", labelStyle);
        
        ironIngotLabel = new Label("Iron Ingot: 0", labelStyle);
        copperIngotLabel = new Label("Copper Ingot: 0", labelStyle);
        ironGearLabel = new Label("Iron Gear: 0", labelStyle);
        copperWireLabel = new Label("Copper Wire: 0", labelStyle);

        table.add(batteryLabel).pad(10).row();
        table.add(batteryWarningLabel).padLeft(10).padBottom(5).row();
        table.add(new Label("--- Inventory ---", labelStyle)).padLeft(10).padBottom(5).row();
        table.add(dirtLabel).padLeft(10).padBottom(5).row();
        table.add(stoneLabel).padLeft(10).padBottom(5).row();
        table.add(copperLabel).padLeft(10).padBottom(5).row();
        table.add(ironLabel).padLeft(10).padBottom(5).row();
        table.add(coalLabel).padLeft(10).padBottom(10).row();
        
        table.add(new Label("--- Vault ---", labelStyle)).padLeft(10).padBottom(5).row();
        table.add(ironIngotLabel).padLeft(10).padBottom(5).row();
        table.add(copperIngotLabel).padLeft(10).padBottom(5).row();
        table.add(ironGearLabel).padLeft(10).padBottom(5).row();
        table.add(copperWireLabel).padLeft(10).padBottom(5).row();

        stage.addActor(table);
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    /** Call every frame to keep the battery display up-to-date. */
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
        int dirt = inventory.getOrDefault(Block.BlockType.DIRT, 0);
        int stone = inventory.getOrDefault(Block.BlockType.STONE, 0);
        int copper = inventory.getOrDefault(Block.BlockType.COPPER_ORE, 0);
        int iron = inventory.getOrDefault(Block.BlockType.IRON_ORE, 0);
        int coal = inventory.getOrDefault(Block.BlockType.COAL_ORE, 0);

        dirtLabel.setText("Dirt: " + dirt);
        stoneLabel.setText("Stone: " + stone);
        copperLabel.setText("Copper: " + copper);
        ironLabel.setText("Iron: " + iron);
        coalLabel.setText("Coal: " + coal);
    }

    @Override
    public void onVaultUpdated(Map<ItemType, Integer> vault) {
        int ironIngots = vault.getOrDefault(ItemType.IRON_INGOT, 0);
        int copperIngots = vault.getOrDefault(ItemType.COPPER_INGOT, 0);
        int ironGears = vault.getOrDefault(ItemType.IRON_GEAR, 0);
        int copperWires = vault.getOrDefault(ItemType.COPPER_WIRE, 0);
        
        ironIngotLabel.setText("Iron Ingot: " + ironIngots);
        copperIngotLabel.setText("Copper Ingot: " + copperIngots);
        ironGearLabel.setText("Iron Gear: " + ironGears);
        copperWireLabel.setText("Copper Wire: " + copperWires);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
