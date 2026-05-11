package com.david.astrodrill.item;

public enum ItemType {
    RAW_COAL, RAW_COPPER, RAW_IRON, COPPER_INGOT, IRON_INGOT, IRON_GEAR, COPPER_WIRE, CIRCUIT_BOARD,

    // Placeable block items (correspond to Block.BlockType)
    DIRT, STONE,

    // Machine items (crafted, placed via hotbar)
    AUTO_MINER, COAL_GENERATOR,
    IRON_SMELTER, COPPER_SMELTER,
    GEAR_ASSEMBLER, WIRE_ASSEMBLER,

    // Special tools
    DECONSTRUCT_TOOL
}
