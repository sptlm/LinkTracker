package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateGroupingService {

    private final AiAgentProperties properties;
    private final ProcessedUpdatePublisher updatePublisher;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(daemonThreadFactory());
    private final Map<Long, List<LinkUpdate>> updatesByChatId = new HashMap<>();

    public void accept(LinkUpdate update) {
        List<Long> chatIds = update.getTgChatIds() == null ? List.of() : update.getTgChatIds();
        for (Long chatId : chatIds) {
            if (chatId != null) {
                addToGroup(chatId, copyForChat(update, chatId));
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private synchronized void addToGroup(Long chatId, LinkUpdate update) {
        boolean newGroup = !updatesByChatId.containsKey(chatId);
        updatesByChatId.computeIfAbsent(chatId, ignored -> new ArrayList<>()).add(update);
        if (newGroup) {
            long windowMillis = properties.getGrouping().getWindowMs().toMillis();
            scheduler.schedule(() -> flush(chatId), windowMillis, TimeUnit.MILLISECONDS);
        }
    }

    void flush(Long chatId) {
        List<LinkUpdate> updates;
        synchronized (this) {
            updates = updatesByChatId.remove(chatId);
        }

        if (updates == null || updates.isEmpty()) {
            return;
        }

        updatePublisher.publish(toProcessedUpdate(chatId, updates));
    }

    private LinkUpdate toProcessedUpdate(Long chatId, List<LinkUpdate> updates) {
        if (updates.size() == 1) {
            return updates.getFirst();
        }

        LinkUpdate first = updates.getFirst();
        return new LinkUpdate()
                .id(first.getId())
                .url(first.getUrl())
                .description(numberedDescription(updates))
                .author(first.getAuthor())
                .tgChatIds(List.of(chatId))
                .priority(maxPriority(updates).name());
    }

    private String numberedDescription(List<LinkUpdate> updates) {
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < updates.size(); i++) {
            if (i > 0) {
                description.append(System.lineSeparator());
            }
            description.append(i + 1).append(". ").append(updates.get(i).getDescription());
        }
        return description.toString();
    }

    private UpdatePriority maxPriority(List<LinkUpdate> updates) {
        UpdatePriority maxPriority = UpdatePriority.LOW;
        for (LinkUpdate update : updates) {
            maxPriority = UpdatePriority.max(maxPriority, priorityOf(update));
        }
        return maxPriority;
    }

    private UpdatePriority priorityOf(LinkUpdate update) {
        try {
            return UpdatePriority.valueOf(update.getPriority());
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return UpdatePriority.MEDIUM;
        }
    }

    private LinkUpdate copyForChat(LinkUpdate update, Long chatId) {
        return new LinkUpdate()
                .id(update.getId())
                .url(update.getUrl())
                .description(update.getDescription())
                .author(update.getAuthor())
                .tgChatIds(List.of(chatId))
                .priority(update.getPriority());
    }

    private static ThreadFactory daemonThreadFactory() {
        return task -> {
            Thread thread = new Thread(task, "ai-agent-update-grouping");
            thread.setDaemon(true);
            return thread;
        };
    }
}
