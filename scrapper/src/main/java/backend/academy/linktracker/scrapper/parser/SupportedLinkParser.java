package backend.academy.linktracker.scrapper.parser;

import backend.academy.linktracker.scrapper.api.exception.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SupportedLinkParser {

    public ParsedLink parse(String rawLink) {
        if (rawLink == null || rawLink.isBlank()) {
            throw new UnsupportedLinkException(rawLink);
        }

        URI uri;
        try {
            uri = new URI(rawLink.trim());
        } catch (URISyntaxException e) {
            throw new UnsupportedLinkException(rawLink);
        }

        String host = uri.getHost();
        String path = uri.getPath();

        if (host == null || path == null || path.isBlank()) {
            throw new UnsupportedLinkException(rawLink);
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);

        if ("github.com".equals(normalizedHost) || "www.github.com".equals(normalizedHost)) {
            return parseGithub(uri);
        }

        if ("stackoverflow.com".equals(normalizedHost) || "www.stackoverflow.com".equals(normalizedHost)) {
            return parseStackOverflow(uri);
        }

        throw new UnsupportedLinkException(rawLink);
    }

    private ParsedLink parseGithub(URI uri) {
        String[] parts = trimAndSplit(uri.getPath());
        if (parts.length < 2) {
            throw new UnsupportedLinkException(uri.toString());
        }

        String owner = parts[0];
        String repo = parts[1];
        String normalizedUrl = "https://github.com/%s/%s".formatted(owner, repo);
        return new ParsedLink(normalizedUrl, LinkSourceType.GITHUB);
    }

    private ParsedLink parseStackOverflow(URI uri) {
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
