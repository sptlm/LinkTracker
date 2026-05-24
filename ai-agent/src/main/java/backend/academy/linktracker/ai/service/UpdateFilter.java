package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateFilter {

    private final AiAgentProperties properties;

    public boolean shouldProcess(LinkUpdate update) {
        if (update == null || update.getDescription() == null) {
            return false;
        }

        AiAgentProperties.Filtering filtering = properties.getFiltering();
        String description = update.getDescription();
        return hasEnoughText(description, filtering)
                && hasNoStopWords(description, filtering)
                && hasAllowedAuthor(update.getAuthor(), filtering);
    }

    private boolean hasEnoughText(String description, AiAgentProperties.Filtering filtering) {
        return description.length() >= filtering.getMinLength();
    }

    private boolean hasNoStopWords(String description, AiAgentProperties.Filtering filtering) {
        String normalized = description.toLowerCase(Locale.ROOT);
        return filtering.getStopWords().stream()
                .filter(word -> word != null && !word.isBlank())
                .map(word -> word.toLowerCase(Locale.ROOT))
                .noneMatch(normalized::contains);
    }

    private boolean hasAllowedAuthor(String author, AiAgentProperties.Filtering filtering) {
        if (author == null || author.isBlank()) {
            return true;
        }

        return filtering.getExcludedAuthors().stream()
                .filter(excluded -> excluded != null && !excluded.isBlank())
                .noneMatch(excluded -> excluded.equalsIgnoreCase(author));
    }
}
