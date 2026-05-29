package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackedLinkJpaRepository extends JpaRepository<TrackedLinkEntity, Long> {

    Optional<TrackedLinkEntity> findByUrl(String url);

    List<TrackedLinkEntity> findAllByOrderByIdAsc();

    List<TrackedLinkEntity> findAllByIdInOrderByIdAsc(List<Long> ids);

    @Query(value = "select * from tracked_link order by id limit :limit offset :offset", nativeQuery = true)
    List<TrackedLinkEntity> findPage(@Param("offset") long offset, @Param("limit") int limit);

    long countByType(String type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TrackedLinkEntity t
            set t.lastCheckedAt = :lastCheckedAt,
                t.lastUpdatedAt = :lastUpdatedAt
            where t.id = :linkId
            """)
    int updatePollingState(
            @Param("linkId") long linkId,
            @Param("lastCheckedAt") Instant lastCheckedAt,
            @Param("lastUpdatedAt") Instant lastUpdatedAt);
}
