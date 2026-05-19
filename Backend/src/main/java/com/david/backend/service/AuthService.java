package com.david.backend.service;

import com.david.backend.dto.LoginRequest;
import com.david.backend.dto.LoginResponse;
import com.david.backend.dto.RegisterRequest;
import com.david.backend.entity.Player;
import com.david.backend.exception.InvalidCredentialsException;
import com.david.backend.exception.UsernameTakenException;
import com.david.backend.repository.PlayerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(PlayerRepository playerRepository, PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse register(RegisterRequest req) {
        if (playerRepository.findByUsername(req.username()).isPresent()) {
            throw new UsernameTakenException(req.username());
        }
        Player player = new Player(req.username(), passwordEncoder.encode(req.password()));
        Player saved = playerRepository.save(player);
        return new LoginResponse(saved.getId(), saved.getUsername());
    }

    public LoginResponse login(LoginRequest req) {
        Player player = playerRepository.findByUsername(req.username())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(req.password(), player.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new LoginResponse(player.getId(), player.getUsername());
    }
}
