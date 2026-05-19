package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * 2 IRON_INGOT + 1 RAW_OBSIDIAN → 1 HULL_PLATING (requires adjacent CoalGenerator).
 */
public class HullPress extends Machine {
    public HullPress(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        this.processDuration = 8f;
    }

    @Override public String getMachineType() { return "HullPress"; }
    @Override public String getSymbol() { return "HP"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; wantsPower = false; return; }

        GameManager gm = GameManager.getInstance();
        boolean hasInput = gm.hasItems(ItemType.IRON_INGOT, 2) && gm.hasItems(ItemType.RAW_OBSIDIAN, 1);
        wantsPower = hasInput || processTimer > 0;

        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;

        if (processTimer <= 0 && hasInput) {
            gm.consumeItems(ItemType.IRON_INGOT, 2);
            gm.consumeItems(ItemType.RAW_OBSIDIAN, 1);
            processTimer = processDuration;
        }
        if (processTimer > 0) {
            processTimer -= delta;
            if (processTimer <= 0) process();
        }
    }

    @Override
    public void process() {
        GameManager.getInstance().addItems(ItemType.HULL_PLATING, 1);
    }
}
