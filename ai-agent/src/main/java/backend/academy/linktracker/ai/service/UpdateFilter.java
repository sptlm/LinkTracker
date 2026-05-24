package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
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
        return description.trim().length() >= filtering.getMinLength();
    }

    private boolean hasNoStopWords(String description, AiAgentProperties.Filtering filtering) {
        Set<String> words = Arrays.stream(description.toLowerCase(Locale.ROOT).split("\\P{L}+"))
                .filter(word -> !word.isBlank())
                .collect(Collectors.toSet());
        return filtering.getStopWords().stream()
                .filter(word -> word != null && !word.isBlank())
                .map(word -> word.trim().toLowerCase(Locale.ROOT))
                .noneMatch(words::contains);
    }

    private boolean hasAllowedAuthor(String author, AiAgentProperties.Filtering filtering) {
        if (author == null || author.isBlank()) {
            return true;
        }

        String normalizedAuthor = author.trim();
        return filtering.getExcludedAuthors().stream()
                .filter(excluded -> excluded != null && !excluded.isBlank())
                .noneMatch(excluded -> excluded.trim().equalsIgnoreCase(normalizedAuthor));
    }
}
