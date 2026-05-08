package backend.academy.linktracker.scrapper;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractKafkaIntegrationTest {

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.bootstrap-servers", TestcontainersConfiguration::kafkaBootstrapServers);
    }

    protected String kafkaBootstrapServers() {
        return TestcontainersConfiguration.kafkaBootstrapServers();
    }
}
