package backend.academy.linktracker.scrapper.updater;

import backend.academy.linktracker.scrapper.model.TrackedLink;

public interface LinkUpdater {

    boolean supports(TrackedLink link);

    LinkUpdateCheckResult check(TrackedLink link);
}
