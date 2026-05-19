package com.david.backend;

import com.david.backend.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private PlayerRepository playerRepository;

    @BeforeEach
    void clean() {
        playerRepository.deleteAll();
    }

    @Test
    void registerThenLogin_succeeds() throws Exception {
        mvc.perform(post("/api/game/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.playerId").isNumber());

        mvc.perform(post("/api/game/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void duplicateUsername_returns409() throws Exception {
        mvc.perform(post("/api/game/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bob\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/game/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"bob\",\"password\":\"different\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USERNAME_TAKEN"));
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        mvc.perform(post("/api/game/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"carol\",\"password\":\"correct1\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/game/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"carol\",\"password\":\"wrong123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unknownUser_returns401() throws Exception {
        mvc.perform(post("/api/game/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ghost\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shortPassword_returns400() throws Exception {
        mvc.perform(post("/api/game/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"dave\",\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }
}
