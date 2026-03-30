package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import org.springframework.stereotype.Component;

@Component
public class TrackedLinkOrmConverter {

    public TrackedLinkEntity toEntity(TrackedLink link) {
        return new TrackedLinkEntity(
                link.id() > 0 ? link.id() : null,
                link.url(),
                link.type().name(),
                link.createdAt(),
                link.lastCheckedAt(),
                link.lastUpdatedAt());
    }

    public TrackedLink toModel(TrackedLinkEntity entity) {
        return new TrackedLink(
                entity.getId(),
                entity.getUrl(),
                LinkSourceType.valueOf(entity.getType()),
                entity.getCreatedAt(),
                entity.getLastCheckedAt(),
                entity.getLastUpdatedAt());
    }
}
