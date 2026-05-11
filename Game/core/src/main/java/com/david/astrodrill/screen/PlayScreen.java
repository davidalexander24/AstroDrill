package com.david.astrodrill.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.entity.Block.BlockType;
import com.david.astrodrill.entity.Player;
import com.david.astrodrill.item.ItemType;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.david.astrodrill.ui.Hud;
import com.david.astrodrill.machine.Machine;
import com.david.astrodrill.machine.MachineFactory;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.entity.LanderHub;
import com.david.astrodrill.machine.CoalGenerator;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayScreen implements Screen {

    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private Array<Block> activeBlocks = new Array<>();
    private Player player;
    private float digTimer = 0;
    private float placeTimer = 0;
    private SpriteBatch batch;
    private Hud hud;
    private List<Machine> activeMachines = new ArrayList<>();
    public List<Machine> getActiveMachines() { return activeMachines; }
    private LanderHub landerHub;
    private float powerTickTimer = 0f;
    private InputMultiplexer inputMultiplexer;
    private float clickCooldown = 0f;
    private Vector3 pendingClick = null;
    private BitmapFont machineFont;
    private Rectangle placementCheck = new Rectangle();

    // Auto-banking: only bank once per zone entry
    private boolean hasbankedThisEntry = false;

    private final Pool<Block> blockPool = new Pool<Block>() {
        @Override
        protected Block newObject() {
            return new Block();
        }
    };

    private static final int COLS = 100;
    private static final int ROWS = 300;
    private static final float BLOCK_SIZE = 1f;

    private static final int HUB_COL_START = 47;
    private static final int HUB_COL_END = 53;
    private static final int HUB_FOUNDATION_DEPTH = 0;

    @Override
    public void show() {
        camera = new OrthographicCamera();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        hud = new Hud(batch);

        generateWorld();

        landerHub = new LanderHub(49f, 1f, 3f, 3f);

        FreeTypeFontGenerator.FreeTypeFontParameter machineParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        machineParam.size = 12;
        machineParam.color = Color.WHITE;
        machineParam.borderWidth = 1f;
        machineParam.borderColor = Color.BLACK;
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
        machineFont = gen.generateFont(machineParam);
        gen.dispose();

        player = new Player(53f, 1f, BLOCK_SIZE * 0.8f, BLOCK_SIZE * 0.8f);
        player.addObserver(hud);
        hud.setPlayer(player);
        hud.setLanderHub(landerHub);

        GameManager.getInstance().addObserver(hud);

        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(hud.stage);
        inputMultiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
                    float gx = MathUtils.floor(world.x / BLOCK_SIZE) * BLOCK_SIZE;
                    float gy = MathUtils.floor(world.y / BLOCK_SIZE) * BLOCK_SIZE;
                    pendingClick = new Vector3(gx, gy, 0);
                    return true;
                }
                return false;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                player.scrollHotbar(amountY > 0 ? 1 : -1);
                return true;
            }
        });
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void generateWorld() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Block block = blockPool.obtain();
                BlockType type;

                if (c >= HUB_COL_START && c <= HUB_COL_END && r <= HUB_FOUNDATION_DEPTH) {
                    type = BlockType.BEDROCK;
                } else if (r == 0) {
                    type = BlockType.DIRT;
                } else if (r < 50) {
                    float chance = (float) Math.random();
                    if (chance < 0.03f) type = BlockType.IRON_ORE;
                    else if (chance < 0.08f) type = BlockType.COPPER_ORE;
                    else if (chance < 0.15f) type = BlockType.COAL_ORE;
                    else if (chance < 0.30f) type = BlockType.STONE;
                    else type = BlockType.DIRT;
                } else if (r < 150) {
                    float chance = (float) Math.random();
                    if (chance < 0.04f) type = BlockType.GOLD_ORE;
                    else if (chance < 0.10f) type = BlockType.SILICON_ORE;
                    else if (chance < 0.25f) type = BlockType.STONE;
                    else type = BlockType.BASALT;
                } else {
                    float chance = (float) Math.random();
                    if (chance < 0.15f) type = BlockType.URANIUM_ORE;
                    else type = BlockType.OBSIDIAN;
                }

                block.init(c * BLOCK_SIZE, -r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE, type);
                activeBlocks.add(block);
            }
        }
    }

    private boolean isMachineSupportedBy(Block block) {
        for (Machine machine : activeMachines) {
            boolean horizontalOverlap = machine.x < block.bounds.x + block.bounds.width
                && machine.x + machine.width > block.bounds.x;
            boolean verticalSupport = Math.abs(machine.y - (block.bounds.y + block.bounds.height)) < 0.15f;
            if (horizontalOverlap && verticalSupport) return true;
        }
        return false;
    }

    /**
     * Mines a block at (x,y). Used by player drill and AutoMiner.
     * NEVER breaks player-placed blocks or bedrock.
     */
    public Block.BlockType mineBlockAt(float x, float y) {
        for (int i = 0; i < activeBlocks.size; i++) {
            Block block = activeBlocks.get(i);
            if (block.active && block.bounds.contains(x, y)) {
                if (!block.isDestructible) return null;
                if (block.isPlayerPlaced) return null;   // drill/autominer cannot break player-placed
                if (isMachineSupportedBy(block)) return null;

                Block.BlockType type = block.type;
                activeBlocks.removeIndex(i);
                blockPool.free(block);
                return type;
            }
        }
        return null;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        player.update(delta);

        // ── Hotbar Slot Selection (Number Keys 1-9) ──────────────────────
        for (int i = 0; i < 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                player.setActiveSlot(i);
            }
        }

        // ── Sandbox Click: Placement / Deconstruction (with radius check) ──
        clickCooldown -= delta;
        if (pendingClick != null && clickCooldown <= 0) {
            float dx = pendingClick.x + 0.5f - player.getCenterX();
            float dy = pendingClick.y + 0.5f - player.getCenterY();
            float distSq = dx * dx + dy * dy;
            if (distSq <= Player.INTERACT_RADIUS * Player.INTERACT_RADIUS) {
                handleWorldClick(pendingClick.x, pendingClick.y);
            }
            pendingClick = null;
            clickCooldown = 0.15f;
        } else {
            pendingClick = null;
        }

        // ── Hub Proximity Detection & Recharge ───────────────────────────
        boolean wasInZone = player.isInHubZone;
        player.isInHubZone = landerHub.isInSafeZone(player.getCenterX(), player.getCenterY());

        if (player.isInHubZone) {
            player.rechargeBattery(LanderHub.RECHARGE_RATE, delta);

            // Auto-bank inventory on zone entry
            if (!hasbankedThisEntry) {
                String bankedMsg = player.bankInventoryToVault();
                if (bankedMsg != null) {
                    hud.showBankingPopup(bankedMsg);
                }
                hasbankedThisEntry = true;
            }
        } else {
            hasbankedThisEntry = false;
        }

        hud.setHubPanelVisible(player.isInHubZone);

        // X-axis movement and collision
        player.x += player.velocityX * delta;
        player.bounds.x = player.x;

        boolean blockedX = false;
        for (Block block : activeBlocks) {
            if (block.active && player.bounds.overlaps(block.bounds)) {
                blockedX = true;
                if (player.velocityX > 0) {
                    player.x = block.bounds.x - player.width;
                } else if (player.velocityX < 0) {
                    player.x = block.bounds.x + block.bounds.width;
                }
                player.bounds.x = player.x;
                player.velocityX = 0;
                break;
            }
        }

        // Y-axis movement and collision
        player.y += player.velocityY * delta;
        player.bounds.y = player.y;

        for (Block block : activeBlocks) {
            if (block.active && player.bounds.overlaps(block.bounds)) {
                if (player.velocityY < 0) {
                    player.y = block.bounds.y + block.bounds.height;
                } else if (player.velocityY > 0) {
                    player.y = block.bounds.y - player.height;
                }
                player.bounds.y = player.y;
                player.velocityY = 0;
                break;
            }
        }

        // Mining logic (drill never breaks player-placed blocks — enforced in mineBlockAt)
        digTimer -= delta;
        if (digTimer <= 0) {
            Block.BlockType mined = null;

            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                mined = mineBlockAt(player.x + player.width / 2, player.y - 0.1f);
            } else if ((Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) && blockedX) {
                mined = mineBlockAt(player.x - 0.1f, player.y + player.height / 2);
            } else if ((Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) && blockedX) {
                mined = mineBlockAt(player.x + player.width + 0.1f, player.y + player.height / 2);
            }

            if (mined != null) {
                player.addBlockToInventory(mined);
                digTimer = 0.2f;
            }
        }

        // Flight Transition
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            GameManager.getInstance().changeScreen(GameManager.ScreenType.FLIGHT);
        }

        // Power Grid: CoalGenerators self-power; all other machines check adjacency in their own update()
        for (Machine m : activeMachines) {
            if (m instanceof CoalGenerator) {
                m.isPowered = ((CoalGenerator) m).isActive();
            }
            // Non-generator machines set their own isPowered in update() via hasAdjacentPower()
        }

        // Machine Update (no gravity — machines are static grid objects)
        for (Machine machine : activeMachines) {
            machine.update(delta, this);
        }

        // Camera
        camera.position.x = player.x;
        camera.position.y = player.y;
        float halfViewW = camera.viewportWidth / 2f;
        float worldWidth = COLS * BLOCK_SIZE;
        if (camera.position.x < halfViewW) camera.position.x = halfViewW;
        if (camera.position.x > worldWidth - halfViewW) camera.position.x = worldWidth - halfViewW;
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        // ── Rendering ────────────────────────────────────────────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Block block : activeBlocks) {
            if (block.active) {
                switch(block.type) {
                    case DIRT:        shapeRenderer.setColor(0.54f, 0.27f, 0.07f, 1f); break;
                    case STONE:       shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f); break;
                    case COPPER_ORE:  shapeRenderer.setColor(0.8f, 0.4f, 0.0f, 1f); break;
                    case IRON_ORE:    shapeRenderer.setColor(0.75f, 0.75f, 0.75f, 1f); break;
                    case COAL_ORE:    shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f); break;
                    case BASALT:      shapeRenderer.setColor(0.25f, 0.25f, 0.25f, 1f); break;
                    case GOLD_ORE:    shapeRenderer.setColor(1f, 0.84f, 0f, 1f); break;
                    case SILICON_ORE: shapeRenderer.setColor(0.6f, 0.8f, 0.9f, 1f); break;
                    case URANIUM_ORE: shapeRenderer.setColor(0.2f, 1f, 0.2f, 1f); break;
                    case OBSIDIAN:    shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 1f); break;
                    case BEDROCK:     shapeRenderer.setColor(0.1f, 0.1f, 0.3f, 1f); break;
                    default:          shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f); break;
                }
                // Player-placed blocks get a slight tint (brighter) to distinguish them
                if (block.isPlayerPlaced) {
                    shapeRenderer.setColor(
                        Math.min(1f, shapeRenderer.getColor().r + 0.15f),
                        Math.min(1f, shapeRenderer.getColor().g + 0.15f),
                        Math.min(1f, shapeRenderer.getColor().b + 0.15f),
                        1f
                    );
                }
                shapeRenderer.rect(block.x, block.y, block.width, block.height);
            }
        }

        // Render Machines — distinct colors per type
        for (Machine machine : activeMachines) {
            String type = machine.getMachineType();
            Color machineColor;
            switch (type) {
                case "CoalGenerator":
                    machineColor = machine.isPowered ? new Color(1f, 0.4f, 0.1f, 1f) : new Color(0.4f, 0.15f, 0.05f, 1f);
                    break;
                case "IronSmelter":
                    machineColor = machine.isPowered ? new Color(1f, 0.65f, 0.2f, 1f) : new Color(0.45f, 0.28f, 0.1f, 1f);
                    break;
                case "CopperSmelter":
                    machineColor = machine.isPowered ? new Color(0.95f, 0.55f, 0.25f, 1f) : new Color(0.42f, 0.24f, 0.12f, 1f);
                    break;
                case "GearAssembler":
                    machineColor = machine.isPowered ? new Color(0.2f, 0.95f, 0.9f, 1f) : new Color(0.08f, 0.45f, 0.42f, 1f);
                    break;
                case "WireAssembler":
                    machineColor = machine.isPowered ? new Color(0.8f, 0.4f, 1f, 1f) : new Color(0.35f, 0.15f, 0.5f, 1f);
                    break;
                case "AutoMiner":
                    machineColor = machine.isPowered ? new Color(1f, 1f, 0.3f, 1f) : new Color(0.5f, 0.15f, 0.15f, 1f);
                    break;
                default:
                    machineColor = new Color(0.5f, 0.5f, 0.5f, 1f);
                    break;
            }
            shapeRenderer.setColor(machineColor);
            shapeRenderer.rect(machine.x, machine.y, machine.width, machine.height);
            
            // Border
            shapeRenderer.set(ShapeRenderer.ShapeType.Line);
            if (machine.isPowered) shapeRenderer.setColor(Color.WHITE);
            else shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
            shapeRenderer.rect(machine.x, machine.y, machine.width, machine.height);
            shapeRenderer.set(ShapeRenderer.ShapeType.Filled);
        }

        shapeRenderer.end();

        // Render machine symbols using SpriteBatch
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Machine machine : activeMachines) {
            String symbol = machine.getSymbol();
            // Center the text roughly. Each character is roughly 0.2-0.3 units wide in this world scale.
            // machineFont is generated with size 12, we need to scale it down to fit in 1x1 world units.
            machineFont.getData().setScale(0.02f); 
            machineFont.setColor(machine.isPowered ? Color.WHITE : Color.GRAY);
            machineFont.draw(batch, symbol, machine.x + 0.5f - 0.15f, machine.y + 0.65f);
        }
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Render LanderHub
        shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 1f);
        shapeRenderer.rect(landerHub.x, landerHub.y, landerHub.width, landerHub.height);

        // Render Player
        shapeRenderer.setColor(0f, 0.5f, 1f, 1f);
        shapeRenderer.rect(player.x, player.y, player.width, player.height);

        shapeRenderer.end();

        // HUD
        hud.updateBattery();
        hud.updateHotbar(delta);
        hud.stage.act(delta);
        hud.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        float viewportWidth = 30f * BLOCK_SIZE;
        float viewportHeight = viewportWidth * ((float) height / width);
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        if (player != null) {
            camera.position.x = player.x;
            camera.position.y = player.y;
        } else {
            camera.position.x = viewportWidth / 2f;
        }
        camera.update();
        hud.stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (machineFont != null) machineFont.dispose();
        shapeRenderer.dispose();
        batch.dispose();
        hud.dispose();
        for (Block block : activeBlocks) blockPool.free(block);
        activeBlocks.clear();
    }

    // ── Sandbox Construction Helpers ──────────────────────────────────────

    private void handleWorldClick(float gx, float gy) {
        ItemType active = player.getActiveHotbarItem();
        if (active == null) return;

        if (active == ItemType.DECONSTRUCT_TOOL) {
            deconstructAt(gx, gy);
        } else if (Player.isMachineItem(active)) {
            // Place a machine from machine inventory
            placeMachineAt(gx, gy, active);
        } else {
            // Try to place a block
            BlockType bt = Player.itemTypeToBlockType(active);
            if (bt != null) {
                placeBlockAt(gx, gy, bt);
            }
        }
    }

    private void placeMachineAt(float gx, float gy, ItemType machineType) {
        // Prevent placing on top of the player
        placementCheck.set(gx, gy, BLOCK_SIZE, BLOCK_SIZE);
        if (player.bounds.overlaps(placementCheck)) return;

        if (!player.consumeMachineItem(machineType)) return;

        // Check cell not occupied
        for (Block b : activeBlocks) {
            if (b.active && Math.abs(b.x - gx) < 0.01f && Math.abs(b.y - gy) < 0.01f) {
                player.addMachineToHotbar(machineType); // refund
                return;
            }
        }
        for (Machine m : activeMachines) {
            if (Math.abs(m.x - gx) < 0.01f && Math.abs(m.y - gy) < 0.01f) {
                player.addMachineToHotbar(machineType); // refund
                return;
            }
        }

        String key = Player.getMachineFactoryKey(machineType);
        Machine machine = MachineFactory.createMachine(key, gx, gy);
        if (machine != null) {
            activeMachines.add(machine);
        } else {
            player.addMachineToHotbar(machineType); // refund on failure
        }
    }

    private void placeBlockAt(float gx, float gy, BlockType type) {
        // Prevent placing on top of the player
        placementCheck.set(gx, gy, BLOCK_SIZE, BLOCK_SIZE);
        if (player.bounds.overlaps(placementCheck)) return;

        // Must have the block in inventory
        if (!player.hasResources(type, 1)) return;

        // Prevent placing on bedrock foundation
        int col = Math.round(gx / BLOCK_SIZE);
        int row = Math.round(-gy / BLOCK_SIZE);
        if (col >= HUB_COL_START && col <= HUB_COL_END && row <= HUB_FOUNDATION_DEPTH) return;

        // Check cell not occupied by block
        for (Block b : activeBlocks) {
            if (b.active && Math.abs(b.x - gx) < 0.01f && Math.abs(b.y - gy) < 0.01f) return;
        }

        // Check cell not occupied by machine
        for (Machine m : activeMachines) {
            if (Math.abs(m.x - gx) < 0.01f && Math.abs(m.y - gy) < 0.01f) return;
        }

        player.consumeResources(type, 1);
        Block block = blockPool.obtain();
        block.init(gx, gy, BLOCK_SIZE, BLOCK_SIZE, type, true); // isPlayerPlaced = true
        activeBlocks.add(block);
    }

    /**
     * Deconstruct tool: can ONLY break player-placed blocks (and machines).
     * Never breaks natural terrain or bedrock.
     */
    private void deconstructAt(float gx, float gy) {
        // Try removing a player-placed block
        for (int i = 0; i < activeBlocks.size; i++) {
            Block b = activeBlocks.get(i);
            if (b.active && Math.abs(b.x - gx) < 0.01f && Math.abs(b.y - gy) < 0.01f) {
                if (!b.isPlayerPlaced) return; // cannot deconstruct natural terrain
                // Return the block to inventory
                player.addBlockToInventory(b.type);
                activeBlocks.removeIndex(i);
                blockPool.free(b);
                return;
            }
        }

        // Try removing a machine — return it to machineInventory
        Iterator<Machine> it = activeMachines.iterator();
        while (it.hasNext()) {
            Machine m = it.next();
            if (Math.abs(m.x - gx) < 0.01f && Math.abs(m.y - gy) < 0.01f) {
                ItemType machineItem = machineTypeToItemType(m.getMachineType());
                if (machineItem != null) {
                    player.addMachineToHotbar(machineItem);
                }
                it.remove();
                return;
            }
        }
    }

    /** Maps a Machine's type string back to an ItemType for inventory return. */
    private ItemType machineTypeToItemType(String type) {
        switch (type) {
            case "AutoMiner":      return ItemType.AUTO_MINER;
            case "CoalGenerator":  return ItemType.COAL_GENERATOR;
            case "IronSmelter":    return ItemType.IRON_SMELTER;
            case "CopperSmelter":  return ItemType.COPPER_SMELTER;
            case "GearAssembler":  return ItemType.GEAR_ASSEMBLER;
            case "WireAssembler":  return ItemType.WIRE_ASSEMBLER;
            default:               return null;
        }
    }
}
