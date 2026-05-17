package backend.academy.linktracker.scrapper.api;

import static backend.academy.linktracker.scrapper.configuration.ValkeyCacheConfiguration.LINK_LIST_CACHE;
import static backend.academy.linktracker.scrapper.configuration.ValkeyCacheConfiguration.LINK_LIST_KEY_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresValkeyIT;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "app.persistence.access-type=ORM",
            "app.valkey.cache.ttl=500ms",
        })
class LinkListValkeyCacheIT extends AbstractPostgresValkeyIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void cleanValkey() {
        try (RedisClusterConnection clusterConnection = redisConnectionFactory.getClusterConnection()) {
            if (clusterConnection != null) {
                for (RedisClusterNode node : clusterConnection.clusterCommands().clusterGetNodes()) {
                    clusterConnection.serverCommands().flushAll(node);
                }
                return;
            }
        }

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }

    @Test
    void shouldCacheLinksResponseInValkeyAsJsonByPrefixedChatIdKey() throws Exception {
        registerChatAndAddLink(1, "https://github.com/user/repo");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));

        String cachedValue = redisTemplate.opsForValue().get(cacheKey(1));
        assertThat(cachedValue).isNotBlank();

        JsonNode cachedJson = objectMapper.readTree(cachedValue);
        assertThat(cachedJson.get("size").asInt()).isEqualTo(1);
        assertThat(cachedJson.get("links").get(0).get("url").asText()).isEqualTo("https://github.com/user/repo");
    }

    @Test
    void shouldExpireLinksResponseAfterConfiguredTtlAndRepopulateOnNextGet() throws Exception {
        registerChatAndAddLink(1, "https://github.com/user/repo");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1)).andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNotNull();

        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() ->
                        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNull());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));

        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNotNull();
    }

    @Test
    void shouldReturnRepeatedResponseFromCacheInsteadOfDatabase() throws Exception {
        registerChatAndAddLink(1, "https://github.com/user/repo");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));

        long linkId = linkRepository
                .findByUrl("https://github.com/user/repo")
                .orElseThrow()
                .id();
        subscriptionRepository.delete(1, linkId);

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/user/repo"));
    }

    @Test
    void shouldInvalidateLinksCacheAfterLinkAndTagMutations() throws Exception {
        registerChatAndAddLink(1, "https://github.com/user/repo");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1)).andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNotNull();

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/second-repo\"}"))
                .andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNull();

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2));
        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNotNull();

        mockMvc.perform(post("/tags")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tag\":\"java\"}"))
                .andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNull();

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1)).andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNotNull();

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());
        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNull();
    }

    @Test
    void shouldNotEvictOtherChatCacheEntriesWhenOneChatChanges() throws Exception {
        registerChatAndAddLink(1, "https://github.com/user/repo");
        registerChatAndAddLink(2, "https://github.com/user/other-repo");

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1)).andExpect(status().isOk());
        mockMvc.perform(get("/links").header("Tg-Chat-Id", 2)).andExpect(status().isOk());

        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNotNull();
        assertThat(redisTemplate.opsForValue().get(cacheKey(2))).isNotNull();

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/second-repo\"}"))
                .andExpect(status().isOk());

        assertThat(redisTemplate.opsForValue().get(cacheKey(1))).isNull();
        assertThat(redisTemplate.opsForValue().get(cacheKey(2))).isNotNull();

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2));
        mockMvc.perform(get("/links").header("Tg-Chat-Id", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1));
    }

    private void registerChatAndAddLink(long chatId, String link) throws Exception {
        mockMvc.perform(post("/tg-chat/{id}", chatId)).andExpect(status().isOk());
        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"" + link + "\"}"))
                .andExpect(status().isOk());
    }

    private String cacheKey(long chatId) {
        return LINK_LIST_CACHE + "::" + LINK_LIST_KEY_PREFIX + chatId;
    }
}
