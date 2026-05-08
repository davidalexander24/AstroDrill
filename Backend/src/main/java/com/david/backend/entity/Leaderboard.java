package com.david.backend.entity;

import jakarta.persistence.*;

@Entity
public class Leaderboard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "player_id")
    private Player player;

    private int maxDepthMined;
    private long fastestLaunchTime;

    public Leaderboard() {}

    public Leaderboard(Player player, int maxDepthMined, long fastestLaunchTime) {
        this.player = player;
        this.maxDepthMined = maxDepthMined;
        this.fastestLaunchTime = fastestLaunchTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public int getMaxDepthMined() { return maxDepthMined; }
    public void setMaxDepthMined(int maxDepthMined) { this.maxDepthMined = maxDepthMined; }

    public long getFastestLaunchTime() { return fastestLaunchTime; }
    public void setFastestLaunchTime(long fastestLaunchTime) { this.fastestLaunchTime = fastestLaunchTime; }
}

