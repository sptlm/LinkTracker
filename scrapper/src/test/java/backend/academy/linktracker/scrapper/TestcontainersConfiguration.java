package backend.academy.linktracker.scrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final Network NETWORK = Network.newNetwork();
    public static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>(
                    DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("linktracker")
            .withUsername("postgres")
            .withPassword("postgres")
            .withNetwork(NETWORK)
            .withNetworkAliases("postgres");
    public static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
                    DockerImageName.parse("apache/kafka-native:4.1.1"))
            .withNetwork(NETWORK)
            .withNetworkAliases("kafka");
    public static final GenericContainer<?> VALKEY_CONTAINER = new GenericContainer<>(
                    DockerImageName.parse("valkey/valkey:8.0-alpine"))
            .withNetwork(NETWORK)
            .withNetworkAliases("valkey")
            .withExposedPorts(6379);

    @SuppressWarnings({"deprecation", "rawtypes", "resource"})
    public static final FixedHostPortGenericContainer VALKEY_CLUSTER_NODE_1 =
            valkeyClusterNode("valkey-cluster-1", 6390, 16390);

    @SuppressWarnings({"deprecation", "rawtypes", "resource"})
    public static final FixedHostPortGenericContainer VALKEY_CLUSTER_NODE_2 =
            valkeyClusterNode("valkey-cluster-2", 6391, 16391);

    @SuppressWarnings({"deprecation", "rawtypes", "resource"})
    public static final FixedHostPortGenericContainer VALKEY_CLUSTER_NODE_3 =
            valkeyClusterNode("valkey-cluster-3", 6392, 16392);

    private static Path findJar(String moduleName) {
        String basePath = new File(moduleName).exists() ? "./" : "../";
        File targetDir = new File(basePath + moduleName + "/target/");
        File[] jars = targetDir.listFiles((dir, name) -> name.endsWith(".jar")
                && !name.endsWith("-plain.jar")
                && !name.endsWith("-javadoc.jar")
                && !name.endsWith("-sources.jar"));
        if (jars == null || jars.length == 0) {
            throw new RuntimeException(
                    "JAR file for " + moduleName + " not found. Run 'mvn clean package -DskipTests'");
        }
        return jars[0].toPath();
    }

    @SuppressWarnings({"deprecation", "rawtypes"})
    private static FixedHostPortGenericContainer valkeyClusterNode(String alias, int hostPort, int hostClusterBusPort) {
        return (FixedHostPortGenericContainer) new FixedHostPortGenericContainer(
                        DockerImageName.parse("valkey/valkey:8.0-alpine").asCanonicalNameString())
                .withFixedExposedPort(hostPort, 6379)
                .withFixedExposedPort(hostClusterBusPort, 16379)
                .withNetwork(NETWORK)
                .withNetworkAliases(alias)
                .withExtraHost("host.docker.internal", "host-gateway")
                .withCommand(
                        "valkey-server",
                        "--port",
                        "6379",
                        "--cluster-enabled",
                        "yes",
                        "--cluster-config-file",
                        "nodes.conf",
                        "--cluster-node-timeout",
                        "5000",
                        "--appendonly",
                        "no",
                        "--cluster-announce-hostname",
                        "host.docker.internal",
                        "--cluster-announce-port",
                        String.valueOf(hostPort),
                        "--cluster-announce-bus-port",
                        String.valueOf(hostClusterBusPort));
    }

    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        return POSTGRESQL_CONTAINER;
    }

    @Bean
    public KafkaContainer kafkaContainer() {
        return KAFKA_CONTAINER;
    }

    @Bean
    public GenericContainer<?> valkeyContainer() {
        return VALKEY_CONTAINER;
    }

    public static String kafkaBootstrapServers() {
        if (!KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.start();
        }
        return KAFKA_CONTAINER.getBootstrapServers();
    }

    public static String postgresJdbcUrl() {
        if (!POSTGRESQL_CONTAINER.isRunning()) {
            POSTGRESQL_CONTAINER.start();
        }
        return POSTGRESQL_CONTAINER.getJdbcUrl();
    }

    public static String postgresUsername() {
        if (!POSTGRESQL_CONTAINER.isRunning()) {
            POSTGRESQL_CONTAINER.start();
        }
        return POSTGRESQL_CONTAINER.getUsername();
    }

    public static String postgresPassword() {
        if (!POSTGRESQL_CONTAINER.isRunning()) {
            POSTGRESQL_CONTAINER.start();
        }
        return POSTGRESQL_CONTAINER.getPassword();
    }

    public static String valkeyHost() {
        if (!VALKEY_CONTAINER.isRunning()) {
            VALKEY_CONTAINER.start();
        }
        return VALKEY_CONTAINER.getHost();
    }

    public static Integer valkeyPort() {
        if (!VALKEY_CONTAINER.isRunning()) {
            VALKEY_CONTAINER.start();
        }
        return VALKEY_CONTAINER.getMappedPort(6379);
    }

    public static void startValkeyCluster() {
        Stream.of(VALKEY_CLUSTER_NODE_1, VALKEY_CLUSTER_NODE_2, VALKEY_CLUSTER_NODE_3)
                .parallel()
                .forEach(container -> {
                    if (!container.isRunning()) {
                        container.start();
                    }
                });

        try {
            Container.ExecResult clusterInfo = VALKEY_CLUSTER_NODE_1.execInContainer(
                    "valkey-cli", "-h", "host.docker.internal", "-p", "6390", "cluster", "info");
            if (clusterInfo.getStdout().contains("cluster_state:ok")) {
                return;
            }

            Stream.of("6390", "6391", "6392").forEach(port -> {
                try {
                    Container.ExecResult resetResult = VALKEY_CLUSTER_NODE_1.execInContainer(
                            "valkey-cli", "-h", "host.docker.internal", "-p", port, "cluster", "reset", "hard");
                    if (resetResult.getExitCode() != 0) {
                        throw new IllegalStateException("Failed to reset Valkey test node " + port + ": "
                                + resetResult.getStdout() + resetResult.getStderr());
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to reset Valkey test node " + port, e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while resetting Valkey test node " + port, e);
                }
            });

            Container.ExecResult createResult = VALKEY_CLUSTER_NODE_1.execInContainer(
                    "valkey-cli",
                    "--cluster",
                    "create",
                    "host.docker.internal:6390",
                    "host.docker.internal:6391",
                    "host.docker.internal:6392",
                    "--cluster-replicas",
                    "0",
                    "--cluster-yes");
            if (createResult.getExitCode() != 0) {
                throw new IllegalStateException(
                        "Failed to create Valkey test cluster: " + createResult.getStdout() + createResult.getStderr());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize Valkey test cluster", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while initializing Valkey test cluster", e);
        }
    }

    public static String valkeyClusterNodes() {
        startValkeyCluster();
        return "host.docker.internal:6390,host.docker.internal:6391,host.docker.internal:6392";
    }

    @Bean
    public GenericContainer<?> scrapperContainer(
            PostgreSQLContainer<?> postgresContainer, GenericContainer<?> valkeyContainer) {
        return new GenericContainer<>(new ImageFromDockerfile()
                        .withFileFromPath("app.jar", findJar("scrapper"))
                        .withDockerfileFromBuilder(builder -> builder.from("openjdk:25-ea-slim")
                                .copy("app.jar", "/app.jar")
                                .entryPoint("java", "--enable-preview", "-jar", "/app.jar")
                                .build()))
                .dependsOn(postgresContainer)
                .dependsOn(valkeyContainer)
                .withNetwork(NETWORK)
                .withNetworkAliases("scrapper")
                .withExposedPorts(8081)
                .withEnv("SPRING_PROFILES_ACTIVE", "test")
                .withEnv("SCRAPPER_NOTIFICATION_TRANSPORT", "HTTP")
                .withEnv("SCRAPPER_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/linktracker")
                .withEnv("SCRAPPER_DATASOURCE_USERNAME", "postgres")
                .withEnv("SCRAPPER_DATASOURCE_PASSWORD", "postgres")
                .withEnv("SCRAPPER_VALKEY_HOST", "valkey")
                .withEnv("SCRAPPER_VALKEY_PORT", "6379")
                .withEnv("GITHUB_TOKEN", "mock")
                .withEnv("STACKOVERFLOW_KEY", "mock")
                .withEnv("STACKOVERFLOW_ACCESS_KEY", "mock");
    }

    @Bean
    public GenericContainer<?> botContainer() {
        return new GenericContainer<>(new ImageFromDockerfile()
                        .withFileFromPath("app.jar", findJar("bot"))
                        .withDockerfileFromBuilder(builder -> builder.from("openjdk:25-ea-slim")
                                .copy("app.jar", "/app.jar")
                                .entryPoint("java", "--enable-preview", "-jar", "/app.jar")
                                .build()))
                .withNetwork(NETWORK)
                .withNetworkAliases("bot")
                .withExposedPorts(8080)
                .withEnv("BOT_NOTIFICATION_TRANSPORT", "HTTP")
                .withEnv("APP_SCRAPPER_BASE_URL", "http://scrapper:8081")
                .withEnv("TELEGRAM_TOKEN", "mock_token");
    }
}
