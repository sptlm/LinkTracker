package backend.academy.linktracker.scrapper.api;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIntegrationTest;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.SubscriptionRepository;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
abstract class AbstractScrapperApiIT extends AbstractPostgresIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected LinkRepository linkRepository;

    @Autowired
    protected ChatRepository chatRepository;

    @Autowired
    protected SubscriptionRepository subscriptionRepository;

    @Autowired
    protected TagRepository tagRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ApplicationContext applicationContext;

    protected abstract Class<?> expectedLinkRepositoryType();

    protected abstract Class<?> expectedChatRepositoryType();

    protected abstract Class<?> expectedSubscriptionRepositoryType();

    protected abstract Class<?> expectedTagRepositoryType();

    protected abstract boolean expectEntityManagerFactory();

    @Test
    void shouldUseConfiguredRepositoryImplementation() {
        assertInstanceOf(expectedLinkRepositoryType(), linkRepository);
        assertInstanceOf(expectedChatRepositoryType(), chatRepository);
        assertInstanceOf(expectedSubscriptionRepositoryType(), subscriptionRepository);
        assertInstanceOf(expectedTagRepositoryType(), tagRepository);
    }

    @Test
    void shouldEnableJpaOnlyWhenNeeded() {
        boolean present = applicationContext.containsBean("entityManagerFactory");
        if (expectEntityManagerFactory()) {
            assertTrue(present);
            return;
        }
        assertTrue(!present);
    }

    @Test
    void shouldApplyFlywayMigrationsOnCleanDatabase() {
        Integer count = jdbcClient
                .sql("select count(*) from flyway_schema_history where success = true")
                .query(Integer.class)
                .single();

        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    void shouldAddAndGetLinkForRegisteredChat() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(
                        post("/links")
                                .header("Tg-Chat-Id", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"link\":\"https://github.com/user/repo\",\"tags\":[\"java\"],\"filters\":[\"branch=main\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://github.com/user/repo"))
                .andExpect(jsonPath("$.tags[0]").value("java"))
                .andExpect(jsonPath("$.filters").isArray())
                .andExpect(jsonPath("$.filters").isEmpty());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/user/repo"))
                .andExpect(jsonPath("$.links[0].tags[0]").value("java"))
                .andExpect(jsonPath("$.links[0].filters").isArray())
                .andExpect(jsonPath("$.links[0].filters").isEmpty());
    }

    @Test
    void shouldDeletePreviouslyAddedLink() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(0));
    }

    @Test
    void shouldRejectDuplicateLink() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldManageTagsSeparatelyFromLinks() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tags\":[\"java\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tags")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tag\":\"backend\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value("backend"))
                .andExpect(jsonPath("$.tags[1]").value("java"));

        mockMvc.perform(get("/tags").header("Tg-Chat-Id", 1).param("link", "https://github.com/user/repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value("backend"))
                .andExpect(jsonPath("$.tags[1]").value("java"));

        mockMvc.perform(put("/tags")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tags\":[\"orm\",\"sql\",\"orm\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value("orm"))
                .andExpect(jsonPath("$.tags[1]").value("sql"));

        mockMvc.perform(delete("/tags")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tag\":\"orm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value("sql"));
    }

    @Test
    void shouldRejectDuplicateTag() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tags\":[\"java\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tags")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tag\":\"java\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldNotDeleteLinkFromUnknownChat() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAddingLinkForUnknownChat() throws Exception {
        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isNotFound());
    }
}
