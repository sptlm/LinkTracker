package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.TrackedLinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedLinkJpaRepository extends JpaRepository<TrackedLinkEntity, Long> {

    Optional<TrackedLinkEntity> findByUrl(String url);

    List<TrackedLinkEntity> findAllByOrderByIdAsc();
}
