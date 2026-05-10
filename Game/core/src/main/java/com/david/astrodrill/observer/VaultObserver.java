package com.david.astrodrill.observer;

import com.david.astrodrill.item.ItemType;
import java.util.Map;

public interface VaultObserver {
    void onVaultUpdated(Map<ItemType, Integer> vault);
}
