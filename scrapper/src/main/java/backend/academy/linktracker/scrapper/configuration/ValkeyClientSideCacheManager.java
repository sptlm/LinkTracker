package backend.academy.linktracker.scrapper.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

@Slf4j
@RequiredArgsConstructor
public class ValkeyClientSideCacheManager extends AbstractCacheManager {

    private static final String[] KEYEVENT_PATTERNS = {"__keyevent@*__:del", "__keyevent@*__:expired"};
    private static final String KEYEVENTS_CONFIG = "Egx";

    private final StatefulRedisClusterConnection<String, String> connection;
    private final StatefulRedisClusterPubSubConnection<String, String> pubSubConnection;
    private final ObjectMapper objectMapper;
    private final Map<String, Class<?>> valueTypes;
    private final Duration ttl;
    private final int capacity;

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return List.of();
    }

    @Override
    protected Cache getMissingCache(String name) {
        Class<?> valueType = valueTypes.get(name);
        if (valueType == null) {
            return null;
        }
        return new ValkeyClientSideCache(name, connection, pubSubConnection, objectMapper, valueType, ttl, capacity);
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        connection.sync().configSet("notify-keyspace-events", KEYEVENTS_CONFIG);
        pubSubConnection.sync().psubscribe(KEYEVENT_PATTERNS);
        log.info("Subscribed to Valkey keyevent patterns for client-side cache invalidation");
    }

    @PreDestroy
    public void destroy() {
        pubSubConnection.sync().punsubscribe(KEYEVENT_PATTERNS);
        getCacheNames().stream()
                .map(this::getCache)
                .filter(ValkeyClientSideCache.class::isInstance)
                .map(ValkeyClientSideCache.class::cast)
                .forEach(ValkeyClientSideCache::close);
    }
}
