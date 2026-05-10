package com.david.astrodrill.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
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
    public float speed;
    public Rectangle bounds;
    public Map<Block.BlockType, Integer> inventory;
    private List<InventoryObserver> observers;
    
    public float maxBattery = 100f;
    public float currentBattery = 100f;
    
    // Physics constants
    public static final float GRAVITY = -15f;
    public static final float MAX_FALL_SPEED = -10f;

    public Player(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velocityX = 0;
        this.velocityY = 0;
        this.speed = 5f;
        this.bounds = new Rectangle(x, y, width, height);
        this.inventory = new HashMap<>();
        this.observers = new ArrayList<>();
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
        return inventory.getOrDefault(type, 0) >= amount;
    }

    public void consumeResources(Block.BlockType type, int amount) {
        if (hasResources(type, amount)) {
            int current = inventory.get(type);
            inventory.put(type, current - amount);
            notifyObservers();
        }
    }

    public void update(float delta) {
        // Apply gravity
        velocityY += GRAVITY * delta;
        if (velocityY < MAX_FALL_SPEED) {
            velocityY = MAX_FALL_SPEED;
        }

        // Horizontal input
        velocityX = 0;
        boolean moving = false;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            velocityX = -speed;
            moving = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            velocityX = speed;
            moving = true;
        }

        boolean mining = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        if (moving || mining) {
            currentBattery -= 5f * delta; // Drain battery
        }

        if (currentBattery <= 0) {
            respawn();
        }
    }

    public void respawn() {
        this.x = 10;
        this.y = 0;
        this.currentBattery = this.maxBattery;
        this.inventory.clear();
        notifyObservers();
    }
}
