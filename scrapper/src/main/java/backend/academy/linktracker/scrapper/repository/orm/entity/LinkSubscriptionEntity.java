package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "link_subscription")
public class LinkSubscriptionEntity {

    @EmbeddedId
    private LinkSubscriptionId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "subscription_tag",
            joinColumns = {
                @JoinColumn(name = "chat_id", referencedColumnName = "chat_id"),
                @JoinColumn(name = "link_id", referencedColumnName = "link_id")
            })
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    protected LinkSubscriptionEntity() {}

    public LinkSubscriptionEntity(LinkSubscriptionId id, Instant createdAt, Set<String> tags) {
        this.id = id;
        this.createdAt = createdAt;
        this.tags = new LinkedHashSet<>(tags);
    }

    public LinkSubscriptionId getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<String> getTags() {
        return tags;
    }
}
