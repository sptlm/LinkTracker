package backend.academy.linktracker.scrapper.repository;

import java.util.List;

public interface TagRepository {

    boolean exists(long chatId, long linkId, String tag);

    List<String> findByChatIdAndLinkId(long chatId, long linkId);

    void add(long chatId, long linkId, String tag);

    void replace(long chatId, long linkId, List<String> tags);

    void delete(long chatId, long linkId, String tag);
}
