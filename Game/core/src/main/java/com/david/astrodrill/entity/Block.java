package com.david.astrodrill.entity;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Pool.Poolable;

public class Block implements Poolable {

    public enum BlockType {
        DIRT, STONE, COAL_ORE, COPPER_ORE, IRON_ORE, BASALT, GOLD_ORE, SILICON_ORE, OBSIDIAN, URANIUM_ORE, BEDROCK
    }

    /** Minimum drill strength required to mine a given block type. BEDROCK is unreachable. */
    public static int requiredStrength(BlockType t) {
        if (t == null) return 1;
        switch (t) {
            case DIRT: case STONE: case COAL_ORE: case COPPER_ORE: case IRON_ORE:
                return 1;
            case BASALT: case GOLD_ORE: case SILICON_ORE:
                return 2;
            case OBSIDIAN: case URANIUM_ORE:
                return 3;
            case BEDROCK:
            default:
                return 99;
        }
    }

    public BlockType type;
    public float x;
    public float y;
    public float width;
    public float height;
    public boolean active;
    public boolean isDestructible = true;

    /** True if this block was placed by the player (not world-generated). */
    public boolean isPlayerPlaced = false;

    public Rectangle bounds;

    public Block() {
        this.active = false;
        this.bounds = new Rectangle();
    }

    public void init(float x, float y, float width, float height, BlockType type) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.type = type;
        this.active = true;
        this.isDestructible = (type != BlockType.BEDROCK);
        this.isPlayerPlaced = false;
        this.bounds.set(x, y, width, height);
    }

    /** Overload for player-placed blocks. */
    public void init(float x, float y, float width, float height, BlockType type, boolean playerPlaced) {
        init(x, y, width, height, type);
        this.isPlayerPlaced = playerPlaced;
    }

    @Override
    public void reset() {
        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;
        this.type = null;
        this.active = false;
        this.isDestructible = true;
        this.isPlayerPlaced = false;
        this.bounds.set(0, 0, 0, 0);
    }
}
