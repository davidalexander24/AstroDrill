package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

import java.util.List;

/**
 * Consumes 1 RAW_COAL every BURN_DURATION seconds while at least one adjacent
 * machine is requesting power. Burn rate scales with the number of adjacent
 * working machines (2 neighbors → 2× consumption). Does not require external
 * power — it IS the power source.
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
            burnTimer = 0f;
            isPowered = false;
            return;
        }

        int demand = countAdjacentDemand(screen.getActiveMachines());

        if (demand > 0) {
            if (burnTimer <= 0) {
                GameManager gm = GameManager.getInstance();
                if (gm.hasItems(ItemType.RAW_COAL, 1)) {
                    gm.consumeItems(ItemType.RAW_COAL, 1);
                    burnTimer = BURN_DURATION;
                }
            }
            if (burnTimer > 0) {
                burnTimer -= delta * demand;
                if (burnTimer < 0) burnTimer = 0;
            }
        }

        // Visual "lit" state: only when actively providing power this frame.
        isPowered = burnTimer > 0 && demand > 0;
    }

    private int countAdjacentDemand(List<Machine> all) {
        int count = 0;
        for (Machine m : all) {
            if (m == this) continue;
            if (m.userDisabled) continue;
            if (!m.wantsPower) continue;
            float dx = Math.abs(this.x - m.x);
            float dy = Math.abs(this.y - m.y);
            boolean adjH = Math.abs(dx - 1f) < 0.15f && dy < 0.15f;
            boolean adjV = dx < 0.15f && Math.abs(dy - 1f) < 0.15f;
            if (adjH || adjV) count++;
        }
        return count;
    }

    /** Available for adjacency power lookup if currently burning OR has coal queued in the vault. */
    public boolean isActive() {
        return burnTimer > 0 || GameManager.getInstance().hasItems(ItemType.RAW_COAL, 1);
    }

    @Override
    public void process() { /* No output — it just provides power */ }
}
