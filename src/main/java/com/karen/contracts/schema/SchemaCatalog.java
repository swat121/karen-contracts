package com.karen.contracts.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Classpath-based accessor for the device command JSON Schemas bundled in this jar.
 *
 * <p>On construction the catalog reads {@code schemas/catalog.json} from the classpath,
 * parses every referenced schema file, and indexes the results by {@code (commandId, version)}.
 * Construction is fail-fast: a missing or unparseable catalog / schema file throws
 * {@link IllegalStateException} immediately.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * JsonNode schema = SchemaCatalog.getDefault().getSchema("TOGGLE_LOCK", 1);
 * }</pre>
 *
 * <p>No JSON-Schema validation engine is bundled. Consumers bring their own validator
 * and feed it the {@link JsonNode} returned by this class.
 */
public class SchemaCatalog {

    private static final String CATALOG_PATH = "schemas/catalog.json";

    /** Lazy-initialised shared instance. */
    private static volatile SchemaCatalog defaultInstance;

    private final Map<String, JsonNode> schemaIndex;
    private final Map<String, String>   featureIndex;

    /**
     * Loads {@code schemas/catalog.json} and every referenced schema from the classpath.
     *
     * @throws IllegalStateException if the catalog or any referenced schema file is
     *                               missing or cannot be parsed
     */
    public SchemaCatalog() {
        ClassLoader classLoader = resolveClassLoader();
        ObjectMapper mapper = new ObjectMapper();
        List<SchemaRef> refs = loadCatalog(classLoader, mapper);

        Map<String, JsonNode> schemas  = new LinkedHashMap<>();
        Map<String, String>   features = new LinkedHashMap<>();
        for (SchemaRef ref : refs) {
            if (ref.getVersion() == null) {
                throw new IllegalStateException(
                        "catalog.json entry for commandId='" + ref.getCommandId() + "' has null version");
            }
            if (ref.getFeature() == null) {
                throw new IllegalStateException(
                        "catalog.json entry for commandId='" + ref.getCommandId() + "' has null feature");
            }
            JsonNode node;
            try {
                node = mapper.readTree(loadRaw(classLoader, ref));
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to parse JSON Schema at '" + ref.getPath() + "': " + e.getMessage(), e);
            }
            String k = key(ref.getCommandId(), ref.getVersion());
            schemas.put(k, node);
            features.put(k, ref.getFeature());
        }
        this.schemaIndex  = Map.copyOf(schemas);
        this.featureIndex = Map.copyOf(features);
    }

    /**
     * Returns the JSON Schema {@link JsonNode} for the given {@code (commandId, version)} key.
     *
     * @throws NoSuchElementException if no schema is registered for this key
     */
    public JsonNode getSchema(String commandId, int version) {
        JsonNode node = schemaIndex.get(key(commandId, version));
        if (node == null) {
            throw new NoSuchElementException(
                    "No schema registered for commandId='" + commandId + "', version=" + version);
        }
        return node;
    }

    /**
     * Returns the feature group (e.g. {@code "switch"}, {@code "sensor"}) that the command
     * belongs to, as declared in the catalog manifest. Callers use this to route a command
     * to the right device feature without carrying the feature type through the UI.
     *
     * @throws NoSuchElementException if no schema is registered for this key
     */
    public String getFeature(String commandId, int version) {
        String feature = featureIndex.get(key(commandId, version));
        if (feature == null) {
            throw new NoSuchElementException(
                    "No schema registered for commandId='" + commandId + "', version=" + version);
        }
        return feature;
    }

    /**
     * Returns a shared, lazily-initialised {@code SchemaCatalog} instance.
     * Safe for concurrent use (double-checked locking).
     */
    public static SchemaCatalog getDefault() {
        if (defaultInstance == null) {
            synchronized (SchemaCatalog.class) {
                if (defaultInstance == null) {
                    defaultInstance = new SchemaCatalog();
                }
            }
        }
        return defaultInstance;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static List<SchemaRef> loadCatalog(ClassLoader cl, ObjectMapper mapper) {
        try (InputStream is = cl.getResourceAsStream(CATALOG_PATH)) {
            if (is == null) {
                throw new IllegalStateException(
                        "schemas/catalog.json not found on classpath. "
                        + "Ensure karen-contracts jar is on the classpath.");
            }
            return mapper.readValue(is, new TypeReference<List<SchemaRef>>() {});
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to parse schemas/catalog.json: " + e.getMessage(), e);
        }
    }

    private static String loadRaw(ClassLoader cl, SchemaRef ref) {
        try (InputStream is = cl.getResourceAsStream(ref.getPath())) {
            if (is == null) {
                throw new IllegalStateException(
                        "Schema file not found on classpath: '" + ref.getPath()
                        + "' (referenced from catalog for commandId='"
                        + ref.getCommandId() + "', version=" + ref.getVersion() + ")");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read schema file '" + ref.getPath() + "': " + e.getMessage(), e);
        }
    }

    private static String key(String commandId, int version) {
        return Objects.requireNonNull(commandId, "commandId must not be null") + "|" + version;
    }

    private static ClassLoader resolveClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl != null ? cl : SchemaCatalog.class.getClassLoader();
    }
}
