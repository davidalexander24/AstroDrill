package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

public class Assembler extends Machine {

    public enum Recipe {
        IRON_GEAR, COPPER_WIRE
    }

    public Recipe currentRecipe = Recipe.IRON_GEAR;

    public Assembler(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.processDuration = 2f;
    }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (!isPowered) {
            return;
        }

        if (processTimer <= 0) {
            GameManager gm = GameManager.getInstance();
            if (currentRecipe == Recipe.IRON_GEAR) {
                if (gm.hasItems(ItemType.IRON_INGOT, 2)) {
                    gm.consumeItems(ItemType.IRON_INGOT, 2);
                    processTimer = processDuration;
                }
            } else if (currentRecipe == Recipe.COPPER_WIRE) {
                if (gm.hasItems(ItemType.COPPER_INGOT, 1)) {
                    gm.consumeItems(ItemType.COPPER_INGOT, 1);
                    processTimer = processDuration;
                }
            }
        }

        if (processTimer > 0) {
            processTimer -= delta;
            if (processTimer <= 0) {
                process();
            }
        }
    }

    @Override
    public void process() {
        GameManager gm = GameManager.getInstance();
        if (currentRecipe == Recipe.IRON_GEAR) {
            gm.addItems(ItemType.IRON_GEAR, 1);
        } else if (currentRecipe == Recipe.COPPER_WIRE) {
            gm.addItems(ItemType.COPPER_WIRE, 3);
        }
    }
}
