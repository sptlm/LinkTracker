package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAgentUpdateProcessor {

    private final AiAgentProperties properties;
    private final UpdateFilter updateFilter;
    private final Summarizer summarizer;
    private final UpdatePrioritizer prioritizer;

    public Optional<LinkUpdate> process(LinkUpdate update) {
        if (!updateFilter.shouldProcess(update)) {
            return Optional.empty();
        }

        int threshold = properties.getSummarization().getThreshold();
        String description = update.getDescription();
        String processedDescription =
                description.length() > threshold ? summarizer.summarize(description, threshold) : description;
        UpdatePriority priority = prioritizer.prioritize(description);

        return Optional.of(new LinkUpdate()
                .id(update.getId())
                .url(update.getUrl())
                .description(processedDescription)
                .author(update.getAuthor())
                .tgChatIds(update.getTgChatIds())
                .priority(priority.name()));
    }
}
