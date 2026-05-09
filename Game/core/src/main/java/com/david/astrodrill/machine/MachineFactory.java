package com.david.astrodrill.machine;

public class MachineFactory {
    public static Machine createMachine(String type, float x, float y) {
        if ("AutoMiner".equals(type)) {
            // Give it some default dimensions, e.g. 1x1 like a block
            return new AutoMiner(x, y, 1f, 1f);
        }
        return null;
    }
}
