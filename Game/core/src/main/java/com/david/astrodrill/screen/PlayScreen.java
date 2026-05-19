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
    private Vector3 pendingRightClick = null;
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

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 2.0f;
    private static final float ZOOM_STEP = 0.1f;

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
                Vector3 world = camera.unproject(new Vector3(screenX, screenY, 0));
                float gx = MathUtils.floor(world.x / BLOCK_SIZE) * BLOCK_SIZE;
                float gy = MathUtils.floor(world.y / BLOCK_SIZE) * BLOCK_SIZE;
                if (button == Input.Buttons.LEFT) {
                    pendingClick = new Vector3(gx, gy, 0);
                    return true;
                }
                if (button == Input.Buttons.RIGHT) {
                    pendingRightClick = new Vector3(gx, gy, 0);
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
     * Returns null (no break) if drillStrength is too low for the block's stratum.
     */
    public Block.BlockType mineBlockAt(float x, float y) {
        for (int i = 0; i < activeBlocks.size; i++) {
            Block block = activeBlocks.get(i);
            if (block.active && block.bounds.contains(x, y)) {
                if (!block.isDestructible) return null;
                if (block.isPlayerPlaced) return null;
                if (isMachineSupportedBy(block)) return null;
                if (player != null && !player.canMine(block.type)) return null;

                Block.BlockType type = block.type;
                activeBlocks.removeIndex(i);
                blockPool.free(block);
                return type;
            }
        }
        return null;
    }

    /** Read-only block lookup at (x,y) — used to peek before showing "drill too weak" popup. */
    private Block findBlockAt(float x, float y) {
        for (int i = 0; i < activeBlocks.size; i++) {
            Block block = activeBlocks.get(i);
            if (block.active && block.bounds.contains(x, y)) return block;
        }
        return null;
    }

    /**
     * Substepped X movement + collision. Returns true if any collision was hit
     * (used by horizontal mining to detect "pressed against a wall").
     */
    private boolean substepAxisX(float totalDx, float worldMaxX) {
        boolean blocked = false;
        int steps = Math.max(1, (int) Math.ceil(Math.abs(totalDx) / 0.1f));
        float stepDx = totalDx / steps;
        for (int s = 0; s < steps; s++) {
            if (stepDx == 0f) break;
            player.x += stepDx;
            if (player.x < 0f) { player.x = 0f; player.velocityX = 0f; stepDx = 0f; blocked = true; }
            if (player.x > worldMaxX) { player.x = worldMaxX; player.velocityX = 0f; stepDx = 0f; blocked = true; }
            player.bounds.x = player.x;
            for (Block block : activeBlocks) {
                if (!block.active || !player.bounds.overlaps(block.bounds)) continue;
                float blockLeft = block.bounds.x;
                float blockRight = block.bounds.x + block.bounds.width;
                boolean straddleLeft  = (player.x < blockLeft)  && (player.x + player.width  > blockLeft);
                boolean straddleRight = (player.x < blockRight) && (player.x + player.width  > blockRight);
                if (straddleRight && !straddleLeft) {
                    player.x = blockRight;                  // came from the right
                } else if (straddleLeft && !straddleRight) {
                    player.x = blockLeft - player.width;    // came from the left
                } else if (player.velocityX > 0) {
                    player.x = blockLeft - player.width;
                } else if (player.velocityX < 0) {
                    player.x = blockRight;
                }
                player.bounds.x = player.x;
                player.velocityX = 0f;
                stepDx = 0f;
                blocked = true;
                break;
            }
        }
        return blocked;
    }

    /** Substepped Y movement + collision. Same shape as the X version. */
    private void substepAxisY(float totalDy) {
        int steps = Math.max(1, (int) Math.ceil(Math.abs(totalDy) / 0.1f));
        float stepDy = totalDy / steps;
        for (int s = 0; s < steps; s++) {
            if (stepDy == 0f) break;
            player.y += stepDy;
            player.bounds.y = player.y;
            for (Block block : activeBlocks) {
                if (!block.active || !player.bounds.overlaps(block.bounds)) continue;
                float blockBottom = block.bounds.y;
                float blockTop = block.bounds.y + block.bounds.height;
                boolean straddleBottom = (player.y < blockBottom) && (player.y + player.height > blockBottom);
                boolean straddleTop    = (player.y < blockTop)    && (player.y + player.height > blockTop);
                if (straddleTop && !straddleBottom) {
                    player.y = blockTop;                       // landed from above
                } else if (straddleBottom && !straddleTop) {
                    player.y = blockBottom - player.height;    // bonked from below
                } else if (player.velocityY > 0) {
                    player.y = blockBottom - player.height;
                } else if (player.velocityY < 0) {
                    player.y = blockTop;
                }
                player.bounds.y = player.y;
                player.velocityY = 0f;
                stepDy = 0f;
                break;
            }
        }
    }

    /**
     * Non-destructive lookup: returns the type of the block directly below the
     * machine (used by AutoMiner). Returns null for empty cells, bedrock, or
     * player-placed blocks. The block is NOT removed — the AutoMiner taps the
     * resource without consuming it.
     */
    public Block.BlockType peekBlockTypeBelow(Machine m) {
        Block target = findBlockAt(m.x + m.width / 2f, m.y - 0.1f);
        if (target == null || !target.isDestructible || target.isPlayerPlaced) return null;
        return target.type;
    }

    private float drillTooWeakCooldown = 0f;

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Cap delta so a huge first-frame (asset loading) can't tunnel the player
        // through the terrain before collision has a chance to catch them.
        if (delta > 0.05f) delta = 0.05f;

        player.update(delta);

        // ── Hotbar Slot Selection (Number Keys 1-9) ──────────────────────
        for (int i = 0; i < 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                player.setActiveSlot(i);
            }
        }

        // ── Camera Zoom (+/-): smaller zoom = closer in ──────────────────
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS) || Gdx.input.isKeyJustPressed(Input.Keys.PLUS)) {
            camera.zoom = Math.max(MIN_ZOOM, camera.zoom - ZOOM_STEP);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            camera.zoom = Math.min(MAX_ZOOM, camera.zoom + ZOOM_STEP);
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

        // ── Right-Click: toggle machine on/off ─────────────────────────────
        if (pendingRightClick != null) {
            float dx = pendingRightClick.x + 0.5f - player.getCenterX();
            float dy = pendingRightClick.y + 0.5f - player.getCenterY();
            if (dx * dx + dy * dy <= Player.INTERACT_RADIUS * Player.INTERACT_RADIUS) {
                for (Machine m : activeMachines) {
                    if (Math.abs(m.x - pendingRightClick.x) < 0.01f
                            && Math.abs(m.y - pendingRightClick.y) < 0.01f) {
                        m.userDisabled = !m.userDisabled;
                        String name = formatMachineName(m.getMachineType());
                        hud.showBankingPopup(name + (m.userDisabled ? " OFF" : " ON"));
                        break;
                    }
                }
            }
            pendingRightClick = null;
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

        // Substepped axis-separated collision. We move in <=0.1-unit slices so the player
        // can't tunnel a full cell, and decide push direction from overlap geometry
        // (which edge does the player straddle?) so the resolver works even when the
        // previous position was already invalid. Falls back to velocity sign only when
        // the player is fully inside a block (player height 0.8 < block height 1.0).
        float worldMaxX = COLS * BLOCK_SIZE - player.width;
        boolean blockedX = substepAxisX(player.velocityX * delta, worldMaxX);
        substepAxisY(player.velocityY * delta);

        // Mining logic (drill never breaks player-placed blocks — enforced in mineBlockAt)
        drillTooWeakCooldown -= delta;
        digTimer -= delta;
        if (digTimer <= 0) {
            float tx = -999f, ty = -999f;

            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                tx = player.x + player.width / 2; ty = player.y - 0.1f;
            } else if ((Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) && blockedX) {
                tx = player.x - 0.1f; ty = player.y + player.height / 2;
            } else if ((Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) && blockedX) {
                tx = player.x + player.width + 0.1f; ty = player.y + player.height / 2;
            }

            if (tx != -999f) {
                // Peek the target block to give clear feedback when the drill is too weak
                Block target = findBlockAt(tx, ty);
                if (target != null && target.isDestructible && !target.isPlayerPlaced
                        && !player.canMine(target.type) && drillTooWeakCooldown <= 0f) {
                    hud.showBankingPopup("Drill too weak for this layer");
                    drillTooWeakCooldown = 1.0f;
                }

                Block.BlockType mined = mineBlockAt(tx, ty);
                if (mined != null) {
                    player.addBlockToInventory(mined);
                    // Scale cooldown inversely to speed so a wheel-upgraded player
                    // doesn't outrun their drill. 0.85 / speed keeps mining ~15% faster
                    // than 1-block walk time at every wheel tier.
                    digTimer = 0.85f / player.speed;
                }
            }
        }

        // Flight Transition — must be at hub with required cargo
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            GameManager gm = GameManager.getInstance();
            int fuel = gm.getItemCount(ItemType.ROCKET_FUEL);
            int plate = gm.getItemCount(ItemType.HULL_PLATING);
            int chip = gm.getItemCount(ItemType.CIRCUIT_BOARD);
            if (!player.isInHubZone) {
                hud.showBankingPopup("Return to the hub to launch");
            } else if (fuel < 20 || plate < 10 || chip < 5) {
                hud.showBankingPopup("Launch needs: 20 Fuel ("+fuel+"), 10 Plating ("+plate+"), 5 Circuits ("+chip+")");
            } else {
                gm.changeScreen(GameManager.ScreenType.FLIGHT);
            }
        }

        // Machine Update — two phases so CoalGenerator can read this frame's wantsPower from neighbors.
        // Phase 1: consumers. They set wantsPower based on their inputs, then process if powered.
        for (Machine machine : activeMachines) {
            if (machine instanceof CoalGenerator) continue;
            try {
                machine.update(delta, this);
            } catch (Exception ex) {
                Gdx.app.error("PlayScreen", "Machine update failed: " + machine.getMachineType(), ex);
            }
        }
        // Phase 2: generators. Burn coal proportional to adjacent demand set in Phase 1.
        for (Machine machine : activeMachines) {
            if (!(machine instanceof CoalGenerator)) continue;
            try {
                machine.update(delta, this);
            } catch (Exception ex) {
                Gdx.app.error("PlayScreen", "CoalGenerator update failed", ex);
            }
        }

        // Camera
        camera.position.x = player.x;
        camera.position.y = player.y;
        float halfViewW = camera.viewportWidth * camera.zoom / 2f;
        float worldWidth = COLS * BLOCK_SIZE;
        if (worldWidth > halfViewW * 2f) {
            if (camera.position.x < halfViewW) camera.position.x = halfViewW;
            if (camera.position.x > worldWidth - halfViewW) camera.position.x = worldWidth - halfViewW;
        } else {
            camera.position.x = worldWidth / 2f;
        }
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

        // Render machine FILLS (still inside the Filled pass)
        for (Machine machine : activeMachines) {
            shapeRenderer.setColor(machineFillColor(machine));
            shapeRenderer.rect(machine.x, machine.y, machine.width, machine.height);
        }

        // Render LanderHub
        shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 1f);
        shapeRenderer.rect(landerHub.x, landerHub.y, landerHub.width, landerHub.height);

        // Render Player
        shapeRenderer.setColor(0f, 0.5f, 1f, 1f);
        shapeRenderer.rect(player.x, player.y, player.width, player.height);

        shapeRenderer.end();

        // Separate pass for machine BORDERS
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Machine machine : activeMachines) {
            if (machine.userDisabled) shapeRenderer.setColor(1f, 0.2f, 0.2f, 1f);
            else if (machine.isPowered) shapeRenderer.setColor(Color.WHITE);
            else shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
            shapeRenderer.rect(machine.x, machine.y, machine.width, machine.height);
        }
        shapeRenderer.end();

        // Render machine symbols using SpriteBatch
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        machineFont.getData().setScale(0.02f);
        for (Machine machine : activeMachines) {
            String symbol = machine.getSymbol();
            if (symbol == null) symbol = "?";
            machineFont.setColor(machine.isPowered ? Color.WHITE : Color.GRAY);
            machineFont.draw(batch, symbol, machine.x + 0.5f - 0.15f, machine.y + 0.65f);
        }
        batch.end();

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

        // Prevent placing on bedrock foundation (only the bedrock row, not the air above it)
        int col = Math.round(gx / BLOCK_SIZE);
        int row = Math.round(-gy / BLOCK_SIZE);
        if (col >= HUB_COL_START && col <= HUB_COL_END && row == HUB_FOUNDATION_DEPTH) return;

        // Prevent placement that would overlap the LanderHub entity AABB
        Rectangle hubBounds = new Rectangle(landerHub.x, landerHub.y, landerHub.width, landerHub.height);
        if (placementCheck.overlaps(hubBounds)) return;

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

    /** Distinct color per machine type. Falls back to gray for unknown types. */
    private Color machineFillColor(Machine machine) {
        String type = machine.getMachineType();
        boolean on = machine.isPowered && !machine.userDisabled;
        if (type == null) return new Color(0.5f, 0.5f, 0.5f, 1f);
        switch (type) {
            case "CoalGenerator": return on ? new Color(1f, 0.4f, 0.1f, 1f)   : new Color(0.4f, 0.15f, 0.05f, 1f);
            case "IronSmelter":   return on ? new Color(1f, 0.65f, 0.2f, 1f)  : new Color(0.45f, 0.28f, 0.1f, 1f);
            case "CopperSmelter": return on ? new Color(0.95f, 0.55f, 0.25f, 1f) : new Color(0.42f, 0.24f, 0.12f, 1f);
            case "GoldSmelter":   return on ? new Color(1f, 0.85f, 0.2f, 1f)  : new Color(0.5f, 0.42f, 0.1f, 1f);
            case "GearAssembler": return on ? new Color(0.2f, 0.95f, 0.9f, 1f) : new Color(0.08f, 0.45f, 0.42f, 1f);
            case "WireAssembler": return on ? new Color(0.8f, 0.4f, 1f, 1f)   : new Color(0.35f, 0.15f, 0.5f, 1f);
            case "AutoMiner":     return on ? new Color(1f, 1f, 0.3f, 1f)     : new Color(0.5f, 0.15f, 0.15f, 1f);
            case "Refinery":      return on ? new Color(0.6f, 0.8f, 0.95f, 1f) : new Color(0.25f, 0.35f, 0.45f, 1f);
            case "CircuitFab":    return on ? new Color(0.3f, 0.95f, 0.5f, 1f) : new Color(0.12f, 0.45f, 0.22f, 1f);
            case "FuelMixer":     return on ? new Color(0.5f, 1f, 0.3f, 1f)   : new Color(0.2f, 0.45f, 0.12f, 1f);
            case "HullPress":     return on ? new Color(0.7f, 0.7f, 0.85f, 1f) : new Color(0.3f, 0.3f, 0.4f, 1f);
            default:              return new Color(0.5f, 0.5f, 0.5f, 1f);
        }
    }

    /** Pretty name for popups: "IronSmelter" → "Iron Smelter". */
    private static String formatMachineName(String type) {
        if (type == null) return "Machine";
        switch (type) {
            case "IronSmelter":   return "Iron Smelter";
            case "CopperSmelter": return "Copper Smelter";
            case "GoldSmelter":   return "Gold Smelter";
            case "CoalGenerator": return "Coal Generator";
            case "GearAssembler": return "Gear Assembler";
            case "WireAssembler": return "Wire Assembler";
            case "Refinery":      return "Refinery";
            case "CircuitFab":    return "Circuit Fab";
            case "FuelMixer":     return "Fuel Mixer";
            case "HullPress":     return "Hull Press";
            case "AutoMiner":     return "Auto Miner";
            default:              return type;
        }
    }

    /** Maps a Machine's type string back to an ItemType for inventory return. */
    private ItemType machineTypeToItemType(String type) {
        switch (type) {
            case "AutoMiner":      return ItemType.AUTO_MINER;
            case "CoalGenerator":  return ItemType.COAL_GENERATOR;
            case "IronSmelter":    return ItemType.IRON_SMELTER;
            case "CopperSmelter":  return ItemType.COPPER_SMELTER;
            case "GoldSmelter":    return ItemType.GOLD_SMELTER;
            case "GearAssembler":  return ItemType.GEAR_ASSEMBLER;
            case "WireAssembler":  return ItemType.WIRE_ASSEMBLER;
            case "Refinery":       return ItemType.REFINERY;
            case "CircuitFab":     return ItemType.CIRCUIT_FAB;
            case "FuelMixer":      return ItemType.FUEL_MIXER;
            case "HullPress":      return ItemType.HULL_PRESS;
            default:               return null;
        }
    }
}
