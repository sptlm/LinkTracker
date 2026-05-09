package backend.academy.linktracker.bot;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final KafkaContainer KAFKA_CONTAINER =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

    private static final GenericContainer<?> SCHEMA_REGISTRY = new GenericContainer<>(
                    DockerImageName.parse("confluentinc/cp-schema-registry:7.7.1"))
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081");

    public static String kafkaBootstrapServers() {
        if (!KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.start();
        }
        return KAFKA_CONTAINER.getBootstrapServers();
    }

    public static String schemaRegistryUrl() {
        if (!KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.start();
        }
        if (!SCHEMA_REGISTRY.isRunning()) {
            Testcontainers.exposeHostPorts(KAFKA_CONTAINER.getMappedPort(9092));
            SCHEMA_REGISTRY
                    .withEnv(
                            "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                            "PLAINTEXT://host.testcontainers.internal:" + KAFKA_CONTAINER.getMappedPort(9092))
                    .start();
        }
        return "http://localhost:" + SCHEMA_REGISTRY.getMappedPort(8081);
    }

    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        return KAFKA_CONTAINER;
    }
}
