package backend.academy.linktracker.bot.state;

import java.net.URI;
import java.util.List;

public record TrackDialogState(TrackDialogStep step, URI url, List<String> tags) {

    public static TrackDialogState waitingLink() {
        return new TrackDialogState(TrackDialogStep.WAITING_LINK, null, List.of());
    }

    public static TrackDialogState waitingTags(URI url) {
        return new TrackDialogState(TrackDialogStep.WAITING_TAGS, url, List.of());
    }

    public static TrackDialogState waitingFilters(URI url, List<String> tags) {
        return new TrackDialogState(TrackDialogStep.WAITING_FILTERS, url, List.copyOf(tags));
    }
}
