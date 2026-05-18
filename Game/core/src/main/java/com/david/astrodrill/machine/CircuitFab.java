package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * 1 SILICON_WAFER + 1 COPPER_WIRE → 1 CIRCUIT_BOARD (requires adjacent CoalGenerator).
 */
public class CircuitFab extends Machine {
    public CircuitFab(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        this.processDuration = 5f;
    }

    @Override public String getMachineType() { return "CircuitFab"; }
    @Override public String getSymbol() { return "CF"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; return; }
        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;

        if (processTimer <= 0) {
            GameManager gm = GameManager.getInstance();
            if (gm.hasItems(ItemType.SILICON_WAFER, 1) && gm.hasItems(ItemType.COPPER_WIRE, 1)) {
                gm.consumeItems(ItemType.SILICON_WAFER, 1);
                gm.consumeItems(ItemType.COPPER_WIRE, 1);
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
        GameManager.getInstance().addItems(ItemType.CIRCUIT_BOARD, 1);
    }
}
