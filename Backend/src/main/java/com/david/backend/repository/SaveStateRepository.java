package com.david.backend.repository;

import com.david.backend.entity.Player;
import com.david.backend.entity.SaveState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaveStateRepository extends JpaRepository<SaveState, Long> {
    Optional<SaveState> findByPlayer(Player player);
}

