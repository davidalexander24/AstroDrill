package com.david.astrodrill.crafting;

import com.david.astrodrill.item.ItemType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defines a single upgrade track available in the Hub Terminal → Upgrades tab.
 * Follows the same shape as MachineRecipe — per-tier cost is a Map<ItemType, Integer>.
 *
 * tierCosts[i] is the material cost to advance from level (i+1) to level (i+2).
 * maxTier is the highest reachable tier (so tierCosts has length maxTier - 1).
 */
public class UpgradeDefinition {
    public final String key;
    public final String displayName;
    public final Map<ItemType, Integer>[] tierCosts;
    public final int maxTier;

    @SafeVarargs
    private UpgradeDefinition(String key, String displayName, Map<ItemType, Integer>... tierCosts) {
        this.key = key;
        this.displayName = displayName;
        this.tierCosts = tierCosts;
        this.maxTier = tierCosts.length + 1;
    }

    /** Returns the material cost to advance from currentTier → currentTier + 1, or null if at max. */
    public Map<ItemType, Integer> costToNext(int currentTier) {
        int idx = currentTier - 1;
        if (idx < 0 || idx >= tierCosts.length) return null;
        return tierCosts[idx];
    }

    public static final UpgradeDefinition[] ALL = {
        new UpgradeDefinition("DRILL", "Drill Strength",
            mapOf(ItemType.IRON_INGOT, 10, ItemType.COPPER_INGOT, 5),
            mapOf(ItemType.IRON_GEAR, 10, ItemType.COPPER_WIRE, 5)),

        new UpgradeDefinition("BATTERY", "Battery Cap",
            mapOf(ItemType.COPPER_WIRE, 5),
            mapOf(ItemType.COPPER_WIRE, 10, ItemType.IRON_INGOT, 5),
            mapOf(ItemType.CIRCUIT_BOARD, 3)),

        new UpgradeDefinition("JETPACK", "Jetpack",
            mapOf(ItemType.IRON_GEAR, 10),
            mapOf(ItemType.IRON_GEAR, 15, ItemType.COPPER_WIRE, 10)),

        new UpgradeDefinition("WHEEL", "Wheel Speed",
            mapOf(ItemType.IRON_INGOT, 6, ItemType.COPPER_INGOT, 4),
            mapOf(ItemType.IRON_GEAR, 6, ItemType.COPPER_WIRE, 6)),

        new UpgradeDefinition("HUB", "Hub Tier",
            mapOf(ItemType.IRON_INGOT, 20, ItemType.COPPER_INGOT, 10),
            mapOf(ItemType.CIRCUIT_BOARD, 5, ItemType.IRON_GEAR, 20)),
    };

    private static Map<ItemType, Integer> mapOf(Object... pairs) {
        Map<ItemType, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((ItemType) pairs[i], (Integer) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
