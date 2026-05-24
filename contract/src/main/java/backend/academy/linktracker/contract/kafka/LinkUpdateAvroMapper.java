package backend.academy.linktracker.contract.kafka;

import backend.academy.linktracker.bot.generated.dto.LinkUpdate;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

public final class LinkUpdateAvroMapper {

    private static final String SCHEMA_PATH = "avro/LinkUpdateEvent.avsc";

    private final Schema schema;

    public LinkUpdateAvroMapper() {
        this.schema = loadSchema();
    }

    public GenericRecord toRecord(LinkUpdate update) {
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", update.getId());
        record.put("url", String.valueOf(update.getUrl()));
        record.put("description", update.getDescription());
        record.put("tgChatIds", update.getTgChatIds() == null ? List.of() : update.getTgChatIds());
        record.put("author", update.getAuthor());
        record.put("priority", update.getPriority());
        return record;
    }

    public LinkUpdate fromRecord(GenericRecord record) {
        return new LinkUpdate()
                .id((Long) record.get("id"))
                .url(URI.create(String.valueOf(record.get("url"))))
                .description(String.valueOf(record.get("description")))
                .tgChatIds(chatIds(record.get("tgChatIds")))
                .author(nullableString(record.get("author")))
                .priority(nullableString(record.get("priority")));
    }

    public Schema schema() {
        return schema;
    }

    private List<Long> chatIds(Object rawValue) {
        if (!(rawValue instanceof Iterable<?> rawChatIds)) {
            return List.of();
        }

        List<Long> chatIds = new ArrayList<>();
        for (Object raw : rawChatIds) {
            if (raw instanceof Number number) {
                chatIds.add(number.longValue());
            }
        }
        return chatIds;
    }

    private String nullableString(Object rawValue) {
        return rawValue == null ? null : String.valueOf(rawValue);
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
