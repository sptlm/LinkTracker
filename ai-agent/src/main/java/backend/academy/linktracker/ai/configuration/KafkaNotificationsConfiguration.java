package backend.academy.linktracker.ai.configuration;

import backend.academy.linktracker.ai.properties.KafkaNotificationsProperties;
import backend.academy.linktracker.contract.kafka.LinkUpdateAvroMapper;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaNotificationsConfiguration {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(KafkaNotificationsProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getGroupId());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, properties.getKeyDeserializer());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, properties.getValueDeserializer());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        putSchemaRegistryConfig(config, properties);
        return new DefaultKafkaConsumerFactory<>(config);
    }

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
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public KafkaAdmin kafkaAdmin(KafkaNotificationsProperties properties) {
        return new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers()));
    }

    @Bean
    public NewTopic rawUpdatesTopic(KafkaNotificationsProperties properties) {
        return new NewTopic(
                properties.getRawUpdatesTopic(),
                properties.getTopicsPartitions(),
                properties.getTopicsReplicationFactor());
    }

    @Bean
    public NewTopic processedUpdatesTopic(KafkaNotificationsProperties properties) {
        return new NewTopic(
                properties.getProcessedUpdatesTopic(),
                properties.getTopicsPartitions(),
                properties.getTopicsReplicationFactor());
    }

    @Bean
    @ConditionalOnMissingBean(LinkUpdateAvroMapper.class)
    public LinkUpdateAvroMapper linkUpdateAvroMapper() {
        return new LinkUpdateAvroMapper();
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
