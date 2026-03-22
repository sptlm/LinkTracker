package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class LinkSubscriptionId implements Serializable {

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "link_id", nullable = false)
    private Long linkId;

    protected LinkSubscriptionId() {}

    public LinkSubscriptionId(Long chatId, Long linkId) {
        this.chatId = chatId;
        this.linkId = linkId;
    }

    public Long getChatId() {
        return chatId;
    }

    public Long getLinkId() {
        return linkId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LinkSubscriptionId that)) {
            return false;
        }
        return Objects.equals(chatId, that.chatId) && Objects.equals(linkId, that.linkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, linkId);
    }
}
