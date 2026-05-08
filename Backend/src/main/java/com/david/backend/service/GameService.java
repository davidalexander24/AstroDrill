package com.david.backend.service;

import com.david.backend.entity.Leaderboard;
import com.david.backend.entity.Player;
import com.david.backend.entity.SaveState;
import com.david.backend.repository.LeaderboardRepository;
import com.david.backend.repository.PlayerRepository;
import com.david.backend.repository.SaveStateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameService {

    private final PlayerRepository playerRepository;
    private final SaveStateRepository saveStateRepository;
    private final LeaderboardRepository leaderboardRepository;

    public GameService(PlayerRepository playerRepository,
                       SaveStateRepository saveStateRepository,
                       LeaderboardRepository leaderboardRepository) {
        this.playerRepository = playerRepository;
        this.saveStateRepository = saveStateRepository;
        this.leaderboardRepository = leaderboardRepository;
    }

    public Player loginOrRegister(String username) {
        return playerRepository.findByUsername(username)
                .orElseGet(() -> playerRepository.save(new Player(username)));
    }

    public void saveProgress(String username, int credits, String currentPlanet, int maxDepthMined, long fastestLaunchTime) {
        Player player = loginOrRegister(username);

        Optional<SaveState> saveStateOpt = saveStateRepository.findByPlayer(player);
        SaveState saveState;
        if (saveStateOpt.isPresent()) {
            saveState = saveStateOpt.get();
            saveState.setCredits(credits);
            saveState.setCurrentPlanet(currentPlanet);
        } else {
            saveState = new SaveState(player, credits, currentPlanet);
        }
        saveStateRepository.save(saveState);

        Optional<Leaderboard> leaderboardOpt = leaderboardRepository.findByPlayer(player);
        Leaderboard leaderboard;
        if (leaderboardOpt.isPresent()) {
            leaderboard = leaderboardOpt.get();
            if (maxDepthMined > leaderboard.getMaxDepthMined()) {
                leaderboard.setMaxDepthMined(maxDepthMined);
            }
            if (fastestLaunchTime < leaderboard.getFastestLaunchTime() || leaderboard.getFastestLaunchTime() == 0) {
                leaderboard.setFastestLaunchTime(fastestLaunchTime);
            }
        } else {
            leaderboard = new Leaderboard(player, maxDepthMined, fastestLaunchTime);
        }
        leaderboardRepository.save(leaderboard);
    }

    public List<Leaderboard> getTopLeaderboard() {
        return leaderboardRepository.findTop10ByOrderByMaxDepthMinedDesc();
    }
}

