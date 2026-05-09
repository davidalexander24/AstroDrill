package com.david.astrodrill.machine;

import com.david.astrodrill.screen.PlayScreen;

public abstract class Machine {
    public float x;
    public float y;
    public float width;
    public float height;

    public abstract void update(float delta, PlayScreen screen);
}
