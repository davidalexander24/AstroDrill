package com.david.astrodrill.network;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.entity.Block.BlockType;
import com.david.astrodrill.entity.LanderHub;
import com.david.astrodrill.entity.Player;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.machine.Machine;
import com.david.astrodrill.screen.PlayScreen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot / restore for the entire player save: player, vault, hub, world delta, machines.
 * World state is stored as a delta against the seeded procedural generation — small payload
 * (a few KB typically) instead of dumping every grid cell.
 *
 * Reflection-free: writes via StringBuilder + JsonUtil.escape, reads via JsonReader+JsonValue.
 * This is what makes the save flow work on the GWT/HTML build (libGDX's reflective Json
 * silently fails in browsers because GWT strips reflection metadata).
 */
public final class SaveStateSerializer {

    public static final int SCHEMA_VERSION = 1;

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
        public long playtimeMs;
        public boolean hasLaunchedSuccessfully;
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

    // ───────────────────────── snapshot (write) ─────────────────────────

    public static String snapshot(PlayScreen play, Player player, LanderHub hub) {
        GameSaveDto dto = buildSnapshot(play, player, hub);
        StringBuilder sb = new StringBuilder(256);
        writeGameSave(dto, sb);
        return sb.toString();
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
        dto.playtimeMs = p.playtimeMs;
        dto.hasLaunchedSuccessfully = p.hasLaunchedSuccessfully;
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

    // ───────────────────────── restore (read) ─────────────────────────

    public static GameSaveDto parse(String json) {
        if (json == null || json.isEmpty()) return null;
        JsonValue root = new JsonReader().parse(json);
        return parseGameSave(root);
    }

    // ───────────────────────── JSON writers ─────────────────────────

    private static void writeGameSave(GameSaveDto dto, StringBuilder sb) {
        sb.append('{');
        sb.append("\"schemaVersion\":").append(dto.schemaVersion);
        if (dto.player != null) {
            sb.append(",\"player\":");
            writePlayer(dto.player, sb);
        }
        sb.append(",\"globalVault\":");
        writeStringIntMap(dto.globalVault, sb);
        if (dto.hub != null) {
            sb.append(",\"hub\":");
            writeHub(dto.hub, sb);
        }
        if (dto.world != null) {
            sb.append(",\"world\":");
            writeWorld(dto.world, sb);
        }
        sb.append('}');
    }

    private static void writePlayer(PlayerDto p, StringBuilder sb) {
        sb.append('{');
        sb.append("\"x\":").append(p.x);
        sb.append(",\"y\":").append(p.y);
        sb.append(",\"currentBattery\":").append(p.currentBattery);
        sb.append(",\"maxBattery\":").append(p.maxBattery);
        sb.append(",\"drillStrength\":").append(p.drillStrength);
        sb.append(",\"batteryTier\":").append(p.batteryTier);
        sb.append(",\"jetpackTier\":").append(p.jetpackTier);
        sb.append(",\"wheelTier\":").append(p.wheelTier);
        sb.append(",\"activeSlot\":").append(p.activeSlot);
        sb.append(",\"playtimeMs\":").append(p.playtimeMs);
        sb.append(",\"hasLaunchedSuccessfully\":").append(p.hasLaunchedSuccessfully);
        sb.append(",\"inventory\":");
        writeStringIntMap(p.inventory, sb);
        sb.append(",\"machineInventory\":");
        writeStringIntMap(p.machineInventory, sb);
        sb.append('}');
    }

    private static void writeHub(HubDto h, StringBuilder sb) {
        sb.append('{');
        sb.append("\"x\":").append(h.x);
        sb.append(",\"y\":").append(h.y);
        sb.append(",\"width\":").append(h.width);
        sb.append(",\"height\":").append(h.height);
        sb.append(",\"tier\":").append(h.tier);
        sb.append('}');
    }

    private static void writeWorld(WorldDto w, StringBuilder sb) {
        sb.append('{');
        sb.append("\"seed\":").append(w.seed);
        sb.append(",\"minedCells\":");
        writeIntPairList(w.minedCells, sb);
        sb.append(",\"placedBlocks\":[");
        if (w.placedBlocks != null) {
            boolean first = true;
            for (PlacedBlockDto pb : w.placedBlocks) {
                if (pb == null) continue;
                if (!first) sb.append(',');
                writePlacedBlock(pb, sb);
                first = false;
            }
        }
        sb.append(']');
        sb.append(",\"machines\":[");
        if (w.machines != null) {
            boolean first = true;
            for (MachineDto md : w.machines) {
                if (md == null) continue;
                if (!first) sb.append(',');
                writeMachine(md, sb);
                first = false;
            }
        }
        sb.append(']');
        sb.append('}');
    }

    private static void writePlacedBlock(PlacedBlockDto pb, StringBuilder sb) {
        sb.append('{');
        sb.append("\"col\":").append(pb.col);
        sb.append(",\"row\":").append(pb.row);
        sb.append(",\"type\":\"").append(JsonUtil.escape(pb.type)).append('"');
        sb.append('}');
    }

    private static void writeMachine(MachineDto m, StringBuilder sb) {
        sb.append('{');
        sb.append("\"type\":\"").append(JsonUtil.escape(m.type)).append('"');
        sb.append(",\"x\":").append(m.x);
        sb.append(",\"y\":").append(m.y);
        sb.append(",\"processTimer\":").append(m.processTimer);
        sb.append(",\"userDisabled\":").append(m.userDisabled);
        sb.append('}');
    }

    private static void writeStringIntMap(Map<String, Integer> map, StringBuilder sb) {
        sb.append('{');
        if (map != null) {
            boolean first = true;
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                if (!first) sb.append(',');
                sb.append('"').append(JsonUtil.escape(e.getKey())).append("\":").append(e.getValue().intValue());
                first = false;
            }
        }
        sb.append('}');
    }

