package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

class FallbackSummarizerTest {

    @Test
    void shouldFallbackToStubWhenConfiguredGeminiCallFails() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getSummarization().setProvider(AiAgentProperties.Provider.API);
        GeminiSummarizer geminiSummarizer = Mockito.mock(GeminiSummarizer.class);
        when(geminiSummarizer.summarize("123456789012345", 10)).thenThrow(new IllegalStateException("api failed"));
        ObjectProvider<GeminiSummarizer> geminiSummarizerProvider = objectProvider();
        when(geminiSummarizerProvider.getObject()).thenReturn(geminiSummarizer);
        FallbackSummarizer fallbackSummarizer =
                new FallbackSummarizer(properties, geminiSummarizerProvider, new TruncatingSummarizer());

        String summary = fallbackSummarizer.summarize("123456789012345", 10);

        assertEquals("1234567890...", summary);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<GeminiSummarizer> objectProvider() {
        return Mockito.mock(ObjectProvider.class);
    }
}
