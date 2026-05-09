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
import com.david.astrodrill.observer.InventoryObserver;

import java.util.Map;

public class Hud implements InventoryObserver, Disposable {
    public Stage stage;
    private Label dirtLabel;
    private Label stoneLabel;
    private Label copperLabel;
    private Label ironLabel;

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

        table.add(dirtLabel).pad(10).row();
        table.add(stoneLabel).padLeft(10).padBottom(10).row();
        table.add(copperLabel).padLeft(10).padBottom(10).row();
        table.add(ironLabel).padLeft(10).padBottom(10).row();

        stage.addActor(table);
    }

    @Override
    public void onInventoryUpdated(Map<Block.BlockType, Integer> inventory) {
        int dirt = inventory.getOrDefault(Block.BlockType.DIRT, 0);
        int stone = inventory.getOrDefault(Block.BlockType.STONE, 0);
        int copper = inventory.getOrDefault(Block.BlockType.COPPER, 0);
        int iron = inventory.getOrDefault(Block.BlockType.IRON, 0);

        dirtLabel.setText("Dirt: " + dirt);
        stoneLabel.setText("Stone: " + stone);
        copperLabel.setText("Copper: " + copper);
        ironLabel.setText("Iron: " + iron);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
