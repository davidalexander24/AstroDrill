package com.david.astrodrill.machine;

public class MachineFactory {
    public static Machine createMachine(String type, float x, float y) {
        switch (type) {
            case "AutoMiner":      return new AutoMiner(x, y, 1f, 1f);
            case "CoalGenerator":  return new CoalGenerator(x, y, 1f, 1f);
            case "IronSmelter":    return new IronSmelter(x, y, 1f, 1f);
            case "CopperSmelter":  return new CopperSmelter(x, y, 1f, 1f);
            case "GearAssembler":  return new GearAssembler(x, y, 1f, 1f);
            case "WireAssembler":  return new WireAssembler(x, y, 1f, 1f);
            default:               return null;
        }
    }
}
