package backend.academy.linktracker.scrapper.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.integration.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "app.rate-limit.enabled=true",
            "app.rate-limit.capacity=1",
            "app.rate-limit.refill-tokens=1",
            "app.rate-limit.refill-period=1h",
            "spring.task.scheduling.enabled=false"
        })
class RateLimitingIT extends AbstractPostgresIT {

    private static final String TG_CHAT_ID_HEADER = "Tg-Chat-Id";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnTooManyRequestsWhenChatLimitExceeded() throws Exception {
        mockMvc.perform(post("/tg-chat/1")
                        .header(TG_CHAT_ID_HEADER, "1")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        }))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tg-chat/2")
                        .header(TG_CHAT_ID_HEADER, "1")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.20");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldUseSeparateLimitsForDifferentChatsBehindSameBotInstance() throws Exception {
        mockMvc.perform(post("/tg-chat/1")
                        .header(TG_CHAT_ID_HEADER, "1")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        }))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tg-chat/2")
                        .header(TG_CHAT_ID_HEADER, "2")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.10");
                            return request;
                        }))
                .andExpect(status().isOk());
    }
}
