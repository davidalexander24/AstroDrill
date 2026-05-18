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
                mapOf(ItemType.IRON_INGOT, 15, ItemType.COPPER_INGOT, 6), 1),
            new MachineRecipe("Wire Assembler", "WireAssembler", ItemType.WIRE_ASSEMBLER,
                mapOf(ItemType.IRON_INGOT, 12, ItemType.COPPER_INGOT, 10), 1),

            // ── Stage 4: Automation Reward ─────────────────────────────
            new MachineRecipe("Auto Miner", "AutoMiner", ItemType.AUTO_MINER,
                mapOf(ItemType.IRON_GEAR, 6, ItemType.COPPER_WIRE, 6, ItemType.IRON_INGOT, 4), 1),

            // ── Stage 5: Mid-game refining (Hub Tier 1+) ───────────────
            new MachineRecipe("Gold Smelter", "GoldSmelter", ItemType.GOLD_SMELTER,
                mapOf(ItemType.STONE, 30, ItemType.IRON_INGOT, 8), 1),

            // ── Stage 6: Electronics (Hub Tier 2) ──────────────────────
            new MachineRecipe("Refinery", "Refinery", ItemType.REFINERY,
                mapOf(ItemType.IRON_INGOT, 15, ItemType.COPPER_INGOT, 6), 2),
            new MachineRecipe("Circuit Fab", "CircuitFab", ItemType.CIRCUIT_FAB,
                mapOf(ItemType.IRON_GEAR, 6, ItemType.COPPER_WIRE, 10), 2),

            // ── Stage 7: Rocket components (Hub Tier 3) ────────────────
            new MachineRecipe("Fuel Mixer", "FuelMixer", ItemType.FUEL_MIXER,
                mapOf(ItemType.IRON_INGOT, 25, ItemType.COPPER_WIRE, 12), 3),
            new MachineRecipe("Hull Press", "HullPress", ItemType.HULL_PRESS,
                mapOf(ItemType.IRON_INGOT, 30, ItemType.IRON_GEAR, 10), 3),
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
