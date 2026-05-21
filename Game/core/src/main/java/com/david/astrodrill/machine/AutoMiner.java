package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.entity.Player;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * Sits on top of a block and continuously *taps* it for resource — the block
 * is NOT consumed, so a single AutoMiner is an infinite source of whatever
 * ore it was placed on. Each cycle (once per coal generator burn) deposits
 * 2 raw items to the vault, gated on adjacent CoalGenerator power.
 */
public class AutoMiner extends Machine {
    private float mineTimer = 0f;
    private final float MINE_INTERVAL = 15f; // matches CoalGenerator BURN_DURATION — 1 coal = 2 resources

    public AutoMiner(float x, float y, float width, float height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    @Override public String getMachineType() { return "AutoMiner"; }
    @Override public String getSymbol() { return "AM"; }

    /** AutoMiner uses mineTimer instead of processTimer, so override the base check. */
    @Override public boolean isRunning() {
        return isPowered && !userDisabled && mineTimer > 0f;
    }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; wantsPower = false; return; }

        Block.BlockType source = screen.peekBlockTypeBelow(this);
        wantsPower = source != null;

        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;
        if (source == null) return;

        mineTimer += delta;
        if (mineTimer >= MINE_INTERVAL) {
            depositToVault(source);
            mineTimer = 0f;
        }
    }

    private void depositToVault(Block.BlockType type) {
        ItemType item = Player.blockTypeToRawItemType(type);
        if (item == null) return;
        GameManager.getInstance().addItems(item, 2);
    }

    @Override public void process() { }
}
