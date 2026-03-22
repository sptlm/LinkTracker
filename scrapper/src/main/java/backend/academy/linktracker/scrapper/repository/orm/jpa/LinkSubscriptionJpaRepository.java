package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.LinkSubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.LinkSubscriptionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LinkSubscriptionJpaRepository extends JpaRepository<LinkSubscriptionEntity, LinkSubscriptionId> {

    List<LinkSubscriptionEntity> findAllByIdChatIdOrderByIdLinkIdAsc(long chatId);

    List<LinkSubscriptionEntity> findAllByIdLinkIdOrderByIdChatIdAsc(long linkId);

    boolean existsByIdChatIdAndIdLinkId(long chatId, long linkId);

    @Query("select s.id.chatId from LinkSubscriptionEntity s where s.id.linkId = :linkId order by s.id.chatId")
    List<Long> findChatIdsByLinkId(long linkId);

    long countByIdLinkId(long linkId);
}
