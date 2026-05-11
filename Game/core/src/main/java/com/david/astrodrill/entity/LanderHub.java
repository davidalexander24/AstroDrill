package com.david.astrodrill.entity;

public class LanderHub {
    public float x;
    public float y;
    public float width;
    public float height;

    /** Radius (in world units) around the hub center that defines the safe zone. */
    public static final float SAFE_ZONE_RADIUS = 8f;

    /** Rate of battery recharge per second while inside the safe zone. */
    public static final float RECHARGE_RATE = 40f; // ~2.5 seconds from 0 to 100

    /** Current upgrade tier of the hub. Unlocks manufacturing recipes. */
    public int tier = 1;

    public LanderHub(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** Returns the center X position of the hub. */
    public float getCenterX() {
        return x + width / 2f;
    }

    /** Returns the center Y position of the hub. */
    public float getCenterY() {
        return y + height / 2f;
    }

    /**
     * Checks whether the given point (typically the player center) is within
     * the safe zone radius.
     */
    public boolean isInSafeZone(float px, float py) {
        float dx = px - getCenterX();
        float dy = py - getCenterY();
        return (dx * dx + dy * dy) <= (SAFE_ZONE_RADIUS * SAFE_ZONE_RADIUS);
    }

    public void upgradeTier() {
        tier++;
    }
}
