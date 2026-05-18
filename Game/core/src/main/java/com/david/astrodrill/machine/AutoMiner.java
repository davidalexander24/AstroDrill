package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.entity.Player;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

/**
 * Sits on top of a block, drills it, then descends one cell.
 * Each successful mine deposits 2× the raw resource to the vault (the automation
 * dividend that justifies feeding the adjacent CoalGenerator).
 */
public class AutoMiner extends Machine {
    private float mineTimer = 0f;
    private final float MINE_INTERVAL = 1.5f;

    public AutoMiner(float x, float y, float width, float height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    @Override public String getMachineType() { return "AutoMiner"; }
    @Override public String getSymbol() { return "AM"; }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (userDisabled) { isPowered = false; wantsPower = false; return; }

        // Drill is wanted only while there is a mineable block below — otherwise idle.
        wantsPower = screen.canMineBelowMachine(this);

        isPowered = hasAdjacentPower(screen.getActiveMachines());
        if (!isPowered) return;
        if (!wantsPower) return;

        mineTimer += delta;
        if (mineTimer >= MINE_INTERVAL) {
            Block.BlockType type = screen.mineBlockForMachine(this.x + this.width / 2f, this.y - 0.1f);
            if (type != null) {
                depositToVault(type);
                this.y -= 1f;
            }
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
