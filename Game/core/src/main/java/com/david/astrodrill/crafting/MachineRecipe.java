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

    private MachineRecipe(String displayName, String machineFactoryKey, ItemType outputItemType,
                          Map<ItemType, Integer> cost, int requiredHubTier, String description) {
        this.displayName = displayName;
        this.machineFactoryKey = machineFactoryKey;
        this.outputItemType = outputItemType;
        this.cost = Collections.unmodifiableMap(cost);
        this.requiredHubTier = requiredHubTier;
        this.description = description;
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
                + "1 Raw Iron -> 1 Iron Ingot  (5s)\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),
            new MachineRecipe("Copper Smelter", "CopperSmelter", ItemType.COPPER_SMELTER,
                mapOf(ItemType.STONE, 20, ItemType.RAW_COPPER, 10), 1,
                "Smelts raw copper ore into copper ingots.\n"
                + "\n"
                + "1 Raw Copper -> 1 Copper Ingot  (5s)\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),

            // ── Stage 2: Power (requires smelted ingots) ───────────────
            new MachineRecipe("Coal Generator", "CoalGenerator", ItemType.COAL_GENERATOR,
                mapOf(ItemType.STONE, 30), 1,
                "Powers all adjacent (cardinal) machines.\n"
                + "\n"
                + "Burns 1 Raw Coal every 15s of demand-weighted time.\n"
                + "Idle when no neighbor needs power.\n"
                + "\n"
                + "Right click to toggle OFF/ON."),

            // ── Stage 3: Assemblers (sustained smelting) ───────────────
            new MachineRecipe("Gear Assembler", "GearAssembler", ItemType.GEAR_ASSEMBLER,
                mapOf(ItemType.IRON_INGOT, 15, ItemType.COPPER_INGOT, 6), 1,
                "Stamps iron ingots into mechanical gears.\n"
                + "\n"
                + "2 Iron Ingots -> 1 Iron Gear  (6s)\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),
            new MachineRecipe("Wire Assembler", "WireAssembler", ItemType.WIRE_ASSEMBLER,
                mapOf(ItemType.IRON_INGOT, 12, ItemType.COPPER_INGOT, 10), 1,
                "Draws copper ingots into wire.\n"
                + "\n"
                + "1 Copper Ingot -> 2 Copper Wire  (6s)\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),

            // ── Stage 4: Automation Reward ─────────────────────────────
            new MachineRecipe("Auto Miner", "AutoMiner", ItemType.AUTO_MINER,
                mapOf(ItemType.IRON_GEAR, 6, ItemType.COPPER_WIRE, 6, ItemType.IRON_INGOT, 4), 1,
                "Mines the block directly below it every 15s\n"
                + "and deposits 2 raw resources to the vault.\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),

            // ── Stage 5: Mid-game refining (Hub Tier 1+) ───────────────
            new MachineRecipe("Gold Smelter", "GoldSmelter", ItemType.GOLD_SMELTER,
                mapOf(ItemType.STONE, 30, ItemType.IRON_INGOT, 8), 1,
                "Smelts raw gold from the Mantle into ingots.\n"
                + "\n"
                + "1 Raw Gold -> 1 Gold Ingot  (8s)\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),

            // ── Stage 6: Electronics (Hub Tier 2) ──────────────────────
            new MachineRecipe("Refinery", "Refinery", ItemType.REFINERY,
                mapOf(ItemType.IRON_INGOT, 15, ItemType.COPPER_INGOT, 6), 2,
                "Refines raw silicon into silicon wafers.\n"
                + "\n"
                + "1 Raw Silicon -> 1 Silicon Wafer  (7s)\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),
            new MachineRecipe("Circuit Fab", "CircuitFab", ItemType.CIRCUIT_FAB,
                mapOf(ItemType.IRON_GEAR, 6, ItemType.COPPER_WIRE, 10), 2,
                "Fabricates circuit boards from wafers, wire, and gold contacts.\n"
                + "\n"
                + "1 Wafer + 1 Wire + 1 Gold Ingot -> 1 Circuit Board (8s)\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),

            // ── Stage 7: Rocket components (Hub Tier 3) ────────────────
            new MachineRecipe("Fuel Mixer", "FuelMixer", ItemType.FUEL_MIXER,
                mapOf(ItemType.IRON_INGOT, 25, ItemType.COPPER_WIRE, 12), 3,
                "Mixes uranium and coal into rocket fuel.\n"
                + "\n"
                + "1 Raw Uranium + 1 Raw Coal -> 1 Rocket Fuel  (10s)\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),
            new MachineRecipe("Hull Press", "HullPress", ItemType.HULL_PRESS,
                mapOf(ItemType.IRON_INGOT, 30, ItemType.IRON_GEAR, 10, ItemType.GOLD_INGOT, 5), 3,
                "Presses iron and obsidian into hull plating.\n"
                + "\n"
                + "2 Iron Ingots + 1 Raw Obsidian -> 1 Hull Plating  (8s)\n"
                + "\n"
                + "Requires adjacent Coal Generator power.\n"
                + "Right click to toggle OFF/ON."),
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
