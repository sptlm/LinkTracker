package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "link_subscription")
public class LinkSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "link_subscription_seq")
    @SequenceGenerator(name = "link_subscription_seq", sequenceName = "link_subscription_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "subscription_tag", joinColumns = @JoinColumn(name = "subscription_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    protected LinkSubscriptionEntity() {}

    public LinkSubscriptionEntity(Long id, Long chatId, Long linkId, Instant createdAt, Set<String> tags) {
        this.id = id;
        this.chatId = chatId;
        this.linkId = linkId;
        this.createdAt = createdAt;
        this.tags = new LinkedHashSet<>(tags);
    }

    public Long getId() {
        return id;
    }

    public Long getChatId() {
        return chatId;
    }

    public Long getLinkId() {
        return linkId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = new LinkedHashSet<>(tags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LinkSubscriptionEntity that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
