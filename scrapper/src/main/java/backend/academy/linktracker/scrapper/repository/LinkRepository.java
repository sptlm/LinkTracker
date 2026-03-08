package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LinkRepository {

    long nextId();

    TrackedLink save(TrackedLink link);

    Optional<TrackedLink> findById(long id);

    Optional<TrackedLink> findByUrl(String url);

    List<TrackedLink> findAll();

    void updatePollingState(long linkId, Instant lastCheckedAt, Instant lastUpdatedAt);

    void deleteById(long id);
}
