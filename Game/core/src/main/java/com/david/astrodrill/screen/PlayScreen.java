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
    
    private final Pool<Block> blockPool = new Pool<Block>() {
        @Override
        protected Block newObject() {
            return new Block();
        }
    };

    private static final int COLS = 20;
    private static final int ROWS = 50;
    private static final float BLOCK_SIZE = 1f;

    @Override
    public void show() {
        camera = new OrthographicCamera();
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        hud = new Hud(batch);
        
        generateWorld();
        
        // Spawn player on top of the center-most dirt block
        player = new Player(COLS / 2f * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE * 0.8f, BLOCK_SIZE * 0.8f);
        player.addObserver(hud);
    }
    
    private void generateWorld() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Block block = blockPool.obtain();
                
                BlockType type;
                if (r < 10) {
                    type = BlockType.DIRT;
                } else {
                    float chance = (float) Math.random();
                    if (chance < 0.1f) {
                        type = BlockType.COPPER;
                    } else if (chance < 0.2f) {
                        type = BlockType.IRON;
                    } else {
                        type = BlockType.STONE;
                    }
                }
                
                // Blocks are generated going downwards
                block.init(c * BLOCK_SIZE, -r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE, type);
                
                activeBlocks.add(block);
            }
        }
    }

    public boolean mineBlockAt(float x, float y) {
        for (int i = 0; i < activeBlocks.size; i++) {
            Block block = activeBlocks.get(i);
            if (block.active && block.bounds.contains(x, y)) {
                player.addBlockToInventory(block.type);
                
                activeBlocks.removeIndex(i);
                blockPool.free(block);
                return true;
            }
        }
        return false;
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
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                if (mineBlockAt(player.x + player.width / 2, player.y - 0.1f)) digTimer = 0.2f;
            } else if ((Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) && blockedX) {
                if (mineBlockAt(player.x - 0.1f, player.y + player.height / 2)) digTimer = 0.2f;
            } else if ((Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) && blockedX) {
                if (mineBlockAt(player.x + player.width + 0.1f, player.y + player.height / 2)) digTimer = 0.2f;
            }
        }

        // Machine Placement Logic
        placeTimer -= delta;
        if (Gdx.input.isKeyPressed(Input.Keys.M) && placeTimer <= 0) {
            if (player.hasResources(BlockType.IRON, 5) && player.hasResources(BlockType.COPPER, 5)) {
                player.consumeResources(BlockType.IRON, 5);
                player.consumeResources(BlockType.COPPER, 5);
                Machine miner = MachineFactory.createMachine("AutoMiner", player.x, player.y);
                if (miner != null) {
                    activeMachines.add(miner);
                }
                placeTimer = 0.5f;
            }
        }

        // Flight Transition Logic
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            GameManager.getInstance().changeScreen(GameManager.ScreenType.FLIGHT);
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

        // Update camera position to follow player's Y
        camera.position.y = player.y;
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
                    case COPPER:
                        shapeRenderer.setColor(0.8f, 0.4f, 0.0f, 1f); // Orange
                        break;
                    case IRON:
                        shapeRenderer.setColor(0.75f, 0.75f, 0.75f, 1f); // Silver
                        break;
                }
                shapeRenderer.rect(block.x, block.y, block.width, block.height);
            }
        }
        
        // Render Machines
        for (Machine machine : activeMachines) {
            shapeRenderer.setColor(0.5f, 0f, 0.5f, 1f); // Purple for AutoMiner
            shapeRenderer.rect(machine.x, machine.y, machine.width, machine.height);
        }
        
        // Render Player
        shapeRenderer.setColor(0f, 0.5f, 1f, 1f); // Bright Blue
        shapeRenderer.rect(player.x, player.y, player.width, player.height);
        
        shapeRenderer.end();

        hud.stage.act(delta);
        hud.stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        float viewportWidth = COLS * BLOCK_SIZE;
        float viewportHeight = viewportWidth * ((float) height / width);
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        camera.position.x = viewportWidth / 2f;
        if (player != null) {
            camera.position.y = player.y;
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
