package com.karen.contracts.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single entry in the {@code schemas/catalog.json} manifest.
 *
 * <p>Acts as the lookup key {@code (commandId, version)} and carries the classpath-relative
 * {@code path} to the JSON Schema file.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaRef {

    /** Runtime command identifier (e.g. {@code "TOGGLE_LOCK"}). */
    private String commandId;

    /** Schema version. {@link Integer} for null-safe Jackson deserialization. */
    private Integer version;

    /** Feature category (e.g. {@code "switch"}, {@code "sensor"}). */
    private String feature;

    /** Device subtype (e.g. {@code "relay"}, {@code "temperature"}). */
    private String subtype;

    /**
     * Classpath-relative path to the JSON Schema file
     * (e.g. {@code "schemas/switch/relay/TOGGLE_LOCK.v1.json"}).
     */
    private String path;
}
