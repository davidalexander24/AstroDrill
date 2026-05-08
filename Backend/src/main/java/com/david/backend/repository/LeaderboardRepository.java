package com.david.backend.repository;

import com.david.backend.entity.Leaderboard;
import com.david.backend.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {
    Optional<Leaderboard> findByPlayer(Player player);
    List<Leaderboard> findTop10ByOrderByMaxDepthMinedDesc();
}

