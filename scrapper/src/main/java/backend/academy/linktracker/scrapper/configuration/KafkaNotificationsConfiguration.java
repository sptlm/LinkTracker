package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.contract.kafka.LinkUpdateAvroCodec;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration("scrapperKafkaNotificationsConfiguration")
@ConditionalOnProperty(prefix = "app.notifications", name = "transport", havingValue = "KAFKA", matchIfMissing = true)
public class KafkaNotificationsConfiguration {

    @Bean
    public ProducerFactory<String, String> producerFactory(KafkaNotificationsProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaNotificationsProperties properties) {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers()));
    }

    @Bean
    public LinkUpdateAvroCodec linkUpdateAvroCodec(KafkaNotificationsProperties properties) {
        return new LinkUpdateAvroCodec(properties.getSchemaRegistryUrl(), properties.getUpdatesTopic());
    }

    @Bean
    public NewTopic linkUpdatesTopic(KafkaNotificationsProperties properties) {
        return new NewTopic(
                        properties.getUpdatesTopic(),
                        properties.getUpdatesTopicPartitions(),
                        properties.getUpdatesTopicReplicationFactor())
                .configs(Map.of(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2"));
    }
}
