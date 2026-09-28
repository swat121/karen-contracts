package com.karen.contracts.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the wire format of {@code karen.sensor.data}: exact JSON field names and
 * unknown-field tolerance on the way in, same reasoning as the automation contracts tests.
 */
class SensorDataEventSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void roundTripsAllFields() throws Exception {
        SensorDataEvent event = SensorDataEvent.builder()
                .deviceId("patrick-001")
                .macAddress("F0:F5:BD:00:11:22")
                .sensorAddress("28FF641E9C160423")
                .type("TEMPERATURE")
                .unit("celsius")
                .value(21.5)
                .measuredAt(1_757_000_000_000L)
                .requestId("req-123")
                .build();

        String json = mapper.writeValueAsString(event);
        SensorDataEvent roundTripped = mapper.readValue(json, SensorDataEvent.class);

        assertEquals(event, roundTripped);
    }

    @Test
    void serializesExactJsonFieldNames() throws Exception {
        SensorDataEvent event = SensorDataEvent.builder()
                .deviceId("patrick-001")
                .macAddress("F0:F5:BD:00:11:22")
                .sensorAddress("28FF641E9C160423")
                .type("TEMPERATURE")
                .unit("celsius")
                .value(21.5)
                .measuredAt(1_757_000_000_000L)
                .requestId("req-123")
                .build();

        JsonNode node = mapper.readTree(mapper.writeValueAsString(event));

        assertTrue(node.has("deviceId"));
        assertTrue(node.has("macAddress"));
        assertTrue(node.has("sensorAddress"));
        assertTrue(node.has("type"));
        assertTrue(node.has("unit"));
        assertTrue(node.has("value"));
        assertTrue(node.has("measuredAt"));
        assertTrue(node.has("requestId"));

        assertEquals("TEMPERATURE", node.get("type").asText());
        assertEquals(21.5, node.get("value").asDouble());
        assertEquals(1_757_000_000_000L, node.get("measuredAt").asLong());
    }

    @Test
    void unknownFieldIsIgnoredOnDeserialization() throws Exception {
        String json = "{"
                + "\"deviceId\":\"patrick-001\","
                + "\"type\":\"TEMPERATURE\","
                + "\"value\":21.5,"
                + "\"futureField\":\"from a newer producer\""
                + "}";

        SensorDataEvent event = mapper.readValue(json, SensorDataEvent.class);

        assertEquals("patrick-001", event.getDeviceId());
        assertEquals("TEMPERATURE", event.getType());
        assertEquals(21.5, event.getValue());
    }
}
