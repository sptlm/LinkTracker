package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.ValkeyCacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkListCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ValkeyCacheProperties properties;
    private final LinkListClientSideCache clientSideCache;

    public Optional<ListLinksResponse> get(long chatId) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }

        String key = cacheKey(chatId);
        String cachedValue;
        try {
            cachedValue = clientSideCache
                    .get(key, () -> redisTemplate.opsForValue().get(key))
                    .orElse(null);
        } catch (RuntimeException e) {
            logCacheFailure("read", chatId, e);
            return Optional.empty();
        }

        if (cachedValue == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(cachedValue, ListLinksResponse.class));
        } catch (JsonProcessingException e) {
            log.atWarn().addKeyValue("chatId", chatId).log("Invalid cached links response, evicting entry");
            evict(chatId);
            return Optional.empty();
        }
    }

    public void put(long chatId, ListLinksResponse response) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String key = cacheKey(chatId);
            String payload = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, payload, properties.getTtl());
        } catch (JsonProcessingException e) {
            logCacheFailure("serialize", chatId, e);
        } catch (RuntimeException e) {
            logCacheFailure("write", chatId, e);
        }
    }

    public void evictAfterCommit(long chatId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(chatId);
                }
            });
            return;
        }

        evict(chatId);
    }

    public void evict(long chatId) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            String key = cacheKey(chatId);
            redisTemplate.delete(key);
            clientSideCache.evictLocal(key);
        } catch (RuntimeException e) {
            logCacheFailure("evict", chatId, e);
        }
    }

    private String cacheKey(long chatId) {
        return String.valueOf(chatId);
    }

    private void logCacheFailure(String operation, long chatId, Exception e) {
        log.atWarn()
                .addKeyValue("operation", operation)
                .addKeyValue("chatId", chatId)
                .setCause(e)
                .log("Valkey links cache operation failed");
    }
}
