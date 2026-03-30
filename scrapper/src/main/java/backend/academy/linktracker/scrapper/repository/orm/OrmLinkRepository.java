package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.jpa.TrackedLinkJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "ORM")
public class OrmLinkRepository implements LinkRepository {

    private final TrackedLinkJpaRepository trackedLinkJpaRepository;
    private final TrackedLinkOrmConverter converter;

    public OrmLinkRepository(TrackedLinkJpaRepository trackedLinkJpaRepository, TrackedLinkOrmConverter converter) {
        this.trackedLinkJpaRepository = trackedLinkJpaRepository;
        this.converter = converter;
    }

    @Override
    public TrackedLink save(TrackedLink link) {
        TrackedLinkEntity saved = trackedLinkJpaRepository.save(converter.toEntity(link));
        return converter.toModel(saved);
    }

    @Override
    public Optional<TrackedLink> findById(long id) {
        return trackedLinkJpaRepository.findById(id).map(converter::toModel);
    }

    @Override
    public List<TrackedLink> findAllById(List<Long> ids) {
        return trackedLinkJpaRepository.findAllByIdInOrderByIdAsc(ids).stream()
            .map(converter::toModel)
            .toList();
    }

    @Override
    public Optional<TrackedLink> findByUrl(String url) {
        return trackedLinkJpaRepository.findByUrl(url).map(converter::toModel);
    }

    @Override
    public List<TrackedLink> findAll() {
        return trackedLinkJpaRepository.findAllByOrderByIdAsc().stream()
            .map(converter::toModel)
            .toList();
    }

    @Override
    public List<TrackedLink> findPage(long offset, int limit) {
        return trackedLinkJpaRepository.findPage(offset, limit).stream()
            .map(converter::toModel)
            .toList();
    }

    @Override
    @Transactional
    public void updatePollingState(long linkId, Instant lastCheckedAt, Instant lastUpdatedAt) {
        trackedLinkJpaRepository.updatePollingState(linkId, lastCheckedAt, lastUpdatedAt);
    }

    @Override
    public void deleteById(long id) {
        trackedLinkJpaRepository.deleteById(id);
    }
}
