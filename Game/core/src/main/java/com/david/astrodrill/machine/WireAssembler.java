package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * Dedicated assembler: 1 COPPER_INGOT → 2 COPPER_WIRE (requires adjacent CoalGenerator).
 */
public class WireAssembler extends Machine {
    public WireAssembler(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        this.processDuration = 4f;
    }

    @Override public String getMachineType() { return "WireAssembler"; }
    @Override public String getSymbol() { return "WA"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;

        if (processTimer <= 0) {
            GameManager gm = GameManager.getInstance();
            if (gm.hasItems(ItemType.COPPER_INGOT, 1)) {
                gm.consumeItems(ItemType.COPPER_INGOT, 1);
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
        GameManager.getInstance().addItems(ItemType.COPPER_WIRE, 2);
    }
}
