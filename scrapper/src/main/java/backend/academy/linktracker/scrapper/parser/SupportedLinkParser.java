package backend.academy.linktracker.scrapper.parser;

import backend.academy.linktracker.scrapper.api.exception.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.parser.site.SiteLinkParser;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportedLinkParser {

    private final List<SiteLinkParser> siteLinkParsers;

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

        if (uri.getHost() == null || uri.getPath() == null || uri.getPath().isBlank()) {
            throw new UnsupportedLinkException(rawLink);
        }

        return siteLinkParsers.stream()
            .filter(parser -> parser.supports(uri))
            .findFirst()
            .map(parser -> parser.parse(uri))
            .orElseThrow(() -> new UnsupportedLinkException(rawLink));
    }
}
