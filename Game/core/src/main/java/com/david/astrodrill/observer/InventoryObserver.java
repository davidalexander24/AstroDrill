package com.david.astrodrill.observer;

import com.david.astrodrill.entity.Block;
import java.util.Map;

public interface InventoryObserver {
    void onInventoryUpdated(Map<Block.BlockType, Integer> inventory);
}
