package backend.academy.linktracker.bot.support;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public final class KafkaTestContainerHolder {

    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

    static {
        KAFKA.start();
    }

    private KafkaTestContainerHolder() {}

    public static KafkaContainer kafka() {
        return KAFKA;
    }
}
