package com.david.backend.entity;

import jakarta.persistence.*;

@Entity
public class SaveState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    private int credits;
    private String currentPlanet;

    public SaveState() {}

    public SaveState(Player player, int credits, String currentPlanet) {
        this.player = player;
        this.credits = credits;
        this.currentPlanet = currentPlanet;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getCurrentPlanet() { return currentPlanet; }
    public void setCurrentPlanet(String currentPlanet) { this.currentPlanet = currentPlanet; }
}

