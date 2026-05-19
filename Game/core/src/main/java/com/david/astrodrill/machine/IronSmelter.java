package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * Dedicated smelter: 1 RAW_IRON → 1 IRON_INGOT (requires adjacent CoalGenerator).
 */
public class IronSmelter extends Machine {
    public IronSmelter(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        this.processDuration = 5f;
    }

    @Override public String getMachineType() { return "IronSmelter"; }
    @Override public String getSymbol() { return "IS"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; wantsPower = false; return; }

        GameManager gm = GameManager.getInstance();
        boolean hasInput = gm.hasItems(ItemType.RAW_IRON, 1);
        wantsPower = hasInput || processTimer > 0;

        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;

        if (processTimer <= 0 && hasInput) {
            gm.consumeItems(ItemType.RAW_IRON, 1);
            processTimer = processDuration;
        }
        if (processTimer > 0) {
            processTimer -= delta;
            if (processTimer <= 0) process();
        }
    }

    @Override
    public void process() {
        GameManager.getInstance().addItems(ItemType.IRON_INGOT, 1);
    }
}
