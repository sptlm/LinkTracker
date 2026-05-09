package backend.academy.linktracker.scrapper.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresValkeyIT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "app.persistence.access-type=ORM",
            "app.valkey.cache.ttl=500ms",
            "app.valkey.cache.client-side.enabled=false",
        })
class LinkListValkeyCacheIT extends AbstractPostgresValkeyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanValkey() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void shouldCacheLinksResponseInValkeyAsJsonByChatIdKey() throws Exception {
        registerChatAndAddLink(1, "https://github.com/user/repo");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));

        String cachedValue = redisTemplate.opsForValue().get("1");
        assertThat(cachedValue).isNotBlank();

        JsonNode cachedJson = objectMapper.readTree(cachedValue);
        assertThat(cachedJson.get("size").asInt()).isEqualTo(1);
        assertThat(cachedJson.get("links").get(0).get("url").asText()).isEqualTo("https://github.com/user/repo");
    }

    @Test
    void shouldExpireLinksResponseAfterConfiguredTtlAndRepopulateOnNextGet() throws Exception {
        registerChatAndAddLink(1, "https://github.com/user/repo");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1)).andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get("1")).isNotNull();

        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(
                        () -> assertThat(redisTemplate.opsForValue().get("1")).isNull());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));

        assertThat(redisTemplate.opsForValue().get("1")).isNotNull();
    }

    @Test
    void shouldInvalidateLinksCacheAfterLinkAndTagMutations() throws Exception {
        registerChatAndAddLink(1, "https://github.com/user/repo");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1)).andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get("1")).isNotNull();

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/second-repo\"}"))
                .andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get("1")).isNull();

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2));
        assertThat(redisTemplate.opsForValue().get("1")).isNotNull();

        mockMvc.perform(post("/tags")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tag\":\"java\"}"))
                .andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get("1")).isNull();

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1)).andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get("1")).isNotNull();

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get("1")).isNull();
    }

    private void registerChatAndAddLink(long chatId, String link) throws Exception {
        mockMvc.perform(post("/tg-chat/{id}", chatId)).andExpect(status().isOk());
        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"" + link + "\"}"))
                .andExpect(status().isOk());
    }
}
