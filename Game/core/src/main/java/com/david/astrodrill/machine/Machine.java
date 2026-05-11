package com.david.astrodrill.machine;

import com.david.astrodrill.screen.PlayScreen;

import java.util.List;

public abstract class Machine {
    public float x;
    public float y;
    public float width;
    public float height;
    public boolean isPowered = false;
    public float processTimer = 0f;
    public float processDuration = 2f;

    /**
     * Returns a unique type string for this machine, used for rendering dispatch.
     */
    public abstract String getMachineType();
    public abstract String getSymbol();

    public abstract void update(float delta, PlayScreen screen);
    public abstract void process();

    /**
     * Checks cardinal adjacency (Up, Down, Left, Right) on the 1x1 grid.
     * Returns true if any active CoalGenerator occupies one of the 4 cardinal neighbors.
     */
    public boolean hasAdjacentPower(List<Machine> allMachines) {
        for (Machine m : allMachines) {
            if (!(m instanceof CoalGenerator)) continue;
            CoalGenerator gen = (CoalGenerator) m;
            if (!gen.isActive()) continue;

            float dx = Math.abs(this.x - m.x);
            float dy = Math.abs(this.y - m.y);

            // Cardinal adjacency: exactly 1 unit apart on one axis, same on the other
            boolean adjHorizontal = Math.abs(dx - 1f) < 0.15f && dy < 0.15f;
            boolean adjVertical   = dx < 0.15f && Math.abs(dy - 1f) < 0.15f;

            if (adjHorizontal || adjVertical) return true;
        }
        return false;
    }
}
