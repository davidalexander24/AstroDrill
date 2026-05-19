package com.david.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;

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

    @Column(columnDefinition = "TEXT")
    private String data;

    @Column
    private Instant updatedAt;

    public SaveState() {}

    public SaveState(Player player, int credits, String currentPlanet, String data) {
        this.player = player;
        this.credits = credits;
        this.currentPlanet = currentPlanet;
        this.data = data;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public String getCurrentPlanet() { return currentPlanet; }
    public void setCurrentPlanet(String currentPlanet) { this.currentPlanet = currentPlanet; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
