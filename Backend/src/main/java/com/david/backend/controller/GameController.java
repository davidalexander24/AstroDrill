package com.david.backend.controller;

import com.david.backend.dto.LeaderboardEntryDto;
import com.david.backend.dto.LoadResponse;
import com.david.backend.dto.LoginRequest;
import com.david.backend.dto.LoginResponse;
import com.david.backend.dto.RegisterRequest;
import com.david.backend.dto.SaveRequest;
import com.david.backend.dto.SaveResponse;
import com.david.backend.service.AuthService;
import com.david.backend.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final AuthService authService;
    private final GameService gameService;

    public GameController(AuthService authService, GameService gameService) {
        this.authService = authService;
        this.gameService = gameService;
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/save")
    public SaveResponse save(@Valid @RequestBody SaveRequest req) {
        return gameService.saveProgress(req);
    }

    @GetMapping("/load/{playerId}")
    public LoadResponse load(@PathVariable Long playerId) {
        return gameService.loadProgress(playerId);
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntryDto> getLeaderboard() {
        return gameService.getTopLeaderboard();
    }
}
