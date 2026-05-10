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
    
    // Vault Labels
    private Label ironIngotLabel;
    private Label copperIngotLabel;
    private Label ironGearLabel;
    private Label copperWireLabel;

    public Hud(SpriteBatch batch) {
        stage = new Stage(new ScreenViewport(), batch);

        Table table = new Table();
        table.top().left();
        table.setFillParent(true);

        BitmapFont font = new BitmapFont();
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);

        dirtLabel = new Label("Dirt: 0", labelStyle);
        stoneLabel = new Label("Stone: 0", labelStyle);
        copperLabel = new Label("Copper: 0", labelStyle);
        ironLabel = new Label("Iron: 0", labelStyle);
        
        ironIngotLabel = new Label("Iron Ingot: 0", labelStyle);
        copperIngotLabel = new Label("Copper Ingot: 0", labelStyle);
        ironGearLabel = new Label("Iron Gear: 0", labelStyle);
        copperWireLabel = new Label("Copper Wire: 0", labelStyle);

        table.add(dirtLabel).pad(10).row();
        table.add(stoneLabel).padLeft(10).padBottom(10).row();
        table.add(copperLabel).padLeft(10).padBottom(10).row();
        table.add(ironLabel).padLeft(10).padBottom(10).row();
        
        table.add(new Label("--- Vault ---", labelStyle)).padLeft(10).padBottom(10).row();
        table.add(ironIngotLabel).padLeft(10).padBottom(10).row();
        table.add(copperIngotLabel).padLeft(10).padBottom(10).row();
        table.add(ironGearLabel).padLeft(10).padBottom(10).row();
        table.add(copperWireLabel).padLeft(10).padBottom(10).row();

        stage.addActor(table);
    }

    @Override
    public void onInventoryUpdated(Map<Block.BlockType, Integer> inventory) {
        int dirt = inventory.getOrDefault(Block.BlockType.DIRT, 0);
        int stone = inventory.getOrDefault(Block.BlockType.STONE, 0);
        int copper = inventory.getOrDefault(Block.BlockType.COPPER_ORE, 0);
        int iron = inventory.getOrDefault(Block.BlockType.IRON_ORE, 0);

        dirtLabel.setText("Dirt: " + dirt);
        stoneLabel.setText("Stone: " + stone);
        copperLabel.setText("Copper: " + copper);
        ironLabel.setText("Iron: " + iron);
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
