package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * Consumes 1 RAW_COAL every 15 seconds to remain Active.
 * Does not require external power — it IS the power source.
 */
public class CoalGenerator extends Machine {
    private float burnTimer = 0f;
    private static final float BURN_DURATION = 15f;

    public CoalGenerator(float x, float y, float w, float h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
    }

    @Override public String getMachineType() { return "CoalGenerator"; }
    @Override public String getSymbol() { return "CG"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) {
            burnTimer = 0f;  // force inactive — neighbors lose power next frame
            return;
        }
        if (burnTimer > 0) {
            burnTimer -= delta;
        } else {
            // Try to consume coal from vault
            GameManager gm = GameManager.getInstance();
            if (gm.hasItems(ItemType.RAW_COAL, 1)) {
                gm.consumeItems(ItemType.RAW_COAL, 1);
                burnTimer = BURN_DURATION;
            }
        }
    }

    /** Returns true if this generator is currently burning fuel. */
    public boolean isActive() { return burnTimer > 0; }

    @Override
    public void process() { /* No output — it just provides power */ }
}
