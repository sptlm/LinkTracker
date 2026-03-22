package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.TagRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "app.persistence", name = "access-type", havingValue = "ORM")
public class OrmTagRepository implements TagRepository {

    private final EntityManager entityManager;

    public OrmTagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean exists(long chatId, long linkId, String tag) {
        Number count = (Number) entityManager
                .createNativeQuery(
                        "select count(*) from subscription_tag where chat_id = :chatId and link_id = :linkId and tag = :tag")
                .setParameter("chatId", chatId)
                .setParameter("linkId", linkId)
                .setParameter("tag", tag)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public List<String> findByChatIdAndLinkId(long chatId, long linkId) {
        return entityManager
                .createNativeQuery("""
                        select tag
                        from subscription_tag
                        where chat_id = :chatId and link_id = :linkId
                        order by tag
                        """)
                .setParameter("chatId", chatId)
                .setParameter("linkId", linkId)
                .getResultList()
                .stream()
                .map(String::valueOf)
                .toList();
    }

    @Override
    public void add(long chatId, long linkId, String tag) {
        entityManager
                .createNativeQuery(
                        "insert into subscription_tag (chat_id, link_id, tag) values (:chatId, :linkId, :tag)")
                .setParameter("chatId", chatId)
                .setParameter("linkId", linkId)
                .setParameter("tag", tag)
                .executeUpdate();
    }

    @Override
    public void replace(long chatId, long linkId, List<String> tags) {
        entityManager
                .createNativeQuery("delete from subscription_tag where chat_id = :chatId and link_id = :linkId")
                .setParameter("chatId", chatId)
                .setParameter("linkId", linkId)
                .executeUpdate();

        for (String tag : tags) {
            add(chatId, linkId, tag);
        }
    }

    @Override
    public void delete(long chatId, long linkId, String tag) {
        entityManager
                .createNativeQuery(
                        "delete from subscription_tag where chat_id = :chatId and link_id = :linkId and tag = :tag")
                .setParameter("chatId", chatId)
                .setParameter("linkId", linkId)
                .setParameter("tag", tag)
                .executeUpdate();
    }
}
