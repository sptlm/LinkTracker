package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class FallbackSummarizer implements Summarizer {

    private final AiAgentProperties properties;
    private final ObjectProvider<GeminiSummarizer> geminiSummarizer;
    private final TruncatingSummarizer truncatingSummarizer;

    @Override
    public String summarize(String text, int threshold) {
        if (properties.getSummarization().getProvider() == AiAgentProperties.Provider.STUB) {
            return truncatingSummarizer.summarize(text, threshold);
        }

        try {
            return geminiSummarizer.getObject().summarize(text, threshold);
        } catch (Exception e) {
            log.atWarn().setCause(e).log("Gemini summarization failed, falling back to truncating summarizer");
            return truncatingSummarizer.summarize(text, threshold);
        }
    }
}
