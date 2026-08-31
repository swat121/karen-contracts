package com.karen.contracts.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The catalog is fail-fast: a manifest entry pointing at a missing or unparseable file throws on
 * construction, which would break every consumer at startup rather than at first use. These tests
 * exercise that path so a typo in {@code catalog.json} is caught here instead of in production.
 */
class SchemaCatalogTest {

    private final SchemaCatalog catalog = new SchemaCatalog();

    @Test
    void loadsEverySchemaReferencedByTheManifest() {
        assertNotNull(catalog.getSchema("TOGGLE_LOCK", 1));
        assertNotNull(catalog.getSchema("FORCE_SWITCH_STATE", 1));
        assertNotNull(catalog.getSchema("FORSE_SWITCH_STATE", 1));
        assertNotNull(catalog.getSchema("READ", 1));
        assertNotNull(catalog.getSchema("READ_ALL", 1));
    }

    @Test
    void bothSpellingsOfTheForcedStateCommandDescribeTheSameContract() {
        JsonNode corrected = catalog.getSchema("FORCE_SWITCH_STATE", 1);
        JsonNode original = catalog.getSchema("FORSE_SWITCH_STATE", 1);

        assertEquals(original.get("required"), corrected.get("required"));
        assertEquals(original.get("properties").get("state").get("enum"),
                corrected.get("properties").get("state").get("enum"));
        assertEquals("switch", catalog.getFeature("FORCE_SWITCH_STATE", 1));
        assertEquals("switch", catalog.getFeature("FORSE_SWITCH_STATE", 1));
    }

    @Test
    void switchStateIsLowerCaseOnly() {
        JsonNode states = catalog.getSchema("FORCE_SWITCH_STATE", 1)
                .get("properties").get("state").get("enum");

        assertEquals(2, states.size());
        assertEquals("on", states.get(0).asText());
        assertEquals("off", states.get(1).asText());
    }

    @Test
    void rejectsAnUnknownCommandInsteadOfReturningNull() {
        assertThrows(NoSuchElementException.class, () -> catalog.getSchema("NO_SUCH_COMMAND", 1));
    }
}
