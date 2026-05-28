package backend.academy.linktracker.ai.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class GeminiSummarizerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private WireMockServer server;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void shouldCallGeminiApiAndReturnSummary() throws Exception {
        server.stubFor(post(urlEqualTo("/models/gemini-test:generateContent"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
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
                                """)));

        AiAgentProperties properties = new AiAgentProperties();
        properties.getSummarization().getApi().setBaseUrl(server.baseUrl());
        properties.getSummarization().getApi().setModel("gemini-test");
        properties.getSummarization().getApi().setToken("test-key");
        properties.getSummarization().getApi().setPrompt("Summarize the following update in 2-3 sentences:");
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
        server.verify(postRequestedFor(urlEqualTo("/models/gemini-test:generateContent"))
                .withHeader("x-goog-api-key", equalTo("test-key"))
                .withRequestBody(matchingJsonPath(
                        "$.contents[0].parts[0].text", containing("Summarize the following update in 2-3 sentences")))
                .withRequestBody(matchingJsonPath(
                        "$.contents[0].parts[0].text", containing("A very long update that should be summarized"))));
    }
}
