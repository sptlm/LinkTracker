package backend.academy.linktracker.scrapper.repository.orm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tg_chat")
public class TgChatEntity {

    @Id
    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TgChatEntity() {}

    public TgChatEntity(Long chatId, Instant createdAt) {
        this.chatId = chatId;
        this.createdAt = createdAt;
    }

    public Long getChatId() {
        return chatId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
