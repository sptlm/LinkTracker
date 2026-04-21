package backend.academy.linktracker.scrapper.service.codec;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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

    private final Schema schema;

    public LinkUpdateAvroCodec() {
        this.schema = loadSchema();
    }

    public String encodeToBase64(LinkUpdate update) {
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", update.getId());
        record.put("url", String.valueOf(update.getUrl()));
        record.put("description", update.getDescription());
        record.put("tgChatIds", update.getTgChatIds() == null ? List.of() : update.getTgChatIds());

        GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            writer.write(record, encoder);
            encoder.flush();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to encode LinkUpdate to Avro", e);
        }
    }

    public LinkUpdate decodeFromBase64(String payload) {
        byte[] bytes = Base64.getDecoder().decode(payload);
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);

        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
            GenericRecord record = reader.read(null, decoder);

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
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode Avro payload", e);
        }
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
