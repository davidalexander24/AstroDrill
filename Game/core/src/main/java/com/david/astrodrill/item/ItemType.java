package com.david.astrodrill.item;

public enum ItemType {
    // Raw ores (banked from mining)
    RAW_COAL, RAW_COPPER, RAW_IRON, RAW_GOLD, RAW_SILICON, RAW_URANIUM, RAW_OBSIDIAN,

    // Refined intermediates
    COPPER_INGOT, IRON_INGOT, GOLD_INGOT, SILICON_WAFER,
    IRON_GEAR, COPPER_WIRE, CIRCUIT_BOARD,

    // End-game cargo for rocket launch
    HULL_PLATING, ROCKET_FUEL,

    // Placeable block items (correspond to Block.BlockType)
    DIRT, STONE,

    // Machine items (crafted, placed via hotbar)
    AUTO_MINER, COAL_GENERATOR,
    IRON_SMELTER, COPPER_SMELTER, GOLD_SMELTER,
    GEAR_ASSEMBLER, WIRE_ASSEMBLER,
    REFINERY, CIRCUIT_FAB, FUEL_MIXER, HULL_PRESS,

    // Special tools
    DECONSTRUCT_TOOL
}
