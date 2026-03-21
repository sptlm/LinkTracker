package backend.academy.linktracker.scrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
        RestClient scrapperClient = RestClient.create(scrapperUrl);

        var scrapperResponse =
                scrapperClient.post().uri("/tg-chat/42").retrieve().toBodilessEntity();
        assertEquals(200, scrapperResponse.getStatusCode().value(), "Scrapper should return 200 OK");

        String botUrl = "http://" + botContainer.getHost() + ":" + botContainer.getMappedPort(8080);
        RestClient botClient = RestClient.create(botUrl);

        String updateJson = """
                {
                  "id": 1,
                  "url": "https://github.com/dude/repa",
                  "description": "E2E integration test update",
                  "tgChatIds": [42]
                }
                """;

        var botResponse = botClient
                .post()
                .uri("/updates")
                .header("Content-Type", "application/json")
                .body(updateJson)
                .retrieve()
                .toBodilessEntity();

        assertEquals(200, botResponse.getStatusCode().value(), "Bot should accept update and return 200 OK");
    }
}
