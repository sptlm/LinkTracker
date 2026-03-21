package backend.academy.linktracker.scrapper.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.TestScrapperApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = TestScrapperApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ScrapperApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Требование: Тест 3.1 Добавление и получение ссылки.
     */
    @Test
    void shouldAddAndGetLinkForRegisteredChat() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\",\"tags\":[\"java\"],\"filters\":[\"branch=main\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://github.com/user/repo"));

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/user/repo"));
    }

    /**
     * Требование: Тест 3.2 Добавление и удаление ссылки.
     */
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

    /**
     * Требование: Тест 3.3 Попытка удаления ссылки из несуществующего чата.
     */
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

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.links[0].url").value("https://github.com/user/repo"));
    }

    /**
     * Требование: Тест 3.4 Добавление ссылки в несуществующий чат.
     */
    @Test
    void shouldRejectAddingLinkForUnknownChat() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * Требование: Тест 3.5 Работа с удалённым чатом.
     */
    @Test
    void shouldRejectOperationsForDeletedChat() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());
        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"link\":\"https://github.com/user/repo\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * Требование: Тест 3.6 Удаление несуществующего чата.
     */
    @Test
    void shouldReturnNotFoundForUnknownChatDeletion() throws Exception {
        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isNotFound());
    }
}
