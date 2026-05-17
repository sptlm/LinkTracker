package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.generated.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.properties.ValkeyCacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import java.util.Map;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@EnableCaching
@Configuration
public class ValkeyCacheConfiguration {

    public static final String LINK_LIST_CACHE = "link-list";
    public static final String LINK_LIST_KEY_PREFIX = "chat#";

    @Bean
    @ConditionalOnProperty(prefix = "app.valkey.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "app.valkey.cache.client-side", name = "enabled", havingValue = "false")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper,
            ValkeyCacheProperties properties) {
        RedisCacheConfiguration linkListCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.getTtl())
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(objectMapper, ListLinksResponse.class)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(linkListCacheConfiguration)
                .withInitialCacheConfigurations(Map.of(LINK_LIST_CACHE, linkListCacheConfiguration))
                .disableCreateOnMissingCache()
                .transactionAware()
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.valkey.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(
            prefix = "app.valkey.cache.client-side",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public CacheManager clientSideCacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper,
            ValkeyCacheProperties properties,
            StatefulRedisClusterConnection<String, String> valkeyClusterConnection,
            StatefulRedisClusterPubSubConnection<String, String> valkeyClusterPubSubConnection) {
        return new TransactionAwareCacheManagerProxy(new ValkeyClientSideCacheManager(
                valkeyClusterConnection,
                valkeyClusterPubSubConnection,
                objectMapper,
                Map.of(LINK_LIST_CACHE, ListLinksResponse.class),
                properties.getTtl(),
                properties.getClientSide().getMaxSize()));
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "app.valkey.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ClientResources valkeyClientResources(ValkeyCacheProperties properties) {
        return DefaultClientResources.builder()
                .socketAddressResolver(MappingSocketAddressResolver.create(
                        (Function<HostAndPort, HostAndPort>) address -> mapAddress(address, properties)))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.valkey.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LettuceClientConfigurationBuilderCustomizer valkeyClientResourcesCustomizer(
            ClientResources valkeyClientResources) {
        return builder -> builder.clientResources(valkeyClientResources);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "app.valkey.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(
            prefix = "app.valkey.cache.client-side",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public RedisClusterClient valkeyClusterClient(
            DataRedisProperties redisProperties, ClientResources valkeyClientResources) {
        RedisClusterClient client = RedisClusterClient.create(
                valkeyClientResources,
                redisProperties.getCluster().getNodes().stream()
                        .map(ValkeyCacheConfiguration::redisUri)
                        .toList());
        client.setOptions(ClusterClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3)
                .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder()
                        .enableAllAdaptiveRefreshTriggers()
                        .build())
                .build());
        return client;
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.valkey.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(
            prefix = "app.valkey.cache.client-side",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public StatefulRedisClusterConnection<String, String> valkeyClusterConnection(RedisClusterClient valkeyClusterClient) {
        return valkeyClusterClient.connect();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.valkey.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(
            prefix = "app.valkey.cache.client-side",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public StatefulRedisClusterPubSubConnection<String, String> valkeyClusterPubSubConnection(
            RedisClusterClient valkeyClusterClient) {
        return valkeyClusterClient.connectPubSub();
    }

    private static RedisURI redisUri(String node) {
        String[] hostAndPort = node.split(":", 2);
        return RedisURI.create(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
    }

    private static HostAndPort mapAddress(HostAndPort address, ValkeyCacheProperties properties) {
        ValkeyCacheProperties.HostMapping hostMapping = properties.getHostMapping();
        if (hostMapping.isEnabled() && hostMapping.getSource().equalsIgnoreCase(address.getHostText())) {
            return HostAndPort.of(hostMapping.getTarget(), address.getPort());
        }
        return address;
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.valkey.cache", name = "enabled", havingValue = "false")
    public CacheManager noOpCacheManager() {
        return new NoOpCacheManager();
    }
}