    private static void writeIntPairList(List<int[]> list, StringBuilder sb) {
        sb.append('[');
        if (list != null) {
            boolean first = true;
            for (int[] pair : list) {
                if (pair == null || pair.length < 2) continue;
                if (!first) sb.append(',');
                sb.append('[').append(pair[0]).append(',').append(pair[1]).append(']');
                first = false;
            }
        }
        sb.append(']');
    }

    // ───────────────────────── JSON parsers ─────────────────────────

    private static GameSaveDto parseGameSave(JsonValue root) {
        if (root == null || root.isNull()) return null;
        GameSaveDto dto = new GameSaveDto();
        dto.schemaVersion = root.getInt("schemaVersion", SCHEMA_VERSION);
        if (root.has("player") && !root.get("player").isNull()) {
            dto.player = parsePlayer(root.get("player"));
        }
        if (root.has("globalVault")) {
            dto.globalVault = parseStringIntMap(root.get("globalVault"));
        }
        if (root.has("hub") && !root.get("hub").isNull()) {
            dto.hub = parseHub(root.get("hub"));
        }
        if (root.has("world") && !root.get("world").isNull()) {
            dto.world = parseWorld(root.get("world"));
        }
        return dto;
    }

    private static PlayerDto parsePlayer(JsonValue node) {
        PlayerDto p = new PlayerDto();
        p.x = node.getFloat("x", 0f);
        p.y = node.getFloat("y", 0f);
        p.currentBattery = node.getFloat("currentBattery", 0f);
        p.maxBattery = node.getFloat("maxBattery", 0f);
        p.drillStrength = node.getInt("drillStrength", 0);
        p.batteryTier = node.getInt("batteryTier", 0);
        p.jetpackTier = node.getInt("jetpackTier", 0);
        p.wheelTier = node.getInt("wheelTier", 0);
        p.activeSlot = node.getInt("activeSlot", 0);
        p.playtimeMs = node.getLong("playtimeMs", 0L);
        p.hasLaunchedSuccessfully = node.getBoolean("hasLaunchedSuccessfully", false);
        p.inventory = node.has("inventory") ? parseStringIntMap(node.get("inventory")) : new HashMap<>();
        p.machineInventory = node.has("machineInventory") ? parseStringIntMap(node.get("machineInventory")) : new HashMap<>();
        return p;
    }

    private static HubDto parseHub(JsonValue node) {
        HubDto h = new HubDto();
        h.x = node.getFloat("x", 0f);
        h.y = node.getFloat("y", 0f);
        h.width = node.getFloat("width", 0f);
        h.height = node.getFloat("height", 0f);
        h.tier = node.getInt("tier", 1);
        return h;
    }

    private static WorldDto parseWorld(JsonValue node) {
        WorldDto w = new WorldDto();
        w.seed = node.getLong("seed", 0L);
        w.minedCells = parseIntPairList(node.get("minedCells"));
        w.placedBlocks = new ArrayList<>();
        JsonValue pbArr = node.get("placedBlocks");
        if (pbArr != null && !pbArr.isNull()) {
            for (JsonValue c = pbArr.child; c != null; c = c.next) {
                w.placedBlocks.add(parsePlacedBlock(c));
            }
        }
        w.machines = new ArrayList<>();
        JsonValue mArr = node.get("machines");
        if (mArr != null && !mArr.isNull()) {
            for (JsonValue c = mArr.child; c != null; c = c.next) {
                w.machines.add(parseMachine(c));
            }
        }
        return w;
    }

    private static PlacedBlockDto parsePlacedBlock(JsonValue node) {
        PlacedBlockDto pb = new PlacedBlockDto();
        pb.col = node.getInt("col", 0);
        pb.row = node.getInt("row", 0);
        pb.type = node.getString("type", null);
        return pb;
    }

    private static MachineDto parseMachine(JsonValue node) {
        MachineDto m = new MachineDto();
        m.type = node.getString("type", null);
        m.x = node.getFloat("x", 0f);
        m.y = node.getFloat("y", 0f);
        m.processTimer = node.getFloat("processTimer", 0f);
        m.userDisabled = node.getBoolean("userDisabled", false);
        return m;
    }

    private static Map<String, Integer> parseStringIntMap(JsonValue node) {
        Map<String, Integer> out = new HashMap<>();
        if (node == null || node.isNull()) return out;
        for (JsonValue c = node.child; c != null; c = c.next) {
            if (c.name == null) continue;
            out.put(c.name, c.asInt());
        }
        return out;
    }

    private static List<int[]> parseIntPairList(JsonValue node) {
        List<int[]> out = new ArrayList<>();
        if (node == null || node.isNull()) return out;
        for (JsonValue c = node.child; c != null; c = c.next) {
            int[] arr = c.asIntArray();
            if (arr != null && arr.length >= 2) {
                out.add(new int[] { arr[0], arr[1] });
            }
        }
        return out;
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
