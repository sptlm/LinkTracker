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

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnTooManyRequestsWhenIpLimitExceeded() throws Exception {
        mockMvc.perform(post("/tg-chat/1").with(request -> {
                    request.setRemoteAddr("203.0.113.10");
                    return request;
                }))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tg-chat/2").with(request -> {
                    request.setRemoteAddr("203.0.113.10");
                    return request;
                }))
                .andExpect(status().isTooManyRequests());
    }
}
