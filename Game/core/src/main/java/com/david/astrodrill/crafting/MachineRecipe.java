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
    public final String description;
    public final Map<ItemType, Integer> productionInputs;
    public final Map<ItemType, Integer> productionOutputs;
    public final float productionTime;

    private MachineRecipe(String displayName, String machineFactoryKey, ItemType outputItemType,
                          Map<ItemType, Integer> cost, int requiredHubTier, String description,
                          Map<ItemType, Integer> productionInputs, Map<ItemType, Integer> productionOutputs,
                          float productionTime) {
        this.displayName = displayName;
        this.machineFactoryKey = machineFactoryKey;
        this.outputItemType = outputItemType;
        this.cost = Collections.unmodifiableMap(cost);
        this.requiredHubTier = requiredHubTier;
        this.description = description;
        this.productionInputs = productionInputs != null ? Collections.unmodifiableMap(productionInputs) : null;
        this.productionOutputs = productionOutputs != null ? Collections.unmodifiableMap(productionOutputs) : null;
        this.productionTime = productionTime;
    }

    private static final MachineRecipe[] ALL_RECIPES = buildRecipes();
    public static MachineRecipe[] getAllRecipes() { return ALL_RECIPES; }

    private static MachineRecipe[] buildRecipes() {
        return new MachineRecipe[] {
            // ── Stage 1: Bootstrap Smelters (hand-minable costs) ────────
            new MachineRecipe("Iron Smelter", "IronSmelter", ItemType.IRON_SMELTER,
                mapOf(ItemType.STONE, 20, ItemType.RAW_IRON, 10), 1,
                "Smelts raw iron ore into iron ingots.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                mapOf(ItemType.RAW_IRON, 1), mapOf(ItemType.IRON_INGOT, 1), 5f),
                
            new MachineRecipe("Copper Smelter", "CopperSmelter", ItemType.COPPER_SMELTER,
                mapOf(ItemType.STONE, 20, ItemType.RAW_COPPER, 10), 1,
                "Smelts raw copper ore into copper ingots.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                mapOf(ItemType.RAW_COPPER, 1), mapOf(ItemType.COPPER_INGOT, 1), 5f),

            // ── Stage 2: Power (requires smelted ingots) ───────────────
            new MachineRecipe("Coal Generator", "CoalGenerator", ItemType.COAL_GENERATOR,
                mapOf(ItemType.STONE, 30), 1,
                "Powers all adjacent (cardinal) machines.\n"
                + "\n"
                + "Burns 1 Raw Coal every 15s of demand-weighted time.\n"
                + "Idle when no neighbor needs power.\n"
                + "\n"
                + "Right click to toggle OFF/ON.",
                null, null, 0f),

            // ── Stage 3: Assemblers (sustained smelting) ───────────────
            new MachineRecipe("Gear Assembler", "GearAssembler", ItemType.GEAR_ASSEMBLER,
                mapOf(ItemType.IRON_INGOT, 15, ItemType.COPPER_INGOT, 6), 1,
                "Stamps iron ingots into mechanical gears.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                mapOf(ItemType.IRON_INGOT, 2), mapOf(ItemType.IRON_GEAR, 1), 6f),
                
            new MachineRecipe("Wire Assembler", "WireAssembler", ItemType.WIRE_ASSEMBLER,
                mapOf(ItemType.IRON_INGOT, 12, ItemType.COPPER_INGOT, 10), 1,
                "Draws copper ingots into wire.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                mapOf(ItemType.COPPER_INGOT, 1), mapOf(ItemType.COPPER_WIRE, 2), 6f),

            // ── Stage 4: Automation Reward ─────────────────────────────
            new MachineRecipe("Auto Miner", "AutoMiner", ItemType.AUTO_MINER,
                mapOf(ItemType.IRON_GEAR, 6, ItemType.COPPER_WIRE, 6, ItemType.IRON_INGOT, 4), 1,
                "Mines the block directly below it every 15s\n"
                + "and deposits 2 raw resources to the vault.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                null, null, 15f),

            // ── Stage 5: Mid-game refining (Hub Tier 1+) ───────────────
            new MachineRecipe("Gold Smelter", "GoldSmelter", ItemType.GOLD_SMELTER,
                mapOf(ItemType.STONE, 30, ItemType.IRON_INGOT, 8), 1,
                "Smelts raw gold from the Mantle into ingots.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                mapOf(ItemType.RAW_GOLD, 1), mapOf(ItemType.GOLD_INGOT, 1), 8f),

            // ── Stage 6: Electronics (Hub Tier 2) ──────────────────────
            new MachineRecipe("Refinery", "Refinery", ItemType.REFINERY,
                mapOf(ItemType.IRON_INGOT, 15, ItemType.COPPER_INGOT, 6), 2,
                "Refines raw silicon into silicon wafers.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                mapOf(ItemType.RAW_SILICON, 1), mapOf(ItemType.SILICON_WAFER, 1), 7f),
                
            new MachineRecipe("Circuit Fab", "CircuitFab", ItemType.CIRCUIT_FAB,
                mapOf(ItemType.IRON_GEAR, 6, ItemType.COPPER_WIRE, 10), 2,
                "Fabricates circuit boards from wafers, wire, and gold contacts.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                mapOf(ItemType.SILICON_WAFER, 1, ItemType.COPPER_WIRE, 1, ItemType.GOLD_INGOT, 1), mapOf(ItemType.CIRCUIT_BOARD, 1), 8f),

            // ── Stage 7: Rocket components (Hub Tier 3) ────────────────
            new MachineRecipe("Fuel Mixer", "FuelMixer", ItemType.FUEL_MIXER,
                mapOf(ItemType.IRON_INGOT, 25, ItemType.COPPER_WIRE, 12), 3,
                "Mixes uranium and coal into rocket fuel.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                mapOf(ItemType.RAW_URANIUM, 1, ItemType.RAW_COAL, 1), mapOf(ItemType.ROCKET_FUEL, 1), 10f),
                
            new MachineRecipe("Hull Press", "HullPress", ItemType.HULL_PRESS,
                mapOf(ItemType.IRON_INGOT, 30, ItemType.IRON_GEAR, 10, ItemType.GOLD_INGOT, 5), 3,
                "Presses iron and obsidian into hull plating.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON.",
                mapOf(ItemType.IRON_INGOT, 2, ItemType.RAW_OBSIDIAN, 1), mapOf(ItemType.HULL_PLATING, 1), 8f),
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
