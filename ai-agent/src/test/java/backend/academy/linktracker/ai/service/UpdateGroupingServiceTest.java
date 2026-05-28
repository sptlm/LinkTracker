package backend.academy.linktracker.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class UpdateGroupingServiceTest {

    @Test
    void shouldGroupMultipleUpdatesForSameChat() {
        ProcessedUpdatePublisher publisher = Mockito.mock(ProcessedUpdatePublisher.class);
        UpdateGroupingService groupingService = new UpdateGroupingService(new AiAgentProperties(), publisher);

        groupingService.accept(update(1L, "first update", "LOW", 100L));
        groupingService.accept(update(2L, "second critical update", "HIGH", 100L));
        groupingService.flush(100L);

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(publisher).publish(captor.capture());
        LinkUpdate grouped = captor.getValue();

        assertEquals(1L, grouped.getId());
        assertEquals(
                "1. first update" + System.lineSeparator() + "2. second critical update", grouped.getDescription());
        assertEquals(List.of(100L), grouped.getTgChatIds());
        assertEquals("HIGH", grouped.getPriority());
    }

    @Test
    void shouldPublishSingleUpdateWithoutGroupingChanges() {
        ProcessedUpdatePublisher publisher = Mockito.mock(ProcessedUpdatePublisher.class);
        UpdateGroupingService groupingService = new UpdateGroupingService(new AiAgentProperties(), publisher);
        LinkUpdate update = update(1L, "single update", "MEDIUM", 100L);

        groupingService.accept(update);
        groupingService.flush(100L);

        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(publisher).publish(captor.capture());
        LinkUpdate processed = captor.getValue();

        assertEquals("single update", processed.getDescription());
        assertEquals(List.of(100L), processed.getTgChatIds());
        assertEquals("MEDIUM", processed.getPriority());
    }

    @Test
    void shouldCreateIndependentMessagesForEachChat() {
        ProcessedUpdatePublisher publisher = Mockito.mock(ProcessedUpdatePublisher.class);
        UpdateGroupingService groupingService = new UpdateGroupingService(new AiAgentProperties(), publisher);

        groupingService.accept(update(1L, "shared update", "MEDIUM", 100L, 200L));
        groupingService.flush(100L);
        groupingService.flush(200L);

        verify(publisher, times(2)).publish(Mockito.any(LinkUpdate.class));
    }

    private LinkUpdate update(Long id, String description, String priority, Long... chatIds) {
        return new LinkUpdate()
                .id(id)
                .url(URI.create("https://example.com/" + id))
                .description(description)
                .author("alice")
                .tgChatIds(List.of(chatIds))
                .priority(priority);
    }
}
