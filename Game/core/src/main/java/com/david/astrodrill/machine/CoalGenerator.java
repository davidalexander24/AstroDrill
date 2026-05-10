package com.david.astrodrill.machine;

import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.screen.PlayScreen;

public class CoalGenerator extends Machine {
    private float burnTimer = 0f;

    public CoalGenerator(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void update(float delta, PlayScreen screen) {
        if (burnTimer <= 0) {
            if (GameManager.getInstance().hasItems(ItemType.RAW_COAL, 1)) {
                GameManager.getInstance().consumeItems(ItemType.RAW_COAL, 1);
                burnTimer = 5f;
            }
        }
        
        if (burnTimer > 0) {
            burnTimer -= delta;
        }
    }

    public boolean isGenerating() {
        return burnTimer > 0;
    }

    @Override
    public void process() {
        // CoalGenerator doesn't use the standard process logic
    }
}
