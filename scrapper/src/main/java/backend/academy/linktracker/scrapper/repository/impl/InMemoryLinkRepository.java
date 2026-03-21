package backend.academy.linktracker.scrapper.repository.impl;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLinkRepository implements LinkRepository {

    private final AtomicLong idSequence = new AtomicLong(0);
    private final Map<Long, TrackedLink> linksById = new ConcurrentHashMap<>();
    private final Map<String, Long> idByUrl = new ConcurrentHashMap<>();

    @Override
    public long nextId() {
        return idSequence.incrementAndGet();
    }

    @Override
    public TrackedLink save(TrackedLink link) {
        linksById.put(link.id(), link);
        idByUrl.put(link.url(), link.id());
        return link;
    }

    @Override
    public Optional<TrackedLink> findById(long id) {
        return Optional.ofNullable(linksById.get(id));
    }

    @Override
    public List<TrackedLink> findAllById(List<Long> ids) {
        return ids.stream()
                .map(linksById::get)
                .filter(link -> link != null)
                .sorted(Comparator.comparingLong(TrackedLink::id))
                .toList();
    }

    @Override
    public Optional<TrackedLink> findByUrl(String url) {
        Long id = idByUrl.get(url);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(linksById.get(id));
    }

    @Override
    public List<TrackedLink> findAll() {
        return linksById.values().stream().sorted(Comparator.comparingLong(TrackedLink::id)).toList();
    }

    @Override
    public void updatePollingState(long linkId, Instant lastCheckedAt, Instant lastUpdatedAt) {
        linksById.computeIfPresent(linkId, (id, link) -> link.withPollingState(lastCheckedAt, lastUpdatedAt));
    }

    @Override
    public void deleteById(long id) {
        TrackedLink removed = linksById.remove(id);
        if (removed != null) {
            idByUrl.remove(removed.url());
        }
    }
}
