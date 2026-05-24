package backend.academy.linktracker.ai.configuration;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiAgentPropertiesValidator implements InitializingBean {

    private final AiAgentProperties properties;

    @Override
    public void afterPropertiesSet() {
        if (properties.getSummarization().getProvider() != AiAgentProperties.Provider.API) {
            return;
        }

        AiAgentProperties.Api api = properties.getSummarization().getApi();
        List<String> missingProperties = new ArrayList<>();
        addIfBlank(missingProperties, "ai-agent.summarization.api.base-url", api.getBaseUrl());
        addIfBlank(missingProperties, "ai-agent.summarization.api.token", api.getToken());
        addIfBlank(missingProperties, "ai-agent.summarization.api.model", api.getModel());

        if (!missingProperties.isEmpty()) {
            throw new IllegalStateException("Gemini summarization API configuration is incomplete: missing "
                    + String.join(", ", missingProperties));
        }
    }

    private void addIfBlank(List<String> missingProperties, String propertyName, String value) {
        if (value == null || value.isBlank()) {
            missingProperties.add(propertyName);
        }
    }
}
