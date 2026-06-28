package com.karen.contracts.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

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
 * SchemaCatalog catalog = SchemaCatalog.getDefault();
 * JsonNode schema = catalog.getSchema("TOGGLE_LOCK", 1);
 * }</pre>
 *
 * <p>No JSON-Schema validation engine is bundled. Consumers bring their own validator
 * and feed it the {@link JsonNode} or raw {@link String} returned by this class.
 */
public class SchemaCatalog {

    private static final String CATALOG_PATH = "schemas/catalog.json";

    /** Lazy-initialised default instance. */
    private static volatile SchemaCatalog defaultInstance;

    private final Map<String, SchemaRef> refIndex;
    private final Map<String, JsonNode>  schemaIndex;
    private final Map<String, String>    rawIndex;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * Creates a new {@code SchemaCatalog} by loading {@code schemas/catalog.json}
     * from the current thread's context classloader (falls back to this class's classloader).
     *
     * @throws IllegalStateException if the catalog or any referenced schema file is
     *                               missing or cannot be parsed
     */
    public SchemaCatalog() {
        this(resolveClassLoader());
    }

    /**
     * Creates a new {@code SchemaCatalog} using the supplied classloader.
     * Useful for tests or OSGi-style environments where context classloader differs.
     */
    public SchemaCatalog(ClassLoader classLoader) {
        ObjectMapper mapper = new ObjectMapper();
        List<SchemaRef> refs = loadCatalog(classLoader, mapper);

        Map<String, SchemaRef> refMap    = new LinkedHashMap<>();
        Map<String, JsonNode>  schemaMap = new LinkedHashMap<>();
        Map<String, String>    rawMap    = new LinkedHashMap<>();

        for (SchemaRef ref : refs) {
            if (ref.getVersion() == null) {
                throw new IllegalStateException(
                        "catalog.json entry for commandId='" + ref.getCommandId() + "' has null version");
            }
            String key = key(ref.getCommandId(), ref.getVersion());
            refMap.put(key, ref);

            String raw = loadRaw(classLoader, ref);
            rawMap.put(key, raw);

            JsonNode node;
            try {
                node = mapper.readTree(raw);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Failed to parse JSON Schema at '" + ref.getPath() + "': " + e.getMessage(), e);
            }
            schemaMap.put(key, node);
        }

        this.refIndex    = Collections.unmodifiableMap(refMap);
        this.schemaIndex = Collections.unmodifiableMap(schemaMap);
        this.rawIndex    = Collections.unmodifiableMap(rawMap);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

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
     * Returns an {@link Optional} containing the JSON Schema, or empty if not found.
     */
    public Optional<JsonNode> findSchema(String commandId, int version) {
        return Optional.ofNullable(schemaIndex.get(key(commandId, version)));
    }

    /**
     * Returns the raw JSON Schema string for the given {@code (commandId, version)} key.
     *
     * @throws NoSuchElementException if no schema is registered for this key
     */
    public String getRawSchema(String commandId, int version) {
        String raw = rawIndex.get(key(commandId, version));
        if (raw == null) {
            throw new NoSuchElementException(
                    "No schema registered for commandId='" + commandId + "', version=" + version);
        }
        return raw;
    }

    /**
     * Returns all registered {@link SchemaRef} entries in catalog order.
     */
    public Collection<SchemaRef> listRefs() {
        return refIndex.values();
    }

    // -------------------------------------------------------------------------
    // Static default
    // -------------------------------------------------------------------------

    /**
     * Returns a shared, lazily-initialised {@code SchemaCatalog} instance.
     * Equivalent to {@code new SchemaCatalog()} but reuses the same object on
     * repeated calls. Safe for concurrent use (double-checked locking).
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
