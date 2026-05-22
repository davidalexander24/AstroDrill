package com.david.astrodrill.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.item.ItemType;
import com.david.astrodrill.observer.InventoryObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    public float x;
    public float y;
    public float width;
    public float height;
    public float velocityX;
    public float velocityY;
    /** True if the player rover is currently oriented facing left (used to flip the sprite). */
    public boolean facingLeft = false;
    public float speed;
    public float stateTime = 0f;
    public boolean isMoving = false;
    public boolean isJetting = false;
    public Rectangle bounds;
    public Map<Block.BlockType, Integer> inventory;
    private List<InventoryObserver> observers;
    
    private float spawnX;
    private float spawnY;
    
    public float maxBattery = 120f;
    public float currentBattery = 120f;

    public static final float BASE_SPEED = 6.5f;

    /** Set by PlayScreen each frame — true when inside the LanderHub safe zone. */
    public boolean isInHubZone = false;

    // ── Hotbar System ────────────────────────────────────────────────────
    public static final int HOTBAR_SLOTS = 9;

    /** The 9-slot hotbar. null means the slot is empty. Dynamically populated. */
    public final ItemType[] hotbar = new ItemType[HOTBAR_SLOTS];

    /** Currently selected hotbar slot (0-based). */
    public int activeSlot = 0;

    // Physics constants
    public static final float GRAVITY = -15f;
    public static final float MAX_FALL_SPEED = -10f;
    public static final float MAX_RISE_SPEED = 8f;
    public static final float JETPACK_BATTERY_DRAIN = 10f;

    /** Per-instance jetpack thrust; scales with jetpackTier upgrades. */
    public float jetpackThrust = 30f;

    // ── Upgrade Tiers ────────────────────────────────────────────────────
    /** Drill strength gates which strata can be mined: 1=Crust, 2=Mantle, 3=Core. */
    public int drillStrength = 1;
    public int batteryTier = 1;
    public int jetpackTier = 1;
    public int wheelTier = 1;

    /** Whether the current drill can break the given block type. */
    public boolean canMine(Block.BlockType t) {
        return drillStrength >= Block.requiredStrength(t);
    }

    /** Recomputes maxBattery from batteryTier and refills to full. */
    public void applyBatteryUpgrade() {
        this.maxBattery = 120f * batteryTier;
        this.currentBattery = this.maxBattery;
    }

    /** Recomputes jetpackThrust from jetpackTier. */
    public void applyJetpackUpgrade() {
        this.jetpackThrust = 30f + 15f * (jetpackTier - 1);
    }

    /** Recomputes horizontal speed from wheelTier. */
    public void applyWheelUpgrade() {
        this.speed = BASE_SPEED + 1.5f * (wheelTier - 1);
    }

    // ── Interaction Radius ───────────────────────────────────────────────
    public static final float INTERACT_RADIUS = 6f;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velocityX = 0;
        this.velocityY = 0;
        this.speed = BASE_SPEED;
        this.bounds = new Rectangle(x, y, width, height);
        this.spawnX = x;
        this.spawnY = y;
        this.inventory = new HashMap<>();
        this.observers = new ArrayList<>();
    }

    /** Separate inventory for crafted machines (ItemType → count). */
    public Map<ItemType, Integer> machineInventory = new HashMap<>();

    /**
     * Rebuilds the hotbar dynamically based on the player's personal inventory,
     * machine inventory, and the global vault.
     * Order: placeable blocks → machine items → DECONSTRUCT_TOOL (always last).
     */
    public void rebuildHotbar() {
        for (int i = 0; i < HOTBAR_SLOTS; i++) {
            hotbar[i] = null;
        }

        int slotIdx = 0;
        GameManager gm = GameManager.getInstance();

        // 1) Placeable blocks from personal inventory / vault
        Block.BlockType[] placeableBlocks = {
            Block.BlockType.DIRT, Block.BlockType.STONE,
            Block.BlockType.COAL_ORE, Block.BlockType.COPPER_ORE, Block.BlockType.IRON_ORE,
            Block.BlockType.BASALT, Block.BlockType.OBSIDIAN
        };

        for (Block.BlockType bt : placeableBlocks) {
            if (slotIdx >= HOTBAR_SLOTS - 1) break;

            ItemType it = blockTypeToItemType(bt);
            if (it == null) continue;

            int invCount = inventory.getOrDefault(bt, 0);
            int vaultCount = gm.getItemCount(it);

            if (invCount > 0 || vaultCount > 0) {
                hotbar[slotIdx++] = it;
            }
        }

        // 2) Machine items from machineInventory
        ItemType[] machineTypes = {
            ItemType.IRON_SMELTER, ItemType.COPPER_SMELTER, ItemType.GOLD_SMELTER,
            ItemType.COAL_GENERATOR,
            ItemType.GEAR_ASSEMBLER, ItemType.WIRE_ASSEMBLER,
            ItemType.REFINERY, ItemType.CIRCUIT_FAB,
            ItemType.FUEL_MIXER, ItemType.HULL_PRESS,
            ItemType.AUTO_MINER
        };
        for (ItemType mt : machineTypes) {
            if (slotIdx >= HOTBAR_SLOTS - 1) break;
            int count = machineInventory.getOrDefault(mt, 0);
            if (count > 0) {
                hotbar[slotIdx++] = mt;
            }
        }

        // 3) Deconstruct tool always last
        hotbar[HOTBAR_SLOTS - 1] = ItemType.DECONSTRUCT_TOOL;

        if (activeSlot >= HOTBAR_SLOTS) activeSlot = HOTBAR_SLOTS - 1;
    }

    /**
     * Adds a crafted machine to the hotbar's machine inventory.
     * Returns true if successful, false if hotbar is completely full.
     */
    public boolean addMachineToHotbar(ItemType machineType) {
        machineInventory.put(machineType, machineInventory.getOrDefault(machineType, 0) + 1);
        return true;
    }

    /** Consumes one machine item from the machine inventory. */
    public boolean consumeMachineItem(ItemType machineType) {
        int count = machineInventory.getOrDefault(machineType, 0);
        if (count <= 0) return false;
        machineInventory.put(machineType, count - 1);
        return true;
    }

    /** Returns the count of a given item in the slot's source (inventory, vault, or machineInventory). */
    public int getSlotItemCount(ItemType item) {
        if (item == null) return 0;

        // Check if it's a machine item
        if (isMachineItem(item)) {
            return machineInventory.getOrDefault(item, 0);
        }

        // Otherwise it's a block type
        Block.BlockType bt = itemTypeToBlockType(item);
        if (bt != null) {
            return getTotalResourceCount(bt);
        }
        return 0;
    }

    /** Returns true if this ItemType represents a placeable machine. */
    public static boolean isMachineItem(ItemType it) {
        return it == ItemType.AUTO_MINER || it == ItemType.COAL_GENERATOR
            || it == ItemType.IRON_SMELTER || it == ItemType.COPPER_SMELTER
            || it == ItemType.GOLD_SMELTER
            || it == ItemType.GEAR_ASSEMBLER || it == ItemType.WIRE_ASSEMBLER
            || it == ItemType.REFINERY || it == ItemType.CIRCUIT_FAB
            || it == ItemType.FUEL_MIXER || it == ItemType.HULL_PRESS;
    }

    /** Returns the MachineFactory key string for a machine ItemType. */
    public static String getMachineFactoryKey(ItemType it) {
        if (it == null) return null;
        switch (it) {
            case AUTO_MINER:       return "AutoMiner";
            case COAL_GENERATOR:   return "CoalGenerator";
            case IRON_SMELTER:     return "IronSmelter";
            case COPPER_SMELTER:   return "CopperSmelter";
            case GOLD_SMELTER:     return "GoldSmelter";
            case GEAR_ASSEMBLER:   return "GearAssembler";
            case WIRE_ASSEMBLER:   return "WireAssembler";
            case REFINERY:         return "Refinery";
            case CIRCUIT_FAB:      return "CircuitFab";
            case FUEL_MIXER:       return "FuelMixer";
            case HULL_PRESS:       return "HullPress";
            default:               return null;
        }
    }

    /** Maps a BlockType to the corresponding placeable ItemType. */
    public static ItemType blockTypeToItemType(Block.BlockType bt) {
        switch (bt) {
            case DIRT:  return ItemType.DIRT;
            case STONE: return ItemType.STONE;
            default:    return null;
        }
    }

    /** Maps a placeable ItemType back to a BlockType for world placement. */
    public static Block.BlockType itemTypeToBlockType(ItemType it) {
        if (it == null) return null;
        switch (it) {
            case DIRT:  return Block.BlockType.DIRT;
            case STONE: return Block.BlockType.STONE;
            default:    return null;
        }
    }

    /** Returns the ItemType in the currently active hotbar slot, or null. */
    public ItemType getActiveHotbarItem() {
        return hotbar[activeSlot];
    }

    /** Cycles the active slot forward or backward, wrapping around. */
    public void scrollHotbar(int direction) {
        activeSlot = ((activeSlot + direction) % HOTBAR_SLOTS + HOTBAR_SLOTS) % HOTBAR_SLOTS;
    }

    /** Directly set the active slot (0-based). */
    public void setActiveSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SLOTS) {
            activeSlot = slot;
        }
    }

    /**
     * Transfers all items from the player's personal inventory into the
     * GameManager global vault. Returns a summary string for the HUD popup.
     */
    public String bankInventoryToVault() {
        if (inventory.isEmpty()) return null;

        GameManager gm = GameManager.getInstance();
        StringBuilder banked = new StringBuilder();
        int totalBanked = 0;

        for (Map.Entry<Block.BlockType, Integer> entry : inventory.entrySet()) {
            int count = entry.getValue();
            if (count <= 0) continue;

            ItemType vaultType = blockTypeToRawItemType(entry.getKey());
            if (vaultType != null) {
                gm.addItems(vaultType, count);
                if (banked.length() > 0) banked.append(", ");
                banked.append(count).append("x ").append(formatName(entry.getKey().name()));
                totalBanked += count;
            }
        }

        inventory.clear();
        notifyObservers();

        if (totalBanked > 0) {
            return "Banked: " + banked.toString();
        }
        return null;
    }

    /** Maps a mined BlockType to the corresponding raw vault ItemType. */
    public static ItemType blockTypeToRawItemType(Block.BlockType bt) {
        switch (bt) {
            case IRON_ORE:    return ItemType.RAW_IRON;
            case COPPER_ORE:  return ItemType.RAW_COPPER;
            case COAL_ORE:    return ItemType.RAW_COAL;
            case GOLD_ORE:    return ItemType.RAW_GOLD;
            case SILICON_ORE: return ItemType.RAW_SILICON;
            case URANIUM_ORE: return ItemType.RAW_URANIUM;
            case OBSIDIAN:    return ItemType.RAW_OBSIDIAN;
            case BASALT:      return ItemType.STONE;
            case DIRT:        return ItemType.DIRT;
            case STONE:       return ItemType.STONE;
            default:          return null;
        }
    }

    private String formatName(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(p.charAt(0)).append(p.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    /** Returns the count of a given BlockType in the player's personal inventory. */
    public int getInventoryCount(Block.BlockType type) {
        return inventory.getOrDefault(type, 0);
    }

    /** Returns the total count of a given BlockType from both inventory and vault. */
    public int getTotalResourceCount(Block.BlockType type) {
        int inv = inventory.getOrDefault(type, 0);
        int vault = GameManager.getInstance().getItemCount(blockTypeToItemType(type));
        return inv + vault;
    }

    public void addObserver(InventoryObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers() {
        for (InventoryObserver observer : observers) {
            observer.onInventoryUpdated(inventory);
        }
    }

    public void addBlockToInventory(Block.BlockType type) {
        int count = inventory.getOrDefault(type, 0);
        inventory.put(type, count + 1);
        notifyObservers();
    }

    public boolean hasResources(Block.BlockType type, int amount) {
        int invCount = inventory.getOrDefault(type, 0);
        ItemType it = blockTypeToItemType(type);
        int vaultCount = (it != null) ? GameManager.getInstance().getItemCount(it) : 0;
        return (invCount + vaultCount) >= amount;
    }

    public void consumeResources(Block.BlockType type, int amount) {
        if (!hasResources(type, amount)) return;

        int invCount = inventory.getOrDefault(type, 0);
        if (invCount >= amount) {
            inventory.put(type, invCount - amount);
        } else {
            // Consume all from inventory, then the rest from vault
            inventory.put(type, 0);
            int remaining = amount - invCount;
            ItemType it = blockTypeToItemType(type);
            if (it != null) {
                GameManager.getInstance().consumeItems(it, remaining);
            }
        }
        notifyObservers();
    }

    public void update(float delta) {
        // Apply gravity
        velocityY += GRAVITY * delta;

        // Jetpack
        isJetting = false;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            if (currentBattery > 0) {
                velocityY += jetpackThrust * delta;
                isJetting = true;
            }
        }

        if (velocityY < MAX_FALL_SPEED) velocityY = MAX_FALL_SPEED;
        if (velocityY > MAX_RISE_SPEED) velocityY = MAX_RISE_SPEED;

        // Horizontal input
        velocityX = 0;
        boolean moving = false;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            velocityX = -speed;
            moving = true;
            facingLeft = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            velocityX = speed;
            moving = true;
            facingLeft = false;
        }

        // Battery drain
        boolean mining = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        if (isJetting) {
            currentBattery -= JETPACK_BATTERY_DRAIN * delta;
        } else if (moving || mining) {
            currentBattery -= 5f * delta;
        }

        if (currentBattery < 0) currentBattery = 0;
        if (currentBattery <= 0) respawn();

        isMoving = isJetting || moving;
        if (isMoving) {
            stateTime += delta;
        } else {
            stateTime = 0f;
        }
    }

    public void respawn() {
        this.x = spawnX;
        this.y = spawnY;
        this.velocityX = 0;
        this.velocityY = 0;
        this.currentBattery = this.maxBattery;
        this.inventory.clear();
        notifyObservers();
    }

    public void rechargeBattery(float rate, float delta) {
        currentBattery += rate * delta;
        if (currentBattery > maxBattery) currentBattery = maxBattery;
    }

    public float getCenterX() { return x + width / 2f; }
    public float getCenterY() { return y + height / 2f; }
}
