package backend.academy.linktracker.scrapper.service.codec;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaNotificationsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.springframework.stereotype.Component;

@Component
public class LinkUpdateAvroCodec {

    private static final String SCHEMA_PATH = "avro/LinkUpdateEvent.avsc";
    private static final byte MAGIC_BYTE = 0;

    private final Schema schema;
    private final KafkaNotificationsProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<Integer, Schema> schemaCacheById;

    private volatile Integer cachedSchemaId;

    public LinkUpdateAvroCodec(KafkaNotificationsProperties properties) {
        this.properties = properties;
        this.schema = loadSchema();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.objectMapper = JsonMapper.builder().findAndAddModules().build();
        this.schemaCacheById = new ConcurrentHashMap<>();
    }

    public String encodeToBase64(LinkUpdate update) {
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", update.getId());
        record.put("url", String.valueOf(update.getUrl()));
        record.put("description", update.getDescription());
        record.put("tgChatIds", update.getTgChatIds() == null ? List.of() : update.getTgChatIds());

        int schemaId = resolveSchemaId();
        byte[] avroPayload = serializeAvro(record, schema);
        byte[] confluentWire = toConfluentWireFormat(schemaId, avroPayload);
        return Base64.getEncoder().encodeToString(confluentWire);
    }

    public LinkUpdate decodeFromBase64(String payload) {
        byte[] bytes = Base64.getDecoder().decode(payload);
        if (bytes.length < 5 || bytes[0] != MAGIC_BYTE) {
            throw new IllegalArgumentException("Invalid Confluent wire format payload");
        }

        int schemaId = ByteBuffer.wrap(bytes, 1, 4).getInt();
        Schema writerSchema = schemaCacheById.computeIfAbsent(schemaId, this::fetchSchemaById);

        byte[] avroPayload = new byte[bytes.length - 5];
        System.arraycopy(bytes, 5, avroPayload, 0, avroPayload.length);

        GenericRecord record = deserializeAvro(avroPayload, writerSchema);

        List<Long> chatIds = new ArrayList<>();
        List<?> rawChatIds = (List<?>) record.get("tgChatIds");
        for (Object raw : rawChatIds) {
            chatIds.add((Long) raw);
        }

        return new LinkUpdate()
                .id((Long) record.get("id"))
                .url(URI.create(String.valueOf(record.get("url"))))
                .description(String.valueOf(record.get("description")))
                .tgChatIds(chatIds);
    }

    private int resolveSchemaId() {
        if (cachedSchemaId != null) {
            return cachedSchemaId;
        }

        synchronized (this) {
            if (cachedSchemaId != null) {
                return cachedSchemaId;
            }

            try {
                String subject = properties.getUpdatesTopic() + "-value";
                String schemaJson = objectMapper.writeValueAsString(Map.of("schema", schema.toString()));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(properties.getSchemaRegistryUrl() + "/subjects/" + subject + "/versions"))
                        .header("Content-Type", "application/vnd.schemaregistry.v1+json")
                        .POST(HttpRequest.BodyPublishers.ofString(schemaJson))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 300) {
                    throw new IllegalStateException("Schema registry responded with status " + response.statusCode());
                }

                JsonNode json = objectMapper.readTree(response.body());
                cachedSchemaId = json.get("id").asInt();
                schemaCacheById.put(cachedSchemaId, schema);
                return cachedSchemaId;
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Failed to register Avro schema in Schema Registry", e);
            }
        }
    }

    private Schema fetchSchemaById(int id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getSchemaRegistryUrl() + "/schemas/ids/" + id))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Schema registry responded with status " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return new Schema.Parser().parse(json.get("schema").asText());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch schema by id from Schema Registry", e);
        }
    }

    private byte[] serializeAvro(GenericRecord record, Schema writerSchema) {
        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(writerSchema);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            writer.write(record, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to encode LinkUpdate to Avro", e);
        }
    }

    private GenericRecord deserializeAvro(byte[] payload, Schema writerSchema) {
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(writerSchema);
        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(payload, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode Avro payload", e);
        }
    }

    private byte[] toConfluentWireFormat(int schemaId, byte[] payload) {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + payload.length);
        buffer.put(MAGIC_BYTE);
        buffer.putInt(schemaId);
        buffer.put(payload);
        return buffer.array();
    }

    private Schema loadSchema() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SCHEMA_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Avro schema not found: " + SCHEMA_PATH);
            }
            return new Schema.Parser().parse(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Avro schema", e);
        }
    }
}
