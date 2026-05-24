package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeminiSummarizer implements Summarizer {

    private static final String PROMPT = "Summarize the following update in 2-3 sentences:\n\n";

    private final AiAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiSummarizer(
            AiAgentProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("geminiRestClient") RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Override
    public String summarize(String text, int threshold) {
        AiAgentProperties.Api api = properties.getSummarization().getApi();
        if (isBlank(api.getBaseUrl()) || isBlank(api.getToken())) {
            throw new IllegalStateException("Gemini summarization API is not configured");
        }

        Map<String, Object> request = geminiRequest(text);

        String response = restClient
                .post()
                .uri(geminiEndpoint(api))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        String summary = extractSummary(response);
        if (isBlank(summary)) {
            throw new IllegalStateException("Gemini summarization API returned an empty summary");
        }
        return summary;
    }

    private Map<String, Object> geminiRequest(String text) {
        return Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", PROMPT + text)))));
    }

    private String geminiEndpoint(AiAgentProperties.Api api) {
        String baseUrl = trimTrailingSlash(api.getBaseUrl());
        if (baseUrl.endsWith(":generateContent")) {
            return baseUrl;
        }
        if (isBlank(api.getModel())) {
            throw new IllegalStateException("Gemini model is not configured");
        }
        return baseUrl + "/models/" + api.getModel() + ":generateContent";
    }

    private String extractSummary(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty() && parts.get(0).hasNonNull("text")) {
                    return parts.get(0).get("text").asText();
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Gemini summarization response", e);
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimTrailingSlash(String value) {
        String result = value.strip();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
