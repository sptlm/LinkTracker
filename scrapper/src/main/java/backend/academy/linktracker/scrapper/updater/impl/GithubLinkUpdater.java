package backend.academy.linktracker.scrapper.updater.impl;

import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.updater.LinkUpdateCheckResult;
import backend.academy.linktracker.scrapper.updater.LinkUpdater;
import org.springframework.stereotype.Component;

@Component
public class GithubLinkUpdater implements LinkUpdater {

    @Override
    public boolean supports(TrackedLink link) {
        return link.type() == LinkSourceType.GITHUB;
    }

    /// TODO
    @Override
    public LinkUpdateCheckResult check(TrackedLink link) {
        return LinkUpdateCheckResult.notChanged();
    }
}
