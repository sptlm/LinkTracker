package backend.academy.linktracker.scrapper.parser.site;

import backend.academy.linktracker.scrapper.api.exception.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.parser.ParsedLink;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

@Component
public class GithubSiteLinkParser implements SiteLinkParser {

    @Override
    public boolean supports(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return "github.com".equals(normalizedHost) || "www.github.com".equals(normalizedHost);
    }

    @Override
    public ParsedLink parse(URI uri) {
        String[] parts = trimAndSplit(uri.getPath());
        if (parts.length < 2) {
            throw new UnsupportedLinkException(uri.toString());
        }

        String owner = parts[0];
        String repo = parts[1];
        String normalizedUrl = "https://github.com/%s/%s".formatted(owner, repo);
        return new ParsedLink(normalizedUrl, LinkSourceType.GITHUB);
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
