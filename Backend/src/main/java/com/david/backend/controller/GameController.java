package com.david.backend.controller;

import com.david.backend.entity.Leaderboard;
import com.david.backend.entity.Player;
import com.david.backend.service.GameService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/login")
    public Player login(@RequestParam String username) {
        return gameService.loginOrRegister(username);
    }

    @PostMapping("/save")
    public Map<String, String> saveProgress(@RequestBody Map<String, Object> req) {
        String username = (String) req.get("username");
        int credits = (int) req.get("credits");
        String currentPlanet = (String) req.get("currentPlanet");
        int maxDepthMined = (int) req.get("maxDepthMined");

        long fastestLaunchTime = 0;
        if (req.containsKey("fastestLaunchTime")) {
            Object flt = req.get("fastestLaunchTime");
            if (flt instanceof Number) {
                fastestLaunchTime = ((Number) flt).longValue();
            } else {
                fastestLaunchTime = Long.parseLong(flt.toString());
            }
        }

        gameService.saveProgress(username, credits, currentPlanet, maxDepthMined, fastestLaunchTime);
        return Map.of("status", "SUCCESS");
    }

    @GetMapping("/leaderboard")
    public List<Leaderboard> getLeaderboard() {
        return gameService.getTopLeaderboard();
    }
}

