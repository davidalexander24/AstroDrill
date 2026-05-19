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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaveLoadTest {

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

    private Long registerAndGetId(String username) throws Exception {
        MvcResult res = mvc.perform(post("/api/game/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        return node.get("playerId").asLong();
    }

    @Test
    void saveThenLoad_returnsSameBlob() throws Exception {
        Long id = registerAndGetId("alice");
        String blob = "{\"schemaVersion\":1,\"foo\":\"bar\"}";

        mvc.perform(post("/api/game/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":" + id + ",\"data\":" + objectMapper.writeValueAsString(blob)
                        + ",\"credits\":100,\"currentPlanet\":\"PROXIMA_B\",\"maxDepthMined\":50,\"fastestLaunchTime\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedAt").exists());

        mvc.perform(get("/api/game/load/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(id))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.data").value(blob));
    }

    @Test
    void loadUnknownPlayer_returns404() throws Exception {
        mvc.perform(get("/api/game/load/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PLAYER_NOT_FOUND"));
    }

    @Test
    void loadPlayerWithoutSave_returns404() throws Exception {
        Long id = registerAndGetId("bob");
        mvc.perform(get("/api/game/load/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SAVE_NOT_FOUND"));
    }

    @Test
    void save_preservesHighScore() throws Exception {
        Long id = registerAndGetId("carol");

        mvc.perform(post("/api/game/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":" + id + ",\"data\":\"{}\",\"credits\":0,\"currentPlanet\":\"X\",\"maxDepthMined\":200,\"fastestLaunchTime\":5000}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/game/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":" + id + ",\"data\":\"{}\",\"credits\":0,\"currentPlanet\":\"X\",\"maxDepthMined\":50,\"fastestLaunchTime\":9999}"))
                .andExpect(status().isOk());

        MvcResult res = mvc.perform(get("/api/game/leaderboard"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode entries = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(entries.isArray()).isTrue();
        assertThat(entries.get(0).get("username").asText()).isEqualTo("carol");
        assertThat(entries.get(0).get("maxDepthMined").asInt()).isEqualTo(200);
        assertThat(entries.get(0).get("fastestLaunchTime").asLong()).isEqualTo(5000);
    }

    @Test
    void saveWithUnknownPlayer_returns404() throws Exception {
        mvc.perform(post("/api/game/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"playerId\":99999,\"data\":\"{}\",\"credits\":0,\"currentPlanet\":\"X\",\"maxDepthMined\":0,\"fastestLaunchTime\":0}"))
                .andExpect(status().isNotFound());
    }
}
