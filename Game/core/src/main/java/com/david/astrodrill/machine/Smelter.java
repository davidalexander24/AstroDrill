package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

public class Smelter extends Machine {
    private ItemType currentProcessingItem = null;

    public Smelter(float x, float y, float width, float height) {
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
            if (gm.hasItems(ItemType.RAW_IRON, 1)) {
                gm.consumeItems(ItemType.RAW_IRON, 1);
                currentProcessingItem = ItemType.RAW_IRON;
                processTimer = processDuration;
            } else if (gm.hasItems(ItemType.RAW_COPPER, 1)) {
                gm.consumeItems(ItemType.RAW_COPPER, 1);
                currentProcessingItem = ItemType.RAW_COPPER;
                processTimer = processDuration;
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
        if (currentProcessingItem == null) return;
        GameManager gm = GameManager.getInstance();
        if (currentProcessingItem == ItemType.RAW_IRON) {
            gm.addItems(ItemType.IRON_INGOT, 1);
        } else if (currentProcessingItem == ItemType.RAW_COPPER) {
            gm.addItems(ItemType.COPPER_INGOT, 1);
        }
        currentProcessingItem = null;
    }
}
