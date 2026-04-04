package backend.academy.linktracker.scrapper.repository.orm.jpa;

import backend.academy.linktracker.scrapper.repository.orm.entity.LinkSubscriptionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LinkSubscriptionJpaRepository extends JpaRepository<LinkSubscriptionEntity, Long> {

    @EntityGraph(attributePaths = "tags")
    List<LinkSubscriptionEntity> findAllByChatIdOrderByLinkIdAsc(long chatId);

    @EntityGraph(attributePaths = "tags")
    List<LinkSubscriptionEntity> findAllByLinkIdOrderByChatIdAsc(long linkId);

    boolean existsByChatIdAndLinkId(long chatId, long linkId);

    @EntityGraph(attributePaths = "tags")
    Optional<LinkSubscriptionEntity> findByChatIdAndLinkId(long chatId, long linkId);

    @Query("select s.chatId from LinkSubscriptionEntity s where s.linkId = :linkId order by s.chatId")
    List<Long> findChatIdsByLinkId(long linkId);

    long countByLinkId(long linkId);

    void deleteByChatIdAndLinkId(long chatId, long linkId);
}
