package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tracked_link")
public class TrackedLinkEntity {

    @Id
    private Long id;

    @Column(name = "url", nullable = false, unique = true)
    private String url;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    protected TrackedLinkEntity() {}

    public TrackedLinkEntity(
            Long id, String url, String type, Instant createdAt, Instant lastCheckedAt, Instant lastUpdatedAt) {
        this.id = id;
        this.url = url;
        this.type = type;
        this.createdAt = createdAt;
        this.lastCheckedAt = lastCheckedAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastCheckedAt(Instant lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
