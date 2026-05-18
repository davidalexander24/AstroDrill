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
        this.processDuration = 8f;
    }

    @Override public String getMachineType() { return "GoldSmelter"; }
    @Override public String getSymbol() { return "GS"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; wantsPower = false; return; }

        GameManager gm = GameManager.getInstance();
        boolean hasInput = gm.hasItems(ItemType.RAW_GOLD, 1);
        wantsPower = hasInput || processTimer > 0;

        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;

        if (processTimer <= 0 && hasInput) {
            gm.consumeItems(ItemType.RAW_GOLD, 1);
            processTimer = processDuration;
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
