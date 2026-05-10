package com.david.astrodrill.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.david.astrodrill.entity.Block;
import com.david.astrodrill.entity.Block.BlockType;
import com.david.astrodrill.entity.Player;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.david.astrodrill.ui.Hud;
import com.david.astrodrill.machine.Machine;
import com.david.astrodrill.machine.MachineFactory;
import com.david.astrodrill.GameManager;
import com.david.astrodrill.entity.LanderHub;
import com.david.astrodrill.machine.CoalGenerator;
import com.david.astrodrill.machine.Smelter;
import com.david.astrodrill.machine.Assembler;
import java.util.ArrayList;
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
    private LanderHub landerHub;
    private float powerTickTimer = 0f;
    
    private final Pool<Block> blockPool = new Pool<Block>() {
        @Override
        protected Block newObject() {
            return new Block();
        }
    };

    private static final int COLS = 100;
    private static final int ROWS = 300;
    private static final float BLOCK_SIZE = 1f;

    // LanderHub foundation columns (7 blocks wide centered under hub)
    private static final int HUB_COL_START = 47;
    private static final int HUB_COL_END = 53; // inclusive
    private static final int HUB_FOUNDATION_DEPTH = 0;

    @Override
    public void show() {
        camera = new OrthographicCamera();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        hud = new Hud(batch);
        
        generateWorld();
        
        // Spawn LanderHub resting on top of the dirt at the center (centered on column 50)
        // Hub width is 3, so it spans columns 49, 50, 51.
        landerHub = new LanderHub(49f, 1f, 3f, 3f);

        // Spawn player 1 block to the right of the hub
        player = new Player(53f, 1f, BLOCK_SIZE * 0.8f, BLOCK_SIZE * 0.8f);
        player.addObserver(hud);
        hud.setPlayer(player);
        
        GameManager.getInstance().addObserver(hud);
    }
    
    private void generateWorld() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Block block = blockPool.obtain();
                
                BlockType type;

                // LanderHub bedrock foundation: 7 blocks wide at row 0
                if (c >= HUB_COL_START && c <= HUB_COL_END && r <= HUB_FOUNDATION_DEPTH) {
                    type = BlockType.BEDROCK;
                }
                // Row 0: flat dirt surface
                else if (r == 0) {
                    type = BlockType.DIRT;
                }
                // Crust Layer (depth 1-49)
                else if (r < 50) {
                    float chance = (float) Math.random();
                    if (chance < 0.03f) {
                        type = BlockType.IRON_ORE;
                    } else if (chance < 0.08f) {
                        type = BlockType.COPPER_ORE;
                    } else if (chance < 0.15f) {
                        type = BlockType.COAL_ORE;
                    } else if (chance < 0.30f) {
                        type = BlockType.STONE;
                    } else {
                        type = BlockType.DIRT;
                    }
                }
                // Mantle Layer (depth 50-149)
                else if (r < 150) {
                    float chance = (float) Math.random();
                    if (chance < 0.04f) {
                        type = BlockType.GOLD_ORE;
                    } else if (chance < 0.10f) {
                        type = BlockType.SILICON_ORE;
                    } else if (chance < 0.25f) {
                        type = BlockType.STONE;
                    } else {
                        type = BlockType.BASALT;
                    }
                }
                // Core Layer (depth 150-299)
                else {
                    float chance = (float) Math.random();
                    if (chance < 0.15f) {
                        type = BlockType.URANIUM_ORE;
                    } else {
                        type = BlockType.OBSIDIAN;
                    }
                }
                
                // Blocks are generated going downwards
                block.init(c * BLOCK_SIZE, -r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE, type);
                
                activeBlocks.add(block);
            }
        }
    }

    /**
     * Checks whether any active machine is resting on the given block.
     * Used to protect load-bearing blocks from being mined.
     */
    private boolean isMachineSupportedBy(Block block) {
        for (Machine machine : activeMachines) {
            // A machine sits on this block if its bottom is at or near the block's top
            // and they overlap horizontally
            boolean horizontalOverlap = machine.x < block.bounds.x + block.bounds.width
                && machine.x + machine.width > block.bounds.x;
            boolean verticalSupport = Math.abs(machine.y - (block.bounds.y + block.bounds.height)) < 0.15f;
            if (horizontalOverlap && verticalSupport) {
                return true;
            }
        }
        return false;
    }

    public Block.BlockType mineBlockAt(float x, float y) {
        for (int i = 0; i < activeBlocks.size; i++) {
            Block block = activeBlocks.get(i);
            if (block.active && block.bounds.contains(x, y)) {
                // Prevent mining indestructible blocks (BEDROCK)
                if (!block.isDestructible) {
                    return null;
                }
                // Prevent mining blocks that support a machine
                if (isMachineSupportedBy(block)) {
                    return null;
                }
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
        
        boolean blockedY = false;
        for (Block block : activeBlocks) {
            if (block.active && player.bounds.overlaps(block.bounds)) {
                blockedY = true;
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

        // Mining logic
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

        // Machine Placement Logic
        placeTimer -= delta;
        if (Gdx.input.isKeyPressed(Input.Keys.M) && placeTimer <= 0) {
            if (player.hasResources(BlockType.IRON_ORE, 5) && player.hasResources(BlockType.COPPER_ORE, 5)) {
                player.consumeResources(BlockType.IRON_ORE, 5);
                player.consumeResources(BlockType.COPPER_ORE, 5);
                Machine miner = MachineFactory.createMachine("AutoMiner", player.x, player.y);
                if (miner != null) {
                    activeMachines.add(miner);
                }
                placeTimer = 0.5f;
            }
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.G) && placeTimer <= 0) {
            if (player.hasResources(BlockType.IRON_ORE, 10)) {
                player.consumeResources(BlockType.IRON_ORE, 10);
                Machine gen = MachineFactory.createMachine("CoalGenerator", player.x, player.y);
                if (gen != null) {
                    activeMachines.add(gen);
                }
                placeTimer = 0.5f;
            }
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.O) && placeTimer <= 0) {
            if (player.hasResources(BlockType.IRON_ORE, 15)) {
                player.consumeResources(BlockType.IRON_ORE, 15);
                Machine smelter = MachineFactory.createMachine("Smelter", player.x, player.y);
                if (smelter != null) {
                    activeMachines.add(smelter);
                }
                placeTimer = 0.5f;
            }
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.P) && placeTimer <= 0) {
            if (player.hasResources(BlockType.IRON_ORE, 20)) {
                player.consumeResources(BlockType.IRON_ORE, 20);
                Machine assembler = MachineFactory.createMachine("Assembler", player.x, player.y);
                if (assembler != null) {
                    activeMachines.add(assembler);
                }
                placeTimer = 0.5f;
            }
        }
        
        // Assembler Recipe Switch
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            for (Machine m : activeMachines) {
                if (m instanceof Assembler) {
                    Assembler assembler = (Assembler) m;
                    if (assembler.currentRecipe == Assembler.Recipe.IRON_GEAR) {
                        assembler.currentRecipe = Assembler.Recipe.COPPER_WIRE;
                    } else {
                        assembler.currentRecipe = Assembler.Recipe.IRON_GEAR;
                    }
                }
            }
        }

        // Flight Transition Logic
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            GameManager.getInstance().changeScreen(GameManager.ScreenType.FLIGHT);
        }

        // Power Grid Algorithm
        powerTickTimer += delta;
        if (powerTickTimer >= 0.5f) {
            powerTickTimer = 0f;
            List<Machine> poweredQueue = new ArrayList<>();
            
            for (Machine m : activeMachines) {
                m.isPowered = false;
                if (m instanceof CoalGenerator) {
                    if (((CoalGenerator)m).isGenerating()) {
                        m.isPowered = true;
                        poweredQueue.add(m);
                    }
                }
            }
            
            int head = 0;
            while (head < poweredQueue.size()) {
                Machine current = poweredQueue.get(head++);
                for (Machine other : activeMachines) {
                    if (!other.isPowered && other.isAdjacentTo(current.x, current.y, current.width, current.height)) {
                        other.isPowered = true;
                        poweredQueue.add(other);
                    }
                }
            }
        }

        // Machine Update Logic
        for (Machine machine : activeMachines) {
            machine.update(delta, this);
            
            // Basic gravity and collision for machines
            machine.y += Player.GRAVITY * delta * delta * 60; // simple fallback
            boolean machineBlockedY = false;
            for (Block block : activeBlocks) {
                if (block.active && 
                    machine.x < block.bounds.x + block.bounds.width &&
                    machine.x + machine.width > block.bounds.x &&
                    machine.y < block.bounds.y + block.bounds.height &&
                    machine.y + machine.height > block.bounds.y) {
                    machineBlockedY = true;
                    if (Player.GRAVITY < 0) {
                        machine.y = block.bounds.y + block.bounds.height;
                    }
                    break;
                }
            }
        }

        // Update camera to follow player, clamped to world bounds
        camera.position.x = player.x;
        camera.position.y = player.y;

        // Clamp camera X so it doesn't pan past the world edges
        float halfViewW = camera.viewportWidth / 2f;
        float worldWidth = COLS * BLOCK_SIZE;
        if (camera.position.x < halfViewW) camera.position.x = halfViewW;
        if (camera.position.x > worldWidth - halfViewW) camera.position.x = worldWidth - halfViewW;

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for (Block block : activeBlocks) {
            if (block.active) {
                switch(block.type) {
                    case DIRT:
                        shapeRenderer.setColor(0.54f, 0.27f, 0.07f, 1f); // Brown
                        break;
                    case STONE:
                        shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f); // Gray
                        break;
                    case COPPER_ORE:
                        shapeRenderer.setColor(0.8f, 0.4f, 0.0f, 1f); // Orange
                        break;
                    case IRON_ORE:
                        shapeRenderer.setColor(0.75f, 0.75f, 0.75f, 1f); // Silver
                        break;
                    case COAL_ORE:
                        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f); // Charcoal
                        break;
                    case BASALT:
                        shapeRenderer.setColor(0.25f, 0.25f, 0.25f, 1f); // Dark Gray
                        break;
                    case GOLD_ORE:
                        shapeRenderer.setColor(1f, 0.84f, 0f, 1f); // Bright Yellow
                        break;
                    case SILICON_ORE:
                        shapeRenderer.setColor(0.6f, 0.8f, 0.9f, 1f); // Light Blue-Gray
                        break;
                    case URANIUM_ORE:
                        shapeRenderer.setColor(0.2f, 1f, 0.2f, 1f); // Neon Green
                        break;
                    case OBSIDIAN:
                        shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 1f); // Near Black
                        break;
                    case BEDROCK:
                        shapeRenderer.setColor(0.1f, 0.1f, 0.3f, 1f); // Dark Blue
                        break;
                    default:
                        shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f); // Default
                        break;
                }
                shapeRenderer.rect(block.x, block.y, block.width, block.height);
            }
        }
        
        // Render Machines
        for (Machine machine : activeMachines) {
            if (machine instanceof Smelter) {
                if (machine.isPowered) shapeRenderer.setColor(1f, 0.6f, 0f, 1f); // Bright Orange
                else shapeRenderer.setColor(0.5f, 0.3f, 0f, 1f); // Dark Orange
            } else if (machine instanceof Assembler) {
                if (machine.isPowered) shapeRenderer.setColor(0f, 1f, 1f, 1f); // Bright Cyan
                else shapeRenderer.setColor(0f, 0.5f, 0.5f, 1f); // Dark Cyan
            } else {
                if (machine.isPowered) {
                    shapeRenderer.setColor(1f, 1f, 0f, 1f); // Bright Yellow
                } else {
                    shapeRenderer.setColor(0.5f, 0f, 0f, 1f); // Dark Red
                }
            }
            shapeRenderer.rect(machine.x, machine.y, machine.width, machine.height);
        }
        
        // Render LanderHub
        shapeRenderer.setColor(0.4f, 0.4f, 0.4f, 1f); // Massive Gray rectangle
        shapeRenderer.rect(landerHub.x, landerHub.y, landerHub.width, landerHub.height);
        
        // Render Player
        shapeRenderer.setColor(0f, 0.5f, 1f, 1f); // Bright Blue
        shapeRenderer.rect(player.x, player.y, player.width, player.height);
        
        shapeRenderer.end();

        hud.updateBattery();
        hud.stage.act(delta);
        hud.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        // Show ~30 blocks across for readable scale even on the 100-wide grid
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

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        hud.dispose();
        for (Block block : activeBlocks) {
            blockPool.free(block);
        }
        activeBlocks.clear();
    }
}
