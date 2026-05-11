package com.david.astrodrill.crafting;

import com.david.astrodrill.item.ItemType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Defines the cost and tier-gate for manufacturing a machine.
 */
public class MachineRecipe {
    public final String displayName;
    public final String machineFactoryKey;
    public final ItemType outputItemType;
    public final Map<ItemType, Integer> cost;
    public final int requiredHubTier;

    private MachineRecipe(String displayName, String machineFactoryKey, ItemType outputItemType,
                          Map<ItemType, Integer> cost, int requiredHubTier) {
        this.displayName = displayName;
        this.machineFactoryKey = machineFactoryKey;
        this.outputItemType = outputItemType;
        this.cost = Collections.unmodifiableMap(cost);
        this.requiredHubTier = requiredHubTier;
    }

    private static final MachineRecipe[] ALL_RECIPES = buildRecipes();
    public static MachineRecipe[] getAllRecipes() { return ALL_RECIPES; }

    private static MachineRecipe[] buildRecipes() {
        return new MachineRecipe[] {
            // ── Stage 1: Bootstrap Smelters (hand-minable costs) ────────
            new MachineRecipe("Iron Smelter", "IronSmelter", ItemType.IRON_SMELTER,
                mapOf(ItemType.STONE, 20, ItemType.RAW_IRON, 10), 1),
            new MachineRecipe("Copper Smelter", "CopperSmelter", ItemType.COPPER_SMELTER,
                mapOf(ItemType.STONE, 20, ItemType.RAW_COPPER, 10), 1),

            // ── Stage 2: Power (requires smelted ingots) ───────────────
            new MachineRecipe("Coal Generator", "CoalGenerator", ItemType.COAL_GENERATOR,
                mapOf(ItemType.STONE, 30), 1),

            // ── Stage 3: Assemblers (sustained smelting) ───────────────
            new MachineRecipe("Gear Assembler", "GearAssembler", ItemType.GEAR_ASSEMBLER,
                mapOf(ItemType.IRON_INGOT, 20, ItemType.COPPER_INGOT, 10), 1),
            new MachineRecipe("Wire Assembler", "WireAssembler", ItemType.WIRE_ASSEMBLER,
                mapOf(ItemType.IRON_INGOT, 15, ItemType.COPPER_INGOT, 15), 1),

            // ── Stage 4: Automation Reward ─────────────────────────────
            new MachineRecipe("Auto Miner", "AutoMiner", ItemType.AUTO_MINER,
                mapOf(ItemType.IRON_GEAR, 10, ItemType.COPPER_WIRE, 10, ItemType.IRON_INGOT, 5), 1),
        };
    }

    private static Map<ItemType, Integer> mapOf(Object... pairs) {
        Map<ItemType, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((ItemType) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }
}
