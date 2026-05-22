package com.david.astrodrill.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.audio.Music;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.entity.Block.BlockType;
import com.david.astrodrill.entity.Player;
import com.david.astrodrill.item.ItemType;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.david.astrodrill.network.BackendClient;
import com.david.astrodrill.network.BackendException;
import com.david.astrodrill.network.SaveStateSerializer;
import com.david.astrodrill.network.dto.SaveRequest;
import com.david.astrodrill.network.dto.SaveResponse;
import com.david.astrodrill.ui.Hud;
import com.david.astrodrill.machine.Machine;
import com.david.astrodrill.machine.MachineFactory;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.entity.LanderHub;
import com.david.astrodrill.machine.CoalGenerator;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.david.astrodrill.ui.Fonts;
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
    private float machineStateTime = 0f;
    private BitmapFont machineFont;
    private Rectangle placementCheck = new Rectangle();
    private Sound mineSound;
    private Music drillSound;
    private Sound placeSound;
    private Sound jetpackSound;
    private float jetpackSoundTimer = 0f;

    // Auto-banking: only bank once per zone entry
    private boolean hasbankedThisEntry = false;

    // Save status
    private int sessionMaxDepth = 0;
    private boolean savingInFlight = false;

    private Texture bgTexture;

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

    public static final int COLS_PUBLIC = COLS;
    public static final int ROWS_PUBLIC = ROWS;
    public static final float BLOCK_SIZE_PUBLIC = BLOCK_SIZE;

    private long worldSeed;
    public long getWorldSeed() { return worldSeed; }
    public Array<Block> getActiveBlocks() { return activeBlocks; }
    public LanderHub getLanderHub() { return landerHub; }
    public Player getPlayer() { return player; }
    public Pool<Block> getBlockPool() { return blockPool; }

    private boolean initialized = false;

    @Override
    public void show() {
        if (!initialized) {
            camera = new OrthographicCamera();
            shapeRenderer = new ShapeRenderer();
            batch = new SpriteBatch();
            GameManager.getInstance().ensureTexturesLoaded();
            hud = new Hud(batch);

            String pending = GameManager.getInstance().consumePendingSaveBlob();
            SaveStateSerializer.GameSaveDto dto = pending != null ? SaveStateSerializer.parse(pending) : null;

            long seed = (dto != null && dto.world != null) ? dto.world.seed : System.currentTimeMillis();
            generateWorld(seed);

            landerHub = new LanderHub(47.5f, 1f, 6f, 3f);

            machineFont = Fonts.create(12, Color.WHITE, 1f);

            mineSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Mine.wav"));
            drillSound = Gdx.audio.newMusic(Gdx.files.internal("sounds/Drill.wav"));
            placeSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Place.wav"));
            jetpackSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Jetpack.wav"));

            bgTexture = new Texture(Gdx.files.internal("textures/space_bg.png"));
            bgTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

            player = new Player(53f, 1f, BLOCK_SIZE * 0.8f, BLOCK_SIZE * 0.8f);
            player.addObserver(hud);
            hud.setPlayer(player);
            hud.setLanderHub(landerHub);

            GameManager.getInstance().addObserver(hud);

            if (dto != null) {
                restoreFromDto(dto);
            }

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
            initialized = true;
        }
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    private void generateWorld(long seed) {
        this.worldSeed = seed;
        RandomXS128 rng = new RandomXS128(seed);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Block block = blockPool.obtain();
                BlockType type;

                if (c >= HUB_COL_START && c <= HUB_COL_END && r <= HUB_FOUNDATION_DEPTH) {
                    type = BlockType.BEDROCK;
                } else if (r == ROWS - 1) {
                    type = BlockType.BEDROCK;
                } else if (r == 0) {
                    type = BlockType.DIRT;
                } else if (r < 50) {
                    float chance = rng.nextFloat();
                    if (chance < 0.03f) type = BlockType.IRON_ORE;
                    else if (chance < 0.08f) type = BlockType.COPPER_ORE;
                    else if (chance < 0.15f) type = BlockType.COAL_ORE;
                    else if (chance < 0.30f) type = BlockType.STONE;
                    else type = BlockType.DIRT;
                } else if (r < 150) {
                    float chance = rng.nextFloat();
                    if (chance < 0.04f) type = BlockType.GOLD_ORE;
                    else if (chance < 0.10f) type = BlockType.SILICON_ORE;
                    else if (chance < 0.25f) type = BlockType.STONE;
                    else type = BlockType.BASALT;
                } else {
                    float chance = rng.nextFloat();
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

        machineStateTime += delta;
        player.update(delta);

        // Jetpack Sound Effect
        if (player.isJetting) {
            if (jetpackSoundTimer <= 0f) {
                if (jetpackSound != null) {
                    jetpackSound.play(GameManager.getInstance().getSfxVolume());
                }
                jetpackSoundTimer = 0.36f;
            }
            jetpackSoundTimer -= delta;
        } else {
            jetpackSoundTimer = 0f;
        }

        // ── Hotbar Slot Selection (Number Keys 1-9) ──────────────────────
        for (int i = 0; i < 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                player.setActiveSlot(i);
            }
        }

        // ── Manual Save (F5) ────────────────────────────────────────────
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            requestSave(false);
        }

        // Track session max depth (positive number — deeper = larger).
        int curDepth = Math.max(0, (int) Math.floor(-player.y));
        if (curDepth > sessionMaxDepth) sessionMaxDepth = curDepth;

        // Accumulate playtime so the leaderboard's "fastest launch" reflects real game time.
        player.playtimeMs += (long) (delta * 1000f);

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
                        GameManager.getInstance().playMenuSound();
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
                    mineSound.play(GameManager.getInstance().getSfxVolume());
                    if (drillSound != null && !drillSound.isPlaying()) {
                        drillSound.setVolume(GameManager.getInstance().getSfxVolume());
                        drillSound.play();
                    }
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
        GameManager gm = GameManager.getInstance();

        // Frustum AABB — skip anything outside the visible camera bounds so we
        // don't pay SpriteBatch / ShapeRenderer cost for the other ~29k blocks.
        float halfViewW2 = camera.viewportWidth  * camera.zoom * 0.5f + BLOCK_SIZE;
        float halfViewH2 = camera.viewportHeight * camera.zoom * 0.5f + BLOCK_SIZE;
        float viewMinX = camera.position.x - halfViewW2;
        float viewMaxX = camera.position.x + halfViewW2;
        float viewMinY = camera.position.y - halfViewH2;
        float viewMaxY = camera.position.y + halfViewH2;

        float viewW = camera.viewportWidth * camera.zoom;
        float viewH = camera.viewportHeight * camera.zoom;

        // Render parallax background
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        int srcX = (int)(camera.position.x * 20f);
        int srcY = (int)(-camera.position.y * 20f);
        batch.draw(bgTexture, camera.position.x - viewW / 2f, camera.position.y - viewH / 2f, 
                   viewW, viewH, srcX, srcY, (int)(viewW * 20f), (int)(viewH * 20f), false, false);
        batch.end();

        // Pass 1: textured blocks via SpriteBatch.
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.setColor(Color.WHITE);
        for (Block block : activeBlocks) {
            if (!block.active) continue;
            if (block.x + block.width  < viewMinX || block.x > viewMaxX) continue;
            if (block.y + block.height < viewMinY || block.y > viewMaxY) continue;
            TextureRegion region = gm.getBlockTex(block.type);
            if (region == null) continue;
            batch.draw(region, block.x, block.y, block.width, block.height);
        }
        // Additive overlay so player-placed blocks read brighter than world-generated ones.
        batch.flush();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(0.18f, 0.18f, 0.18f, 1f);
        for (Block block : activeBlocks) {
            if (!block.active || !block.isPlayerPlaced) continue;
            if (block.x + block.width  < viewMinX || block.x > viewMaxX) continue;
            if (block.y + block.height < viewMinY || block.y > viewMaxY) continue;
            TextureRegion region = gm.getBlockTex(block.type);
            if (region == null) continue;
            batch.draw(region, block.x, block.y, block.width, block.height);
        }
        batch.flush();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);

        // LanderHub sprite (falls back to gray rect in the ShapeRenderer pass if absent).
        TextureRegion hubRegion = gm.getHubTex();
        if (hubRegion != null) {
            // Offset Y slightly because the 192x96 texture has 10px of transparent padding at the bottom,
            // which causes it to float above the ground. (10/96 * height)
            float renderY = landerHub.y - (10f / 96f) * landerHub.height;
            batch.draw(hubRegion, landerHub.x, renderY, landerHub.width, landerHub.height);
        }

        // Machine sprites (state border still drawn in the Line pass below).
        // Pick on or off variant based on Machine.isRunning(); fall back to on if off missing.
        for (Machine machine : activeMachines) {
            if (machine.x + machine.width  < viewMinX || machine.x > viewMaxX) continue;
            if (machine.y + machine.height < viewMinY || machine.y > viewMaxY) continue;
            TextureRegion mTex;
            if (machine.isRunning()) {
                mTex = gm.getMachineFrame(machine, machineStateTime);
            } else {
                mTex = gm.getMachineOffTex(machine);
                if (mTex == null) mTex = gm.getMachineTex(machine);
            }
            if (mTex != null) {
                batch.draw(mTex, machine.x, machine.y, machine.width, machine.height);
            }
        }

        // Player rover sprite (flipped for left-facing) drawn before the ShapeRenderer
        // pass so machine borders and hub render on top if they overlap.
        TextureRegion playerRegion = gm.getPlayerFrame(player.facingLeft, player.stateTime, player.isMoving);
        if (playerRegion != null) {
            float scale = 1.8f;
            float renderW = player.width * scale;
            float renderH = player.height * scale;
            float renderX = player.x - (renderW - player.width) / 2f;
            // The player_anim.png has ~10px of transparent padding at the bottom of the 128px sprite.
            float renderY = player.y - (10f / 128f) * renderH;
            batch.draw(playerRegion, renderX, renderY, renderW, renderH);
        }
        batch.end();

        // Pass 2: ShapeRenderer for everything still untextured (machines, hub,
        // fallback for any block whose PNG hasn't been generated yet).
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Block block : activeBlocks) {
            if (!block.active) continue;
            if (gm.getBlockTex(block.type) != null) continue;
            if (block.x + block.width  < viewMinX || block.x > viewMaxX) continue;
            if (block.y + block.height < viewMinY || block.y > viewMaxY) continue;
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

        // Render machine FILLS only when no sprite is available (fallback).
        for (Machine machine : activeMachines) {
            if (gm.getMachineTex(machine) != null) continue;
            if (machine.x + machine.width  < viewMinX || machine.x > viewMaxX) continue;
            if (machine.y + machine.height < viewMinY || machine.y > viewMaxY) continue;
            shapeRenderer.setColor(machineFillColor(machine));
            shapeRenderer.rect(machine.x, machine.y, machine.width, machine.height);
        }

        // Render LanderHub fallback (gray rect) only if no sprite was drawn above.
        if (hubRegion == null) {
            shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 1f);
            shapeRenderer.rect(landerHub.x, landerHub.y, landerHub.width, landerHub.height);
        }

        // Fallback player rect (only if the rover sprite is missing).
        if (playerRegion == null) {
            shapeRenderer.setColor(0f, 0.5f, 1f, 1f);
            shapeRenderer.rect(player.x, player.y, player.width, player.height);
        }

        shapeRenderer.end();


        // Render machine symbols using SpriteBatch — only for machines without a sprite.
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        machineFont.getData().setScale(0.02f);
        for (Machine machine : activeMachines) {
            if (gm.getMachineTex(machine) != null) continue;
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
        if (mineSound != null) mineSound.dispose();
        if (drillSound != null) drillSound.dispose();
        if (placeSound != null) placeSound.dispose();
        if (jetpackSound != null) jetpackSound.dispose();
        if (bgTexture != null) bgTexture.dispose();
        shapeRenderer.dispose();
        batch.dispose();
        hud.dispose();
        for (Block block : activeBlocks) blockPool.free(block);
        activeBlocks.clear();
    }

    /**
     * Manually save the current game state to the backend. Async — toast is shown
     * on completion. Pass `silent=true` to suppress the success toast (e.g. during
     * save-on-exit).
     */
    public void requestSave(boolean silent) {
        requestSave(silent, null);
    }

    /**
     * Same as {@link #requestSave(boolean)} but invokes {@code onSuccess} after the save
     * POST completes successfully. Used by Save &amp; Exit so the screen transition only
     * happens once the save row is in the DB — otherwise MainMenuScreen.checkForSave()
     * races the in-flight POST and the Continue button loads a stale state.
     * {@code onSuccess} is NOT invoked on save failure; the player stays on PlayScreen
     * with the "Save failed: ..." toast and can retry.
     */
    public void requestSave(boolean silent, final Runnable onSuccess) {
        Long pid = GameManager.getInstance().getCurrentPlayerId();
        if (pid == null) {
            if (!silent) hud.showBankingPopup("Not logged in — cannot save");
            return;
        }
        if (savingInFlight) {
            if (!silent) hud.showBankingPopup("Save already in progress...");
            return;
        }
        savingInFlight = true;
        if (!silent) hud.showBankingPopup("Saving...");

        String json = SaveStateSerializer.snapshot(this, player, landerHub);
        long launchTime = player.hasLaunchedSuccessfully ? player.playtimeMs : 0L;
        SaveRequest req = new SaveRequest(
                pid, json, 0, "PROXIMA_B", sessionMaxDepth, launchTime);

        BackendClient.save(req, new BackendClient.Callback<SaveResponse>() {
            @Override
            public void onSuccess(SaveResponse result) {
                savingInFlight = false;
                if (!silent) hud.showBankingPopup("Saved.");
                if (onSuccess != null) onSuccess.run();
            }

            @Override
            public void onError(BackendException ex) {
                savingInFlight = false;
                hud.showBankingPopup("Save failed: " + ex.getMessage());
            }
        });
    }

    public boolean isSavingInFlight() { return savingInFlight; }
    public int getSessionMaxDepth() { return sessionMaxDepth; }

    /**
     * Restores the world to match a previously-saved snapshot. Must be called after
     * generateWorld(seed) and after the player+hub+hud are constructed. World state
     * is reapplied through the normal placeBlock/mining codepaths so the BEDROCK,
     * isPlayerPlaced, and isDestructible invariants from CLAUDE.md remain enforced.
     */
    private void restoreFromDto(SaveStateSerializer.GameSaveDto dto) {
        // Player
        if (dto.player != null) {
            player.x = dto.player.x;
            player.y = dto.player.y;
            player.bounds.x = player.x;
            player.bounds.y = player.y;
            player.drillStrength = dto.player.drillStrength;
            player.batteryTier = dto.player.batteryTier;
            player.jetpackTier = dto.player.jetpackTier;
            player.wheelTier = dto.player.wheelTier;
            player.applyBatteryUpgrade();
            player.applyJetpackUpgrade();
            player.applyWheelUpgrade();
            player.currentBattery = Math.min(dto.player.currentBattery, player.maxBattery);
            player.activeSlot = Math.min(Math.max(0, dto.player.activeSlot), Player.HOTBAR_SLOTS - 1);
            player.playtimeMs = dto.player.playtimeMs;
            player.hasLaunchedSuccessfully = dto.player.hasLaunchedSuccessfully;

            player.inventory.clear();
            player.inventory.putAll(SaveStateSerializer.stringMapToBlockEnum(dto.player.inventory));
            player.machineInventory.clear();
            player.machineInventory.putAll(SaveStateSerializer.stringMapToItemEnum(dto.player.machineInventory));
        }

        // Vault
        GameManager.getInstance().replaceGlobalVault(
                SaveStateSerializer.stringMapToItemEnum(dto.globalVault));

        // Hub
        if (dto.hub != null) {
            landerHub.x = dto.hub.x;
            landerHub.y = dto.hub.y;
            landerHub.width = dto.hub.width;
            landerHub.height = dto.hub.height;
            landerHub.tier = Math.max(1, dto.hub.tier);
            
            // Migrate old saves where the hub was 3x3 to the new 6x3 dimensions
            if (landerHub.width == 3f) {
                landerHub.width = 6f;
                landerHub.x = 47.5f;
            }
        }

        // World — apply mined deletions and placed additions on top of the regenerated world
        if (dto.world != null) {
            if (dto.world.minedCells != null) {
                for (int[] cell : dto.world.minedCells) {
                    if (cell == null || cell.length < 2) continue;
                    removeBlockAtCell(cell[0], cell[1], false);
                }
            }
            if (dto.world.placedBlocks != null) {
                for (SaveStateSerializer.PlacedBlockDto pb : dto.world.placedBlocks) {
                    if (pb == null || pb.type == null) continue;
                    BlockType type;
                    try { type = BlockType.valueOf(pb.type); } catch (IllegalArgumentException ex) { continue; }
                    removeBlockAtCell(pb.col, pb.row, true);
                    Block b = blockPool.obtain();
                    b.init(pb.col * BLOCK_SIZE, -pb.row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE, type, true);
                    activeBlocks.add(b);
                }
            }
            if (dto.world.machines != null) {
                for (SaveStateSerializer.MachineDto md : dto.world.machines) {
                    if (md == null || md.type == null) continue;
                    Machine m = MachineFactory.createMachine(md.type, md.x, md.y);
                    if (m == null) continue;
                    m.processTimer = md.processTimer;
                    m.userDisabled = md.userDisabled;
                    activeMachines.add(m);
                }
            }
        }

        player.rebuildHotbar();
    }

    /**
     * Removes any block currently at the given (col,row), respecting bedrock when
     * allowBedrock=false. Used during save restoration only.
     */
    private void removeBlockAtCell(int col, int row, boolean allowBedrock) {
        for (int i = 0; i < activeBlocks.size; i++) {
            Block b = activeBlocks.get(i);
            if (!b.active) continue;
            int bc = Math.round(b.x / BLOCK_SIZE);
            int br = Math.round(-b.y / BLOCK_SIZE);
            if (bc == col && br == row) {
                if (!allowBedrock && b.type == BlockType.BEDROCK) return;
                activeBlocks.removeIndex(i);
                blockPool.free(b);
                return;
            }
        }
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
            placeSound.play(GameManager.getInstance().getSfxVolume());
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
        placeSound.play(GameManager.getInstance().getSfxVolume());
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
                mineSound.play(GameManager.getInstance().getSfxVolume());
                if (drillSound != null && !drillSound.isPlaying()) {
                    drillSound.setVolume(GameManager.getInstance().getSfxVolume());
                    drillSound.play();
                }
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
                mineSound.play(GameManager.getInstance().getSfxVolume());
                if (drillSound != null && !drillSound.isPlaying()) {
                    drillSound.setVolume(GameManager.getInstance().getSfxVolume());
                    drillSound.play();
                }
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
