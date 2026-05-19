package com.david.astrodrill.network;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.entity.Block.BlockType;
import com.david.astrodrill.entity.LanderHub;
import com.david.astrodrill.entity.Player;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.machine.Machine;
import com.david.astrodrill.screen.PlayScreen;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot / restore for the entire player save: player, vault, hub, world delta, machines.
 * World state is stored as a delta against the seeded procedural generation — small payload
 * (a few KB typically) instead of dumping every grid cell.
 */
public final class SaveStateSerializer {

    public static final int SCHEMA_VERSION = 1;

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static final int COLS = PlayScreen.COLS_PUBLIC;
    private static final int ROWS = PlayScreen.ROWS_PUBLIC;
    private static final float BS = PlayScreen.BLOCK_SIZE_PUBLIC;
    private static final int HUB_COL_START = 47;
    private static final int HUB_COL_END = 53;
    private static final int HUB_FOUNDATION_DEPTH = 0;

    private SaveStateSerializer() {}

    // ───────────────────────── DTOs ─────────────────────────

    public static class GameSaveDto {
        public int schemaVersion = SCHEMA_VERSION;
        public PlayerDto player;
        public Map<String, Integer> globalVault;
        public HubDto hub;
        public WorldDto world;
    }

    public static class PlayerDto {
        public float x, y;
        public float currentBattery, maxBattery;
        public int drillStrength, batteryTier, jetpackTier, wheelTier;
        public int activeSlot;
        public Map<String, Integer> inventory;          // BlockType name -> count
        public Map<String, Integer> machineInventory;   // ItemType name -> count
    }

    public static class HubDto {
        public float x, y, width, height;
        public int tier;
    }

    public static class WorldDto {
        public long seed;
        /** Cells where a procedurally-generated non-bedrock block has been removed. */
        public List<int[]> minedCells;
        /** Player-placed blocks: [col, row, typeName]. */
        public List<PlacedBlockDto> placedBlocks;
        public List<MachineDto> machines;
    }

    public static class PlacedBlockDto {
        public int col, row;
        public String type;
    }

    public static class MachineDto {
        public String type;
        public float x, y;
        public float processTimer;
        public boolean userDisabled;
    }

    // ───────────────────────── snapshot ─────────────────────────

    public static String snapshot(PlayScreen play, Player player, LanderHub hub) {
        GameSaveDto dto = buildSnapshot(play, player, hub);
        return GSON.toJson(dto);
    }

    public static GameSaveDto buildSnapshot(PlayScreen play, Player player, LanderHub hub) {
        GameSaveDto dto = new GameSaveDto();
        dto.player = snapshotPlayer(player);
        dto.globalVault = enumMapToString(GameManager.getInstance().getGlobalVault());
        dto.hub = snapshotHub(hub);
        dto.world = snapshotWorld(play);
        return dto;
    }

