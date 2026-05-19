package com.david.astrodrill.network.dto;

public class SaveRequest {
    public Long playerId;
    public String data;
    public int credits;
    public String currentPlanet;
    public int maxDepthMined;
    public long fastestLaunchTime;

    public SaveRequest() {}

    public SaveRequest(Long playerId, String data, int credits, String currentPlanet,
                       int maxDepthMined, long fastestLaunchTime) {
        this.playerId = playerId;
        this.data = data;
        this.credits = credits;
        this.currentPlanet = currentPlanet;
        this.maxDepthMined = maxDepthMined;
        this.fastestLaunchTime = fastestLaunchTime;
    }
}
