package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * Dedicated assembler: 2 IRON_INGOT → 1 IRON_GEAR (requires adjacent CoalGenerator).
 */
public class GearAssembler extends Machine {
    public GearAssembler(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        this.processDuration = 6f;
    }

    @Override public String getMachineType() { return "GearAssembler"; }
    @Override public String getSymbol() { return "GA"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; wantsPower = false; return; }

        GameManager gm = GameManager.getInstance();
        boolean hasInput = gm.hasItems(ItemType.IRON_INGOT, 2);
        wantsPower = hasInput || processTimer > 0;

        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;

        if (processTimer <= 0 && hasInput) {
            gm.consumeItems(ItemType.IRON_INGOT, 2);
            processTimer = processDuration;
        }
        if (processTimer > 0) {
            processTimer -= delta;
            if (processTimer <= 0) process();
        }
    }

    @Override
    public void process() {
        GameManager.getInstance().addItems(ItemType.IRON_GEAR, 1);
    }
}
