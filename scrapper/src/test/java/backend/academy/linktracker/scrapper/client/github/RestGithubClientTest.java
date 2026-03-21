package backend.academy.linktracker.scrapper.client.github;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
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
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestGithubClient client = new RestGithubClient(builder.build());

        server.expect(requestTo("https://example.com/repos/user/repo")).andRespond(withBadRequest());

        assertThrows(ExternalServiceException.class, () -> client.getRepository("user", "repo"));
        server.verify();
    }

    /**
     * Требование: HTTP-клиент (GitHub, StackOverflow) отправляют некорректный ответ сервису Scrapper
     * (HTTP-код не 2xx или тело ответа не соответствует заявленной схеме).
     * Scrapper корректно обрабатывает ошибочные HTTP-статусы и некорректные тела ответов от внешних API,
     * не приводя к падению приложения.
     */
    @Test
    void shouldWrapGithubInvalidBodyIntoExternalServiceException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestGithubClient client = new RestGithubClient(builder.build());

        server.expect(requestTo("https://example.com/repos/user/repo"))
                .andRespond(withSuccess("{\"pushed_at\":\"not-an-instant\"}", MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceException.class, () -> client.getRepository("user", "repo"));
        server.verify();
    }
}
