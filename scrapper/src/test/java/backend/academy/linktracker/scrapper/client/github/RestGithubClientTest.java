package backend.academy.linktracker.scrapper.client.github;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class RestGithubClientTest {

    /**
     * Требование: HTTP-клиент (GitHub, StackOverflow) отправляют некорректный ответ сервису Scrapper
     * (HTTP-код не 2xx или тело ответа не соответствует заявленной схеме).
     * Scrapper корректно обрабатывает ошибочные HTTP-статусы и некорректные тела ответов от внешних API,
     * не приводя к падению приложения.
     */
    @Test
    void shouldWrapGithubHttpErrorIntoExternalServiceException() {
        WireMockServer server = new WireMockServer(0);
        try {
            server.start();
            RestGithubClient client = new RestGithubClient(
                    RestClient.builder().baseUrl(server.baseUrl()).build());

            server.stubFor(
                    get(urlEqualTo("/repos/user/repo")).willReturn(aResponse().withStatus(400)));

            assertThrows(ExternalServiceException.class, () -> client.getRepository("user", "repo"));
        } finally {
            server.stop();
        }
    }

    /**
     * Требование: HTTP-клиент (GitHub, StackOverflow) отправляют некорректный ответ сервису Scrapper
     * (HTTP-код не 2xx или тело ответа не соответствует заявленной схеме).
     * Scrapper корректно обрабатывает ошибочные HTTP-статусы и некорректные тела ответов от внешних API,
     * не приводя к падению приложения.
     */
    @Test
    void shouldWrapGithubInvalidBodyIntoExternalServiceException() {
        WireMockServer server = new WireMockServer(0);
        try {
            server.start();
            RestGithubClient client = new RestGithubClient(
                    RestClient.builder().baseUrl(server.baseUrl()).build());

            server.stubFor(get(urlEqualTo("/repos/user/repo"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withBody("{\"pushed_at\":\"not-an-instant\"}")));

            assertThrows(ExternalServiceException.class, () -> client.getRepository("user", "repo"));
        } finally {
            server.stop();
        }
    }
}
