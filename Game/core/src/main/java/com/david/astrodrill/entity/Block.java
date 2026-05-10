package com.david.astrodrill.entity;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Pool.Poolable;

public class Block implements Poolable {

    public enum BlockType {
        DIRT, STONE, COAL_ORE, COPPER_ORE, IRON_ORE, BASALT, GOLD_ORE, SILICON_ORE, OBSIDIAN, URANIUM_ORE
    }

    public BlockType type;
    public float x;
    public float y;
    public float width;
    public float height;
    public boolean active;
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
        this.bounds.set(x, y, width, height);
    }

    @Override
    public void reset() {
        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;
        this.type = null;
        this.active = false;
        this.bounds.set(0, 0, 0, 0);
    }
}
