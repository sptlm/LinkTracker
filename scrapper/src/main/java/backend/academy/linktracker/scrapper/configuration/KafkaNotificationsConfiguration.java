package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.contract.kafka.LinkUpdateAvroMapper;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration("scrapperKafkaNotificationsConfiguration")
@ConditionalOnExpression(
        "'${app.notifications.transport:KAFKA}' == 'KAFKA' || '${app.kafka.outbox-dispatch-enabled:true}' == 'true'")
public class KafkaNotificationsConfiguration {

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaNotificationsProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, properties.getKeySerializer());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, properties.getValueSerializer());
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        putSchemaRegistryConfig(config, properties);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaNotificationsProperties properties) {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers()));
    }

    @Bean
    @ConditionalOnMissingBean(LinkUpdateAvroMapper.class)
    public LinkUpdateAvroMapper scrapperLinkUpdateAvroMapper() {
        return new LinkUpdateAvroMapper();
    }

    @Bean
    public NewTopic linkUpdatesTopic(KafkaNotificationsProperties properties) {
        return new NewTopic(
                        properties.getUpdatesTopic(),
                        properties.getUpdatesTopicPartitions(),
                        properties.getUpdatesTopicReplicationFactor())
                .configs(Map.of(
                        TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
                        String.valueOf(properties.getUpdatesTopicMinInSyncReplicas())));
    }

    private static void putSchemaRegistryConfig(Map<String, Object> config, KafkaNotificationsProperties properties) {
        if (properties.usesSchemaRegistry()) {
            if (properties.getSchemaRegistryUrl() == null
                    || properties.getSchemaRegistryUrl().isBlank()) {
                throw new IllegalStateException(
                        "app.kafka.schema-registry-url must be set when Avro serializer/deserializer is configured");
            }
            config.put("schema.registry.url", properties.getSchemaRegistryUrl());
        }
    }
}
