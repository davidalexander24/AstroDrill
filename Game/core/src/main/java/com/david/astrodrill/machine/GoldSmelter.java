package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * 1 RAW_GOLD → 1 GOLD_INGOT (requires adjacent CoalGenerator).
 */
public class GoldSmelter extends Machine {
    public GoldSmelter(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        this.processDuration = 5f;
    }

    @Override public String getMachineType() { return "GoldSmelter"; }
    @Override public String getSymbol() { return "GS"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; return; }
        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;

        if (processTimer <= 0) {
            GameManager gm = GameManager.getInstance();
            if (gm.hasItems(ItemType.RAW_GOLD, 1)) {
                gm.consumeItems(ItemType.RAW_GOLD, 1);
                processTimer = processDuration;
            }
        }
        if (processTimer > 0) {
            processTimer -= delta;
            if (processTimer <= 0) process();
        }
    }

    @Override
    public void process() {
        GameManager.getInstance().addItems(ItemType.GOLD_INGOT, 1);
    }
}
