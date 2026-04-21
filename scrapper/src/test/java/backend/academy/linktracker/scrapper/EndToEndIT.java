package backend.academy.linktracker.scrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.awaitility.Awaitility.await;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = TestScrapperApplication.class)
@Import(TestcontainersConfiguration.class)
public class EndToEndIT {

    @Autowired
    @Qualifier("scrapperContainer")
    private GenericContainer<?> scrapperContainer;

    @Autowired
    @Qualifier("botContainer")
    private GenericContainer<?> botContainer;

    @Test
    void testContainersAreRunningAndCommunicating() {
        String scrapperUrl = "http://" + scrapperContainer.getHost() + ":" + scrapperContainer.getMappedPort(8081);
        RestClient scrapperClient = createDirectRestClient(scrapperUrl);

        await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
            var scrapperResponse =
                    scrapperClient.post().uri("/tg-chat/42").retrieve().toBodilessEntity();
            assertEquals(200, scrapperResponse.getStatusCode().value(), "Scrapper should return 200 OK");
        });

        String botUrl = "http://" + botContainer.getHost() + ":" + botContainer.getMappedPort(8080);
        RestClient botClient = createDirectRestClient(botUrl);

        String updateJson = """
                {
                  "id": 1,
                  "url": "https://github.com/dude/repa",
                  "description": "E2E integration test update",
                  "tgChatIds": [42]
                }
                """;

        await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
            var botResponse = botClient
                    .post()
                    .uri("/updates")
                    .header("Content-Type", "application/json")
                    .body(updateJson)
                    .retrieve()
                    .toBodilessEntity();

            assertEquals(200, botResponse.getStatusCode().value(), "Bot should accept update and return 200 OK");
        });
    }

    private RestClient createDirectRestClient(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .proxy(ProxySelector.of(null))
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
    }
}
