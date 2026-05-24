package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AiApiSummarizer implements Summarizer {

    private static final String PROMPT = "Summarize the following update in 2-3 sentences.";

    private final AiAgentProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public String summarize(String text, int threshold) {
        AiAgentProperties.Api api = properties.getSummarization().getApi();
        if (isBlank(api.getBaseUrl()) || isBlank(api.getToken())) {
            throw new IllegalStateException("AI summarization API is not configured");
        }

        Map<String, Object> request = new LinkedHashMap<>();
        if (!isBlank(api.getModel())) {
            request.put("model", api.getModel());
        }
        request.put("prompt", PROMPT + System.lineSeparator() + text);
        request.put("text", text);
        request.put("max_length", threshold);

        String response = RestClient.builder()
                .baseUrl(api.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + api.getToken())
                .build()
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        String summary = extractSummary(response);
        if (isBlank(summary)) {
            throw new IllegalStateException("AI summarization API returned an empty summary");
        }
        return summary;
    }

    private String extractSummary(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.hasNonNull("summary")) {
                return root.get("summary").asText();
            }
            if (root.hasNonNull("generated_text")) {
                return root.get("generated_text").asText();
            }
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode firstChoice = choices.get(0);
                if (firstChoice.path("message").hasNonNull("content")) {
                    return firstChoice.path("message").path("content").asText();
                }
                if (firstChoice.hasNonNull("text")) {
                    return firstChoice.get("text").asText();
                }
            }
        } catch (Exception ignored) {
            return response;
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
