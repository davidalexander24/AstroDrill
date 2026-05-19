package com.david.backend;

import com.david.backend.repository.LeaderboardRepository;
import com.david.backend.repository.PlayerRepository;
import com.david.backend.repository.SaveStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LeaderboardTest {

    @Autowired private MockMvc mvc;
    @Autowired private PlayerRepository playerRepository;
    @Autowired private SaveStateRepository saveStateRepository;
    @Autowired private LeaderboardRepository leaderboardRepository;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        leaderboardRepository.deleteAll();
        saveStateRepository.deleteAll();
        playerRepository.deleteAll();
    }

    private Long register(String username) throws Exception {
        MvcResult res = mvc.perform(post("/api/game/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("playerId").asLong();
    }

    private void save(Long id, int depth) throws Exception {
        mvc.perform(post("/api/game/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":" + id + ",\"data\":\"{}\",\"credits\":0,\"currentPlanet\":\"X\",\"maxDepthMined\":"
                        + depth + ",\"fastestLaunchTime\":0}"))
                .andExpect(status().isOk());
    }

    @Test
    void leaderboard_orderedByMaxDepthDesc() throws Exception {
        Long alice = register("alice");
        Long bob = register("bob");
        Long carol = register("carol");

        save(alice, 100);
        save(bob, 300);
        save(carol, 200);

        MvcResult res = mvc.perform(get("/api/game/leaderboard"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode entries = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).get("username").asText()).isEqualTo("bob");
        assertThat(entries.get(0).get("maxDepthMined").asInt()).isEqualTo(300);
        assertThat(entries.get(1).get("username").asText()).isEqualTo("carol");
        assertThat(entries.get(2).get("username").asText()).isEqualTo("alice");
    }

    @Test
    void leaderboard_dtoShape_noPlayerEntityLeak() throws Exception {
        Long alice = register("alice");
        save(alice, 50);

        MvcResult res = mvc.perform(get("/api/game/leaderboard"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode entries = objectMapper.readTree(res.getResponse().getContentAsString());
        JsonNode first = entries.get(0);
        assertThat(first.has("username")).isTrue();
        assertThat(first.has("maxDepthMined")).isTrue();
        assertThat(first.has("fastestLaunchTime")).isTrue();
        assertThat(first.has("player")).isFalse();
        assertThat(first.has("id")).isFalse();
        assertThat(first.has("passwordHash")).isFalse();
    }
}
