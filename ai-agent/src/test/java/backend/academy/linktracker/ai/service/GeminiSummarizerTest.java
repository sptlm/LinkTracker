package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class GeminiSummarizerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> apiKeyHeader = new AtomicReference<>();

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/models/gemini-test:generateContent", this::handleSummarization);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldCallGeminiApiAndReturnSummary() throws Exception {
        AiAgentProperties properties = new AiAgentProperties();
        properties
                .getSummarization()
                .getApi()
                .setBaseUrl("http://localhost:" + server.getAddress().getPort());
        properties.getSummarization().getApi().setModel("gemini-test");
        properties.getSummarization().getApi().setToken("test-key");
        properties.getSummarization().getApi().setTimeout(Duration.ofSeconds(2));
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getSummarization().getApi().getTimeout());
        requestFactory.setReadTimeout(properties.getSummarization().getApi().getTimeout());
        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(
                        "x-goog-api-key", properties.getSummarization().getApi().getToken())
                .build();
        GeminiSummarizer summarizer = new GeminiSummarizer(properties, objectMapper, restClient);

        String summary = summarizer.summarize("A very long update that should be summarized", 20);

        assertEquals("Short Gemini summary", summary);
        assertEquals("test-key", apiKeyHeader.get());
        JsonNode root = objectMapper.readTree(requestBody.get());
        String prompt =
                root.path("contents").get(0).path("parts").get(0).path("text").asText();
        assertTrue(prompt.contains("Summarize the following update in 2-3 sentences"));
        assertTrue(prompt.contains("A very long update that should be summarized"));
    }

    private void handleSummarization(HttpExchange exchange) throws IOException {
        apiKeyHeader.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        byte[] response = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "Short Gemini summary"
                          }
                        ]
                      }
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
