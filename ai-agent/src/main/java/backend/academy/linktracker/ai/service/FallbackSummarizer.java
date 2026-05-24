package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackSummarizer implements Summarizer {

    private final AiAgentProperties properties;
    private final AiApiSummarizer aiApiSummarizer;
    private final TruncatingSummarizer truncatingSummarizer;

    @Override
    public String summarize(String text, int threshold) {
        if (properties.getSummarization().getProvider() == AiAgentProperties.Provider.STUB || !isApiConfigured()) {
            return truncatingSummarizer.summarize(text, threshold);
        }

        try {
            return aiApiSummarizer.summarize(text, threshold);
        } catch (Exception e) {
            log.atWarn().setCause(e).log("AI summarization failed, falling back to truncating summarizer");
            return truncatingSummarizer.summarize(text, threshold);
        }
    }

    private boolean isApiConfigured() {
        AiAgentProperties.Api api = properties.getSummarization().getApi();
        return api.getBaseUrl() != null
                && !api.getBaseUrl().isBlank()
                && api.getToken() != null
                && !api.getToken().isBlank();
    }
}
