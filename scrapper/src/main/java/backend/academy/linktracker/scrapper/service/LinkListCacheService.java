package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.ValkeyCacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
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
    private final ExecutorService cacheExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public Optional<ListLinksResponse> get(long chatId) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }

        String key = cacheKey(chatId);
        String cachedValue = executeCacheOperation("read", chatId, () -> clientSideCache
                        .get(key, () -> redisTemplate.opsForValue().get(key))
                        .orElse(null))
                .orElse(null);

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
            executeCacheOperation("write", chatId, () -> {
                redisTemplate.opsForValue().set(key, payload, properties.getTtl());
                return null;
            });
        } catch (JsonProcessingException e) {
            logCacheFailure("serialize", chatId, e);
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

        String key = cacheKey(chatId);
        executeCacheOperation("evict", chatId, () -> {
            redisTemplate.delete(key);
            clientSideCache.evictLocal(key);
            return null;
        });
    }

    private String cacheKey(long chatId) {
        return String.valueOf(chatId);
    }

    private <T> Optional<T> executeCacheOperation(String operation, long chatId, Supplier<T> supplier) {
        Future<T> future = cacheExecutor.submit(supplier::get);
        try {
            return Optional.ofNullable(future.get(operationTimeoutMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            logCacheFailure(operation + "-timeout", chatId, e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logCacheFailure(operation, chatId, e);
            return Optional.empty();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            logCacheFailure(operation, chatId, cause);
            return Optional.empty();
        }
    }

    private long operationTimeoutMillis() {
        return Math.max(1, properties.getOperationTimeout().toMillis());
    }

    @PreDestroy
    public void close() {
        cacheExecutor.shutdownNow();
    }

    private void logCacheFailure(String operation, long chatId, Throwable e) {
        log.atWarn()
                .addKeyValue("operation", operation)
                .addKeyValue("chatId", chatId)
                .setCause(e)
                .log("Valkey links cache operation failed");
    }
}
