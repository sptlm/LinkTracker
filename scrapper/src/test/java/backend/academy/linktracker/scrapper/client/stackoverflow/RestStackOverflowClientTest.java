package backend.academy.linktracker.scrapper.client.stackoverflow;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

class RestStackOverflowClientTest {

    /**
     * Требование: HTTP-клиент (GitHub, StackOverflow) отправляют некорректный ответ сервису Scrapper
     * (HTTP-код не 2xx или тело ответа не соответствует заявленной схеме).
     * Scrapper корректно обрабатывает ошибочные HTTP-статусы и некорректные тела ответов от внешних API,
     * не приводя к падению приложения.
     */
    @Test
    void shouldWrapStackOverflowHttpErrorIntoExternalServiceException() {
        WireMockServer server = new WireMockServer(0);
        try {
            server.start();
            RestStackOverflowClient client = new RestStackOverflowClient(
                    RestClient.builder().baseUrl(server.baseUrl()).build());

            server.stubFor(get(urlEqualTo("/questions/123?site=stackoverflow"))
                    .willReturn(aResponse().withStatus(500)));

            assertThrows(ExternalServiceException.class, () -> client.getQuestion(123L));
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
    void shouldWrapStackOverflowInvalidBodyIntoExternalServiceException() {
        WireMockServer server = new WireMockServer(0);
        try {
            server.start();
            RestStackOverflowClient client = new RestStackOverflowClient(
                    RestClient.builder().baseUrl(server.baseUrl()).build());

            server.stubFor(get(urlEqualTo("/questions/123?site=stackoverflow"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withBody("{\"items\":[{\"question_id\":123,\"last_activity_date\":\"bad\"}]}")));

            assertThrows(ExternalServiceException.class, () -> client.getQuestion(123L));
        } finally {
            server.stop();
        }
    }
}
