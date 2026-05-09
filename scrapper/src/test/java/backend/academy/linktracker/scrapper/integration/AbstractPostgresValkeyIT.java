package backend.academy.linktracker.scrapper.integration;

import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

public abstract class AbstractPostgresValkeyIT extends AbstractPostgresIT {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> VALKEY_CONTAINER = TestcontainersConfiguration.VALKEY_CONTAINER;

    @DynamicPropertySource
    static void registerValkeyProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", TestcontainersConfiguration::valkeyHost);
        registry.add("spring.data.redis.port", TestcontainersConfiguration::valkeyPort);
        registry.add("app.valkey.cache.enabled", () -> true);
    }
}