    private static PlayerDto snapshotPlayer(Player p) {
        PlayerDto dto = new PlayerDto();
        dto.x = p.x;
        dto.y = p.y;
        dto.currentBattery = p.currentBattery;
        dto.maxBattery = p.maxBattery;
        dto.drillStrength = p.drillStrength;
        dto.batteryTier = p.batteryTier;
        dto.jetpackTier = p.jetpackTier;
        dto.wheelTier = p.wheelTier;
        dto.activeSlot = p.activeSlot;
        dto.inventory = new HashMap<>();
        for (Map.Entry<BlockType, Integer> e : p.inventory.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                dto.inventory.put(e.getKey().name(), e.getValue());
            }
        }
        dto.machineInventory = new HashMap<>();
        for (Map.Entry<ItemType, Integer> e : p.machineInventory.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                dto.machineInventory.put(e.getKey().name(), e.getValue());
            }
        }
        return dto;
    }

    private static HubDto snapshotHub(LanderHub h) {
        HubDto dto = new HubDto();
        dto.x = h.x;
        dto.y = h.y;
        dto.width = h.width;
        dto.height = h.height;
        dto.tier = h.tier;
        return dto;
    }

    private static WorldDto snapshotWorld(PlayScreen play) {
        WorldDto dto = new WorldDto();
        dto.seed = play.getWorldSeed();
        dto.minedCells = new ArrayList<>();
        dto.placedBlocks = new ArrayList<>();
        dto.machines = new ArrayList<>();

        // Build position -> current block map for O(1) lookup
        Map<Long, Block> currentByPos = new HashMap<>();
        Array<Block> active = play.getActiveBlocks();
        for (int i = 0; i < active.size; i++) {
            Block b = active.get(i);
            if (!b.active) continue;
            int col = Math.round(b.x / BS);
            int row = Math.round(-b.y / BS);
            currentByPos.put(encode(col, row), b);

            if (b.isPlayerPlaced) {
                PlacedBlockDto pb = new PlacedBlockDto();
                pb.col = col;
                pb.row = row;
                pb.type = b.type.name();
                dto.placedBlocks.add(pb);
            }
        }

        // Regenerate to find mined positions (anywhere original had non-bedrock
        // but current world has nothing OR a player-placed block).
        RandomXS128 rng = new RandomXS128(play.getWorldSeed());
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                BlockType originalType = rollOriginalType(rng, c, r);
                if (originalType == BlockType.BEDROCK) continue;
                Block current = currentByPos.get(encode(c, r));
                if (current == null || current.isPlayerPlaced) {
                    dto.minedCells.add(new int[] { c, r });
                }
            }
        }

        for (Machine m : play.getActiveMachines()) {
            MachineDto md = new MachineDto();
            md.type = m.getMachineType();
            md.x = m.x;
            md.y = m.y;
            md.processTimer = m.processTimer;
            md.userDisabled = m.userDisabled;
            dto.machines.add(md);
        }

        return dto;
    }

    // ───────────────────────── restore ─────────────────────────

    public static GameSaveDto parse(String json) {
        if (json == null || json.isEmpty()) return null;
        return GSON.fromJson(json, GameSaveDto.class);
    }

    // ───────────────────────── helpers ─────────────────────────

    /**
     * Re-runs the same generation rules as PlayScreen.generateWorld() to derive the
     * original block type at a given cell. Must stay in lockstep with the generator.
     */
    public static BlockType rollOriginalType(RandomXS128 rng, int c, int r) {
        if (c >= HUB_COL_START && c <= HUB_COL_END && r <= HUB_FOUNDATION_DEPTH) {
            return BlockType.BEDROCK;
        }
        if (r == 0) return BlockType.DIRT;

        float chance = rng.nextFloat();
        if (r < 50) {
            if (chance < 0.03f) return BlockType.IRON_ORE;
            else if (chance < 0.08f) return BlockType.COPPER_ORE;
            else if (chance < 0.15f) return BlockType.COAL_ORE;
            else if (chance < 0.30f) return BlockType.STONE;
            else return BlockType.DIRT;
        } else if (r < 150) {
            if (chance < 0.04f) return BlockType.GOLD_ORE;
            else if (chance < 0.10f) return BlockType.SILICON_ORE;
            else if (chance < 0.25f) return BlockType.STONE;
            else return BlockType.BASALT;
        } else {
            if (chance < 0.15f) return BlockType.URANIUM_ORE;
            else return BlockType.OBSIDIAN;
        }
    }

    private static long encode(int col, int row) {
        return ((long) col) * (ROWS + 1L) + (long) row;
    }

    private static Map<String, Integer> enumMapToString(Map<ItemType, Integer> src) {
        Map<String, Integer> out = new HashMap<>();
        if (src == null) return out;
        for (Map.Entry<ItemType, Integer> e : src.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                out.put(e.getKey().name(), e.getValue());
            }
        }
        return out;
    }

    public static Map<ItemType, Integer> stringMapToItemEnum(Map<String, Integer> src) {
        Map<ItemType, Integer> out = new HashMap<>();
        if (src == null) return out;
        for (Map.Entry<String, Integer> e : src.entrySet()) {
            try { out.put(ItemType.valueOf(e.getKey()), e.getValue()); } catch (IllegalArgumentException ignore) {}
        }
        return out;
    }

    public static Map<BlockType, Integer> stringMapToBlockEnum(Map<String, Integer> src) {
        Map<BlockType, Integer> out = new HashMap<>();
        if (src == null) return out;
        for (Map.Entry<String, Integer> e : src.entrySet()) {
            try { out.put(BlockType.valueOf(e.getKey()), e.getValue()); } catch (IllegalArgumentException ignore) {}
        }
        return out;
    }
}
