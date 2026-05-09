package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.properties.ValkeyCacheProperties;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.CacheFrontend.ValueRetrievalException;
import io.lettuce.core.support.caching.ClientSideCaching;
import jakarta.annotation.PreDestroy;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LinkListClientSideCache implements Closeable {

    private final DataRedisProperties redisProperties;
    private final ValkeyCacheProperties cacheProperties;
    private final ClientResources clientResources;
    private final Object monitor = new Object();
    private final Cache<String, String> localCache;
    private final Map<String, NodeCache> clusterNodeCaches = new ConcurrentHashMap<>();

    private volatile RedisClient client;
    private volatile StatefulRedisConnection<String, String> connection;
    private volatile CacheFrontend<String, String> cacheFrontend;
    private volatile RedisClusterClient clusterClient;
    private volatile StatefulRedisClusterConnection<String, String> clusterConnection;
    private volatile boolean unavailable;

    public LinkListClientSideCache(
            DataRedisProperties redisProperties,
            ValkeyCacheProperties cacheProperties,
            ClientResources clientResources) {
        this.redisProperties = redisProperties;
        this.cacheProperties = cacheProperties;
        this.clientResources = clientResources;
        this.localCache = CacheBuilder.newBuilder()
                .maximumSize(cacheProperties.getClientSide().getMaxSize())
                .build();
    }

    public Optional<String> get(String key, Supplier<String> serverLookup) {
        if (!isEnabled()) {
            return Optional.ofNullable(serverLookup.get());
        }

        if (hasClusterNodes()) {
            return getFromCluster(key, serverLookup);
        }

        CacheFrontend<String, String> frontend = cacheFrontend();
        StatefulRedisConnection<String, String> currentConnection = connection;
        if (frontend == null || currentConnection == null) {
            return Optional.ofNullable(serverLookup.get());
        }

        try {
            return Optional.ofNullable(
                    frontend.get(key, () -> currentConnection.sync().get(key)));
        } catch (ValueRetrievalException e) {
            log.atDebug().addKeyValue("key", key).log("Valkey client-side cache miss");
            return Optional.empty();
        } catch (RuntimeException e) {
            log.atWarn().addKeyValue("key", key).setCause(e).log("Valkey client-side cache read failed");
            return Optional.ofNullable(serverLookup.get());
        }
    }

    public void evictLocal(String key) {
        localCache.invalidate(key);
    }

    public void clearLocal() {
        localCache.invalidateAll();
    }

    private boolean isEnabled() {
        return cacheProperties.isEnabled() && cacheProperties.getClientSide().isEnabled();
    }

    private Optional<String> getFromCluster(String key, Supplier<String> serverLookup) {
        StatefulRedisClusterConnection<String, String> currentClusterConnection = clusterConnection();
        if (currentClusterConnection == null) {
            return Optional.ofNullable(serverLookup.get());
        }

        NodeCache nodeCache = nodeCacheFor(key, currentClusterConnection);
        if (nodeCache == null) {
            return Optional.ofNullable(serverLookup.get());
        }

        try {
            return Optional.ofNullable(nodeCache
                    .frontend()
                    .get(key, () -> nodeCache.connection().sync().get(key)));
        } catch (ValueRetrievalException e) {
            log.atDebug().addKeyValue("key", key).log("Valkey client-side cache miss");
            return Optional.empty();
        } catch (RuntimeException e) {
            log.atWarn().addKeyValue("key", key).setCause(e).log("Valkey cluster client-side cache read failed");
            return Optional.ofNullable(serverLookup.get());
        }
    }

    private StatefulRedisClusterConnection<String, String> clusterConnection() {
        if (unavailable || clusterConnection != null) {
            return clusterConnection;
        }

        synchronized (monitor) {
            if (unavailable || clusterConnection != null) {
                return clusterConnection;
            }

            try {
                RedisClusterClient newClient = RedisClusterClient.create(clientResources, clusterRedisUris());
                newClient.setOptions(ClusterClientOptions.builder()
                        .protocolVersion(ProtocolVersion.RESP3)
                        .build());
                StatefulRedisClusterConnection<String, String> newConnection = newClient.connect(StringCodec.UTF8);
                this.clusterClient = newClient;
                this.clusterConnection = newConnection;
                log.info("Valkey cluster client-side cache enabled for link list responses");
                return newConnection;
            } catch (RuntimeException e) {
                unavailable = true;
                log.atWarn()
                        .setCause(e)
                        .log("Valkey cluster client-side cache is unavailable, falling back to server cache");
                close();
                return null;
            }
        }
    }

    private NodeCache nodeCacheFor(
            String key, StatefulRedisClusterConnection<String, String> currentClusterConnection) {
        RedisClusterNode node = currentClusterConnection.getPartitions().getMasterBySlot(SlotHash.getSlot(key));
        if (node == null) {
            try {
                clusterClient.refreshPartitions();
                node = currentClusterConnection.getPartitions().getMasterBySlot(SlotHash.getSlot(key));
            } catch (RuntimeException e) {
                log.atWarn().addKeyValue("key", key).setCause(e).log("Failed to refresh Valkey cluster topology");
            }
        }
        if (node == null) {
            return null;
        }

        RedisURI uri = node.getUri();
        String nodeKey = node.getNodeId() != null ? node.getNodeId() : uri.getHost() + ":" + uri.getPort();
        return clusterNodeCaches.computeIfAbsent(nodeKey, ignored -> createNodeCache(uri));
    }

    private NodeCache createNodeCache(RedisURI uri) {
        StatefulRedisConnection<String, String> nodeConnection =
                clusterConnection.getConnection(uri.getHost(), uri.getPort());
        CacheFrontend<String, String> nodeFrontend = ClientSideCaching.enable(
                CacheAccessor.forMap(localCache.asMap()), nodeConnection, new TrackingArgs().enabled(true));
        return new NodeCache(nodeConnection, nodeFrontend);
    }

    private CacheFrontend<String, String> cacheFrontend() {
        if (unavailable || cacheFrontend != null) {
            return cacheFrontend;
        }

        synchronized (monitor) {
            if (unavailable || cacheFrontend != null) {
                return cacheFrontend;
            }

            try {
                RedisClient newClient = RedisClient.create(clientResources, redisUri());
                newClient.setOptions(ClientOptions.builder()
                        .protocolVersion(ProtocolVersion.RESP3)
                        .build());
                StatefulRedisConnection<String, String> newConnection = newClient.connect(StringCodec.UTF8);
                CacheFrontend<String, String> newFrontend = ClientSideCaching.enable(
                        CacheAccessor.forMap(localCache.asMap()), newConnection, new TrackingArgs().enabled(true));
                this.client = newClient;
                this.connection = newConnection;
                this.cacheFrontend = newFrontend;
                log.info("Valkey client-side cache enabled for link list responses");
                return newFrontend;
            } catch (RuntimeException e) {
                unavailable = true;
                log.atWarn().setCause(e).log("Valkey client-side cache is unavailable, falling back to server cache");
                close();
                return null;
            }
        }
    }

    private RedisURI redisUri() {
        RedisURI.Builder builder = RedisURI.Builder.redis(redisProperties.getHost(), redisProperties.getPort());
        if (redisProperties.getTimeout() != null) {
            builder.withTimeout(redisProperties.getTimeout());
        }
        if (redisProperties.getUsername() != null && redisProperties.getPassword() != null) {
            builder.withAuthentication(redisProperties.getUsername(), redisProperties.getPassword());
        } else if (redisProperties.getPassword() != null) {
            builder.withPassword(redisProperties.getPassword().toCharArray());
        }
        return builder.build();
    }

    private List<RedisURI> clusterRedisUris() {
        List<RedisURI> uris = new ArrayList<>();
        for (String node : redisProperties.getCluster().getNodes()) {
            if (node == null || node.isBlank()) {
                continue;
            }
            String[] hostAndPort = node.split(":", 2);
            RedisURI.Builder builder = RedisURI.Builder.redis(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
            if (redisProperties.getTimeout() != null) {
                builder.withTimeout(redisProperties.getTimeout());
            }
            if (redisProperties.getUsername() != null && redisProperties.getPassword() != null) {
                builder.withAuthentication(redisProperties.getUsername(), redisProperties.getPassword());
            } else if (redisProperties.getPassword() != null) {
                builder.withPassword(redisProperties.getPassword().toCharArray());
            }
            uris.add(builder.build());
        }
        return uris;
    }

    private boolean hasClusterNodes() {
        if (redisProperties.getCluster() == null) {
            return false;
        }
        List<String> nodes = redisProperties.getCluster().getNodes();
        return nodes != null && nodes.stream().anyMatch(node -> node != null && !node.isBlank());
    }

    @Override
    @PreDestroy
    public void close() {
        CacheFrontend<String, String> frontend = cacheFrontend;
        StatefulRedisConnection<String, String> currentConnection = connection;
        RedisClient currentClient = client;
        StatefulRedisClusterConnection<String, String> currentClusterConnection = clusterConnection;
        RedisClusterClient currentClusterClient = clusterClient;
        cacheFrontend = null;
        connection = null;
        client = null;
        clusterConnection = null;
        clusterClient = null;

        if (frontend != null) {
            frontend.close();
        }
        clusterNodeCaches.values().forEach(NodeCache::close);
        clusterNodeCaches.clear();
        if (currentConnection != null) {
            currentConnection.close();
        }
        if (currentClient != null) {
            currentClient.shutdown();
        }
        if (currentClusterConnection != null) {
            currentClusterConnection.close();
        }
        if (currentClusterClient != null) {
            currentClusterClient.shutdown();
        }
    }

    private record NodeCache(StatefulRedisConnection<String, String> connection, CacheFrontend<String, String> frontend)
            implements Closeable {

        @Override
        public void close() {
            frontend.close();
            connection.close();
        }
    }
}
