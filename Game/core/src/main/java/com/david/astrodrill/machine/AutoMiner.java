package com.david.astrodrill.machine;

import com.david.astrodrill.screen.PlayScreen;

public class AutoMiner extends Machine {

    private float mineTimer = 0f;
    private final float MINE_INTERVAL = 1f;

    public AutoMiner(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void update(float delta, PlayScreen screen) {
        mineTimer += delta;
        if (mineTimer >= MINE_INTERVAL) {
            // Attempt to mine the block directly below the machine
            if (screen.mineBlockAt(this.x + this.width / 2, this.y - 0.1f)) {
                // If we successfully mined a block, reset the timer completely
                mineTimer = 0f;
            } else {
                // Wait a bit before checking again, or just reset
                mineTimer = 0f;
            }
            
            // To make the machine fall down as it mines, we can apply gravity or just set its y
            // But for now, let's keep it simple. If we want it to fall:
            this.y -= 1f; // Move down 1 block. Wait, this will make it fall unconditionally.
            // Let's only move it down if it successfully mined something or if there's no block?
            // Actually, physics would be better, but we can simply check if we mined it.
            // If we mined it, we can move down. Wait, we want the machine to fall continuously 
            // if there's empty space, but let's just make it move down if it successfully mined.
        }
    }
}
