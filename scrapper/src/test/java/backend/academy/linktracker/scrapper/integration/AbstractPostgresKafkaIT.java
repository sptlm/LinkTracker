package backend.academy.linktracker.scrapper.integration;

import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class AbstractPostgresKafkaIT extends AbstractPostgresIT {

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("app.kafka.bootstrap-servers", TestcontainersConfiguration::kafkaBootstrapServers);
    }
}
