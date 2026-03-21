package backend.academy.linktracker.scrapper.parser.site;

import backend.academy.linktracker.scrapper.parser.ParsedLink;
import java.net.URI;

public interface SiteLinkParser {

    boolean supports(URI uri);

    ParsedLink parse(URI uri);
}
