package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.service.linkvalidator.LinkValidator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportedLinkParser {

    private final List<LinkValidator> linkValidators;

    public Optional<URI> parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }

        try {
            URI uri = new URI(rawText.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return Optional.empty();
            }

            return linkValidators.stream()
                    .filter(validator -> validator.supports(uri))
                    .filter(validator -> validator.isValid(uri))
                    .findFirst()
                    .map(validator -> uri);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
