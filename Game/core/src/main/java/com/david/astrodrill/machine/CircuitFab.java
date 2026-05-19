package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * 1 SILICON_WAFER + 1 COPPER_WIRE + 1 GOLD_INGOT → 1 CIRCUIT_BOARD (requires adjacent CoalGenerator).
 */
public class CircuitFab extends Machine {
    public CircuitFab(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        this.processDuration = 8f;
    }

    @Override public String getMachineType() { return "CircuitFab"; }
    @Override public String getSymbol() { return "CF"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; wantsPower = false; return; }

        GameManager gm = GameManager.getInstance();
        boolean hasInput = gm.hasItems(ItemType.SILICON_WAFER, 1)
                        && gm.hasItems(ItemType.COPPER_WIRE, 1)
                        && gm.hasItems(ItemType.GOLD_INGOT, 1);
        wantsPower = hasInput || processTimer > 0;

        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;

        if (processTimer <= 0 && hasInput) {
            gm.consumeItems(ItemType.SILICON_WAFER, 1);
            gm.consumeItems(ItemType.COPPER_WIRE, 1);
            gm.consumeItems(ItemType.GOLD_INGOT, 1);
            processTimer = processDuration;
        }
        if (processTimer > 0) {
            processTimer -= delta;
            if (processTimer <= 0) process();
        }
    }

    @Override
    public void process() {
        GameManager.getInstance().addItems(ItemType.CIRCUIT_BOARD, 1);
    }
}
