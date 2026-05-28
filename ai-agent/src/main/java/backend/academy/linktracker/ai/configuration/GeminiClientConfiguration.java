package backend.academy.linktracker.ai.configuration;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiClientConfiguration {

    private static final String GEMINI_API_KEY_HEADER = "x-goog-api-key";

    @Bean("geminiRestClient")
    @ConditionalOnProperty(prefix = "ai-agent.summarization", name = "provider", havingValue = "API")
    public RestClient geminiRestClient(AiAgentProperties properties) {
        AiAgentProperties.Api api = properties.getSummarization().getApi();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(api.getTimeout());
        requestFactory.setReadTimeout(api.getTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(GEMINI_API_KEY_HEADER, api.getToken())
                .build();
    }
}
