package backend.academy.linktracker.scrapper.client.stackoverflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import backend.academy.linktracker.scrapper.api.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
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
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestStackOverflowClient client = new RestStackOverflowClient(builder.build());

        server.expect(requestTo("https://example.com/questions/123?site=stackoverflow"))
                .andRespond(withServerError());

        assertThrows(ExternalServiceException.class, () -> client.getQuestion(123L));
        server.verify();
    }

    /**
     * Требование: HTTP-клиент (GitHub, StackOverflow) отправляют некорректный ответ сервису Scrapper
     * (HTTP-код не 2xx или тело ответа не соответствует заявленной схеме).
     * Scrapper корректно обрабатывает ошибочные HTTP-статусы и некорректные тела ответов от внешних API,
     * не приводя к падению приложения.
     */
    @Test
    void shouldWrapStackOverflowInvalidBodyIntoExternalServiceException() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://example.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestStackOverflowClient client = new RestStackOverflowClient(builder.build());

        server.expect(requestTo("https://example.com/questions/123?site=stackoverflow"))
                .andRespond(withSuccess(
                        "{\"items\":[{\"question_id\":123,\"last_activity_date\":\"bad\"}]}",
                        MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceException.class, () -> client.getQuestion(123L));
        server.verify();
    }
}
