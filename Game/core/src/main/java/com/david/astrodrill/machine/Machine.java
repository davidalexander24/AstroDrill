package com.david.astrodrill.machine;

import com.david.astrodrill.screen.PlayScreen;

public abstract class Machine {
    public float x;
    public float y;
    public float width;
    public float height;
    public boolean isPowered = false;
    public float processTimer = 0f;
    public float processDuration = 2f;

    public boolean isAdjacentTo(float targetX, float targetY, float targetWidth, float targetHeight) {
        float epsilon = 0.05f;
        boolean overlapX = this.x < targetX + targetWidth + epsilon && this.x + this.width + epsilon > targetX;
        boolean overlapY = this.y < targetY + targetHeight + epsilon && this.y + this.height + epsilon > targetY;
        return overlapX && overlapY;
    }

    public abstract void update(float delta, PlayScreen screen);
    public abstract void process();
}
