package backend.academy.linktracker.scrapper.integration;

import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

public abstract class AbstractPostgresValkeyIT extends AbstractPostgresIT {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> VALKEY_CLUSTER_NODE_1 = TestcontainersConfiguration.VALKEY_CLUSTER_NODE_1;

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> VALKEY_CLUSTER_NODE_2 = TestcontainersConfiguration.VALKEY_CLUSTER_NODE_2;

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> VALKEY_CLUSTER_NODE_3 = TestcontainersConfiguration.VALKEY_CLUSTER_NODE_3;

    @DynamicPropertySource
    static void registerValkeyProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.cluster.nodes", TestcontainersConfiguration::valkeyClusterNodes);
        registry.add("app.valkey.cache.enabled", () -> true);
    }
}
