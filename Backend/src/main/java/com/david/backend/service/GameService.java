package com.david.backend.service;

import com.david.backend.dto.LeaderboardEntryDto;
import com.david.backend.dto.LoadResponse;
import com.david.backend.dto.SaveRequest;
import com.david.backend.dto.SaveResponse;
import com.david.backend.entity.Leaderboard;
import com.david.backend.entity.Player;
import com.david.backend.entity.SaveState;
import com.david.backend.exception.PlayerNotFoundException;
import com.david.backend.exception.SaveNotFoundException;
import com.david.backend.repository.LeaderboardRepository;
import com.david.backend.repository.PlayerRepository;
import com.david.backend.repository.SaveStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Transactional
    public SaveResponse saveProgress(SaveRequest req) {
        Player player = playerRepository.findById(req.playerId())
                .orElseThrow(() -> new PlayerNotFoundException(req.playerId()));

        Instant now = Instant.now();
        SaveState saveState = saveStateRepository.findByPlayer(player)
                .orElseGet(() -> new SaveState(player, req.credits(), req.currentPlanet(), req.data()));
        saveState.setCredits(req.credits());
        saveState.setCurrentPlanet(req.currentPlanet());
        saveState.setData(req.data());
        saveState.setUpdatedAt(now);
        saveStateRepository.save(saveState);

        Leaderboard leaderboard = leaderboardRepository.findByPlayer(player).orElse(null);
        if (leaderboard == null) {
            leaderboard = new Leaderboard(player, req.maxDepthMined(), req.fastestLaunchTime());
        } else {
            if (req.maxDepthMined() > leaderboard.getMaxDepthMined()) {
                leaderboard.setMaxDepthMined(req.maxDepthMined());
            }
            if (req.fastestLaunchTime() > 0
                    && (leaderboard.getFastestLaunchTime() == 0
                        || req.fastestLaunchTime() < leaderboard.getFastestLaunchTime())) {
                leaderboard.setFastestLaunchTime(req.fastestLaunchTime());
            }
        }
        leaderboardRepository.save(leaderboard);

        return new SaveResponse(now);
    }

    @Transactional(readOnly = true)
    public LoadResponse loadProgress(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));
        SaveState saveState = saveStateRepository.findByPlayer(player)
                .orElseThrow(() -> new SaveNotFoundException(playerId));
        return new LoadResponse(player.getId(), player.getUsername(),
                saveState.getData(), saveState.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getTopLeaderboard() {
        return leaderboardRepository.findTop10ByOrderByMaxDepthMinedDesc().stream()
                .map(lb -> new LeaderboardEntryDto(
                        lb.getPlayer().getUsername(),
                        lb.getMaxDepthMined(),
                        lb.getFastestLaunchTime()))
                .toList();
    }
}
