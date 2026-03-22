package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.LinkSourceType;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.TrackedLinkJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "ORM")
public class OrmLinkRepository implements LinkRepository {

    private final TrackedLinkJpaRepository trackedLinkJpaRepository;
    private final EntityManager entityManager;

    public OrmLinkRepository(TrackedLinkJpaRepository trackedLinkJpaRepository, EntityManager entityManager) {
        this.trackedLinkJpaRepository = trackedLinkJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    public long nextId() {
        Object value = entityManager.createNativeQuery("select nextval('tracked_link_id_seq')").getSingleResult();
        return ((Number) value).longValue();
    }

    @Override
    public TrackedLink save(TrackedLink link) {
        trackedLinkJpaRepository.save(toEntity(link));
        return link;
    }

    @Override
    public Optional<TrackedLink> findById(long id) {
        return trackedLinkJpaRepository.findById(id).map(this::toModel);
    }

    @Override
    public List<TrackedLink> findAllById(List<Long> ids) {
        return trackedLinkJpaRepository.findAllById(ids).stream()
                .map(this::toModel)
                .sorted(Comparator.comparingLong(TrackedLink::id))
                .toList();
    }

    @Override
    public Optional<TrackedLink> findByUrl(String url) {
        return trackedLinkJpaRepository.findByUrl(url).map(this::toModel);
    }

    @Override
    public List<TrackedLink> findAll() {
        return trackedLinkJpaRepository.findAllByOrderByIdAsc().stream().map(this::toModel).toList();
    }

    @Override
    public List<TrackedLink> findPage(long offset, int limit) {
        int page = (int) (offset / limit);
        return trackedLinkJpaRepository
                .findAll(PageRequest.of(page, limit, Sort.by(Sort.Direction.ASC, "id")))
                .getContent()
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public void updatePollingState(long linkId, Instant lastCheckedAt, Instant lastUpdatedAt) {
        trackedLinkJpaRepository.findById(linkId).ifPresent(entity -> {
            entity.setLastCheckedAt(lastCheckedAt);
            entity.setLastUpdatedAt(lastUpdatedAt);
            trackedLinkJpaRepository.save(entity);
        });
    }

    @Override
    public void deleteById(long id) {
        trackedLinkJpaRepository.deleteById(id);
    }

    private TrackedLinkEntity toEntity(TrackedLink link) {
        return new TrackedLinkEntity(
                link.id(),
                link.url(),
                link.type().name(),
                link.createdAt(),
                link.lastCheckedAt(),
                link.lastUpdatedAt());
    }

    private TrackedLink toModel(TrackedLinkEntity entity) {
        return new TrackedLink(
                entity.getId(),
                entity.getUrl(),
                LinkSourceType.valueOf(entity.getType()),
                entity.getCreatedAt(),
                entity.getLastCheckedAt(),
                entity.getLastUpdatedAt());
    }
}
