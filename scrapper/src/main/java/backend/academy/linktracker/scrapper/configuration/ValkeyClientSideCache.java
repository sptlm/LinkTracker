package backend.academy.linktracker.scrapper.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.RedisClusterPubSubAdapter;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCache;

@Slf4j
public class ValkeyClientSideCache extends CaffeineCache implements Closeable {

    private final StatefulRedisClusterConnection<String, String> connection;
    private final StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;
    private final ObjectMapper objectMapper;
    private final Class<?> valueType;
    private final Duration ttl;
    private final CacheInvalidationListener invalidationListener;

    public ValkeyClientSideCache(
            String name,
            StatefulRedisClusterConnection<String, String> connection,
            StatefulRedisClusterPubSubConnection<String, String> pubSubConnection,
            ObjectMapper objectMapper,
            Class<?> valueType,
            Duration ttl,
            int capacity) {
        super(
                name,
                Caffeine.newBuilder()
                        .maximumSize(capacity)
                        .expireAfterWrite(ttl.toMillis(), TimeUnit.MILLISECONDS)
                        .build(),
                false);
        this.connection = connection;
        this.pubSubConnection = pubSubConnection;
        this.objectMapper = objectMapper;
        this.valueType = valueType;
        this.ttl = ttl;
        this.invalidationListener = new CacheInvalidationListener(name, getNativeCache());
        this.pubSubConnection.addListener(invalidationListener);
    }

    @Override
    protected Object lookup(Object key) {
        Object value = super.lookup(key);
        if (value != null) {
            return value;
        }

        String redisKey = redisKey(key);
        try {
            String payload = connection.sync().get(redisKey);
            if (payload == null) {
                return null;
            }
            Object response = objectMapper.readValue(payload, valueType);
            getNativeCache().put(key, response);
            return response;
        } catch (Exception e) {
            log.atWarn().addKeyValue("key", redisKey).setCause(e).log("Valkey cache lookup failed");
            return null;
        }
    }

    @Override
    public void put(Object key, Object value) {
        super.put(key, value);
        String redisKey = redisKey(key);
        try {
            String payload = objectMapper.writeValueAsString(value);
            connection.sync().psetex(redisKey, ttl.toMillis(), payload);
        } catch (Exception e) {
            log.atWarn().addKeyValue("key", redisKey).setCause(e).log("Valkey cache put failed");
        }
    }

    @Override
    public void evict(Object key) {
        super.evict(key);
        String redisKey = redisKey(key);
        try {
            connection.sync().del(redisKey);
        } catch (Exception e) {
            log.atWarn().addKeyValue("key", redisKey).setCause(e).log("Valkey cache evict failed");
        }
    }

    @Override
    public void clear() {
        super.clear();
        try {
            connection.sync().del(connection.sync().keys(getName() + "::*").toArray(String[]::new));
        } catch (Exception e) {
            log.atWarn().addKeyValue("cacheName", getName()).setCause(e).log("Valkey cache clear failed");
        }
    }

    @Override
    public void close() {
        pubSubConnection.removeListener(invalidationListener);
    }

    private String redisKey(Object key) {
        return getName() + "::" + key;
    }

    private static class CacheInvalidationListener extends RedisClusterPubSubAdapter<String, String> {

        private final String keyPrefix;
        private final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;

        CacheInvalidationListener(String cacheName, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
            this.keyPrefix = cacheName + "::";
            this.cache = cache;
        }

        @Override
        public void message(RedisClusterNode node, String channel, String message) {
            if (message.startsWith(keyPrefix)) {
                cache.invalidate(message.substring(keyPrefix.length()));
            }
        }

        @Override
        public void message(RedisClusterNode node, String pattern, String channel, String message) {
            message(node, channel, message);
        }
    }
}
