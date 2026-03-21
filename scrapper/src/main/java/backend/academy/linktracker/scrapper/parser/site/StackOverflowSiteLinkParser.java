package backend.academy.linktracker.scrapper.parser.site;

import backend.academy.linktracker.scrapper.api.exception.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.parser.ParsedLink;
import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class StackOverflowSiteLinkParser implements SiteLinkParser {

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return "stackoverflow.com".equals(normalizedHost) || "www.stackoverflow.com".equals(normalizedHost);
    }

    @Override
    public ParsedLink parse(URI uri) {
        String[] parts = trimAndSplit(uri.getPath());
        if (parts.length < 2 || !"questions".equals(parts[0])) {
            throw new UnsupportedLinkException(uri.toString());
        }

        String questionId = parts[1];
        if (!questionId.chars().allMatch(Character::isDigit)) {
            throw new UnsupportedLinkException(uri.toString());
        }

        String normalizedUrl = "https://stackoverflow.com/questions/%s".formatted(questionId);
        return new ParsedLink(normalizedUrl, LinkSourceType.STACKOVERFLOW);
    }

    private String[] trimAndSplit(String path) {
        String normalized = path == null ? "" : path.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? new String[0] : normalized.split("/");
    }
}
