package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiAgentUpdateProcessorTest {

    @Test
    void shouldSummarizeLongTextWithStubProvider() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getFiltering().setMinLength(0);
        properties.getSummarization().setProvider(AiAgentProperties.Provider.STUB);
        properties.getSummarization().setThreshold(10);
        TruncatingSummarizer truncatingSummarizer = new TruncatingSummarizer();
        GeminiSummarizer geminiSummarizer = Mockito.mock(GeminiSummarizer.class);
        FallbackSummarizer fallbackSummarizer =
                new FallbackSummarizer(properties, geminiSummarizer, truncatingSummarizer);
        AiAgentUpdateProcessor processor =
                new AiAgentUpdateProcessor(properties, new UpdateFilter(properties), fallbackSummarizer);
        LinkUpdate update = update("123456789012345");

        LinkUpdate processed = processor.process(update).orElseThrow();

        assertEquals("1234567890...", processed.getDescription());
        assertEquals("HIGH", processed.getPriority());
        verifyNoInteractions(geminiSummarizer);
    }

    @Test
    void shouldPassShortTextWithoutSummarization() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getFiltering().setMinLength(0);
        properties.getSummarization().setThreshold(10);
        FallbackSummarizer summarizer = Mockito.mock(FallbackSummarizer.class);
        AiAgentUpdateProcessor processor =
                new AiAgentUpdateProcessor(properties, new UpdateFilter(properties), summarizer);
        LinkUpdate update = update("short");

        LinkUpdate processed = processor.process(update).orElseThrow();

        assertEquals("short", processed.getDescription());
        verifyNoInteractions(summarizer);
    }

    @Test
    void shouldUseSummarizerForLongText() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getFiltering().setMinLength(0);
        properties.getSummarization().setThreshold(10);
        FallbackSummarizer summarizer = Mockito.mock(FallbackSummarizer.class);
        when(summarizer.summarize("123456789012345", 10)).thenReturn("summary from api");
        AiAgentUpdateProcessor processor =
                new AiAgentUpdateProcessor(properties, new UpdateFilter(properties), summarizer);

        LinkUpdate processed = processor.process(update("123456789012345")).orElseThrow();

        assertEquals("summary from api", processed.getDescription());
    }

    private LinkUpdate update(String description) {
        return new LinkUpdate()
                .id(1L)
                .url(URI.create("https://example.com"))
                .description(description)
                .author("alice")
                .tgChatIds(List.of(1L));
    }
}
