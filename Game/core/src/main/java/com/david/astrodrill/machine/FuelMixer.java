package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * 1 RAW_URANIUM + 1 RAW_COAL → 1 ROCKET_FUEL (requires adjacent CoalGenerator).
 */
public class FuelMixer extends Machine {
    public FuelMixer(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
        this.processDuration = 6f;
    }

    @Override public String getMachineType() { return "FuelMixer"; }
    @Override public String getSymbol() { return "FM"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; return; }
        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;

        if (processTimer <= 0) {
            GameManager gm = GameManager.getInstance();
            if (gm.hasItems(ItemType.RAW_URANIUM, 1) && gm.hasItems(ItemType.RAW_COAL, 1)) {
                gm.consumeItems(ItemType.RAW_URANIUM, 1);
                gm.consumeItems(ItemType.RAW_COAL, 1);
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
        GameManager.getInstance().addItems(ItemType.ROCKET_FUEL, 1);
    }
}
