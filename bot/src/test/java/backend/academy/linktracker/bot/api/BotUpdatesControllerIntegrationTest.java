package backend.academy.linktracker.bot.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.bot.service.BotUpdateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BotUpdatesController.class)
@Import(BotApiExceptionHandler.class)
class BotUpdatesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BotUpdateService botUpdateService;

    /**
     * Требование: Тест 1: Корректный запрос к сервису Бота.
     * Отправьте POST-запрос /updates в сервис Бота с телом, соответствующим ожидаемой схеме.
     * Убедитесь, что ответ — 200 OK.
     */
    @Test
    void shouldReturnOkForValidUpdateRequest() throws Exception {
        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      \"id\": 1,
                      \"url\": \"https://github.com/user/repo\",
                      \"description\": \"Repository updated\",
                      \"tgChatIds\": [1, 2]
                    }
                    """))
            .andExpect(status().isOk());
    }

    /**
     * Требование: Тест 2: Некорректный запрос к сервису Бота.
     * Отправьте POST-запрос /updates в сервис Бота с телом, не соответствующим ожидаемой схеме.
     * Убедитесь, что ответ — не 200 OK (например, 400 Bad Request).
     */
    @Test
    void shouldReturnBadRequestForMalformedUpdateRequest() throws Exception {
        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": \"bad\", \"url\": }"))
            .andExpect(status().isBadRequest());
    }
}
