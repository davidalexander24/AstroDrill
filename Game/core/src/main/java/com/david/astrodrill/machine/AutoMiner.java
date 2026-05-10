package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.item.ItemType;
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
        if (!isPowered) return;
        
        mineTimer += delta;
        if (mineTimer >= MINE_INTERVAL) {
            // Attempt to mine the block directly below the machine
            Block.BlockType type = screen.mineBlockAt(this.x + this.width / 2, this.y - 0.1f);
            if (type != null) {
                // Deposit into Global Vault
                depositToVault(type);
                mineTimer = 0f;
                // Move down
                this.y -= 1f;
            } else {
                mineTimer = 0f;
            }
        }
    }

    private void depositToVault(Block.BlockType type) {
        GameManager gm = GameManager.getInstance();
        switch (type) {
            case IRON_ORE:
                gm.addItems(ItemType.RAW_IRON, 1);
                break;
            case COPPER_ORE:
                gm.addItems(ItemType.RAW_COPPER, 1);
                break;
            case COAL_ORE:
                gm.addItems(ItemType.RAW_COAL, 1);
                break;
            // Ignore other blocks for now or add them if needed
        }
    }

    @Override
    public void process() {
        // AutoMiner doesn't use the process timer yet
    }
}
