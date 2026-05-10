package com.david.astrodrill.machine;

public class MachineFactory {
    public static Machine createMachine(String type, float x, float y) {
        if ("AutoMiner".equals(type)) {
            // Give it some default dimensions, e.g. 1x1 like a block
            return new AutoMiner(x, y, 1f, 1f);
        } else if ("CoalGenerator".equals(type)) {
            return new CoalGenerator(x, y, 1f, 1f);
        } else if ("Smelter".equals(type)) {
            return new Smelter(x, y, 1f, 1f);
        } else if ("Assembler".equals(type)) {
            return new Assembler(x, y, 1f, 1f);
        }
        return null;
    }
}
