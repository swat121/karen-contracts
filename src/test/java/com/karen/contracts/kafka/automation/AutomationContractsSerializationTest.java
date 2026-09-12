package com.karen.contracts.kafka.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the Controller <-> adapter wire format (design doc section 23): PascalCase enum wire
 * values, conditional fields physically absent when {@code null}, and unknown-field tolerance on
 * the way in. A typo in a {@code @JsonProperty} or an {@code @JsonInclude} slipping off a field
 * would otherwise only surface as a cross-service incompatibility, the same reasoning as
 * {@code ExecutionRequestDtoSerializationTest} in karen-chain-builder.
 */
class AutomationContractsSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void commandTypeSerializesToPascalCaseWireValues() throws Exception {
        assertEquals("\"StartAdapterExecution\"",
                mapper.writeValueAsString(AdapterCommandType.START_ADAPTER_EXECUTION));
        assertEquals("\"TerminateAdapterExecution\"",
                mapper.writeValueAsString(AdapterCommandType.TERMINATE_ADAPTER_EXECUTION));
    }

    @Test
    void eventTypeSerializesToPascalCaseWireValues() throws Exception {
        assertEquals("\"AdapterExecutionStarted\"",
                mapper.writeValueAsString(AdapterEventType.ADAPTER_EXECUTION_STARTED));
        assertEquals("\"AdapterExecutionCompleted\"",
                mapper.writeValueAsString(AdapterEventType.ADAPTER_EXECUTION_COMPLETED));
        assertEquals("\"AdapterExecutionFailed\"",
                mapper.writeValueAsString(AdapterEventType.ADAPTER_EXECUTION_FAILED));
        assertEquals("\"AdapterExecutionTerminated\"",
                mapper.writeValueAsString(AdapterEventType.ADAPTER_EXECUTION_TERMINATED));
    }

    @Test
    void errorCodeReadsFromUpperSnakeCaseStrings() throws Exception {
        assertEquals(AdapterErrorCode.DEVICE_UNAVAILABLE,
                mapper.readValue("\"DEVICE_UNAVAILABLE\"", AdapterErrorCode.class));
        assertEquals(AdapterErrorCode.DEVICE_REJECTED,
                mapper.readValue("\"DEVICE_REJECTED\"", AdapterErrorCode.class));
        assertEquals(AdapterErrorCode.CONFIG_OUTDATED,
                mapper.readValue("\"CONFIG_OUTDATED\"", AdapterErrorCode.class));
        assertEquals(AdapterErrorCode.CONFIG_NOT_FOUND,
                mapper.readValue("\"CONFIG_NOT_FOUND\"", AdapterErrorCode.class));
        assertEquals(AdapterErrorCode.INTERNAL,
                mapper.readValue("\"INTERNAL\"", AdapterErrorCode.class));
    }

    @Test
    void startCommandRoundTripsAndCarriesConfigVersion() throws Exception {
        AdapterExecutionCommand start = AdapterExecutionCommand.builder()
                .commandType(AdapterCommandType.START_ADAPTER_EXECUTION)
                .executionId(UUID.randomUUID())
                .adapterId(UUID.randomUUID())
                .definitionKey("switch.relay")
                .adapterVersion(1L)
                .sentAt(1_757_000_000_000L)
                .configVersion(3L)
                .build();

        String json = mapper.writeValueAsString(start);
        JsonNode node = mapper.readTree(json);

        assertEquals("StartAdapterExecution", node.get("commandType").asText());
        assertTrue(node.has("configVersion"));
        assertEquals(3L, node.get("configVersion").asLong());

        AdapterExecutionCommand roundTripped = mapper.readValue(json, AdapterExecutionCommand.class);
        assertEquals(start, roundTripped);
    }

    @Test
    void terminateCommandOmitsConfigVersionWhenAbsent() throws Exception {
        AdapterExecutionCommand terminate = AdapterExecutionCommand.builder()
                .commandType(AdapterCommandType.TERMINATE_ADAPTER_EXECUTION)
                .executionId(UUID.randomUUID())
                .adapterId(UUID.randomUUID())
                .definitionKey("switch.relay")
                .adapterVersion(3L)
                .sentAt(1_757_000_000_000L)
                .build();

        JsonNode node = mapper.readTree(mapper.writeValueAsString(terminate));

        assertEquals("TerminateAdapterExecution", node.get("commandType").asText());
        assertFalse(node.has("configVersion"));
    }

    @Test
    void mandatoryFieldsStayInJsonAsNullWhenUnset() throws Exception {
        // NON_NULL belongs on the four conditional fields only. On the class it would also swallow
        // mandatory fields, making a producer bug look identical to a valid message on the wire.
        AdapterExecutionCommand incomplete = AdapterExecutionCommand.builder()
                .commandType(AdapterCommandType.START_ADAPTER_EXECUTION)
                .definitionKey("switch.relay")
                .build();

        JsonNode command = mapper.readTree(mapper.writeValueAsString(incomplete));

        assertTrue(command.has("executionId"));
        assertTrue(command.get("executionId").isNull());
        assertTrue(command.has("adapterId"));
        assertTrue(command.has("adapterVersion"));
        assertTrue(command.has("sentAt"));
        assertFalse(command.has("configVersion"));

        AdapterExecutionEvent partial = AdapterExecutionEvent.builder()
                .eventType(AdapterEventType.ADAPTER_EXECUTION_STARTED)
                .definitionKey("switch.relay")
                .build();

        JsonNode event = mapper.readTree(mapper.writeValueAsString(partial));

        assertTrue(event.has("executionId"));
        assertTrue(event.get("executionId").isNull());
        assertTrue(event.has("adapterVersion"));
        assertTrue(event.has("sentAt"));
        assertFalse(event.has("actionId"));
        assertFalse(event.has("errorCode"));
        assertFalse(event.has("errorMessage"));
    }

    @Test
    void completedEventRoundTripsAndCarriesActionIdButNotErrorFields() throws Exception {
        AdapterExecutionEvent completed = AdapterExecutionEvent.builder()
                .eventType(AdapterEventType.ADAPTER_EXECUTION_COMPLETED)
                .executionId(UUID.randomUUID())
                .adapterId(UUID.randomUUID())
                .definitionKey("sensor.temperature")
                .adapterVersion(2L)
                .sentAt(1_757_000_001_000L)
                .actionId(UUID.randomUUID())
                .build();

        String json = mapper.writeValueAsString(completed);
        JsonNode node = mapper.readTree(json);

        assertEquals("AdapterExecutionCompleted", node.get("eventType").asText());
        assertTrue(node.has("actionId"));
        assertFalse(node.has("errorCode"));
        assertFalse(node.has("errorMessage"));

        AdapterExecutionEvent roundTripped = mapper.readValue(json, AdapterExecutionEvent.class);
        assertEquals(completed, roundTripped);
    }

    @Test
    void failedEventRoundTripsAndCarriesErrorFieldsButNotActionId() throws Exception {
        AdapterExecutionEvent failed = AdapterExecutionEvent.builder()
                .eventType(AdapterEventType.ADAPTER_EXECUTION_FAILED)
                .executionId(UUID.randomUUID())
                .adapterId(UUID.randomUUID())
                .definitionKey("switch.relay")
                .adapterVersion(1L)
                .sentAt(1_757_000_002_000L)
                .errorCode(AdapterErrorCode.DEVICE_UNAVAILABLE)
                .errorMessage("board did not respond within timeout")
                .build();

        String json = mapper.writeValueAsString(failed);
        JsonNode node = mapper.readTree(json);

        assertEquals("AdapterExecutionFailed", node.get("eventType").asText());
        assertEquals("DEVICE_UNAVAILABLE", node.get("errorCode").asText());
        assertFalse(node.has("actionId"));

        AdapterExecutionEvent roundTripped = mapper.readValue(json, AdapterExecutionEvent.class);
        assertEquals(failed, roundTripped);
    }

    @Test
    void unknownFieldInIncomingCommandDoesNotBreakDeserialization() throws Exception {
        String json = "{"
                + "\"commandType\":\"StartAdapterExecution\","
                + "\"executionId\":\"" + UUID.randomUUID() + "\","
                + "\"adapterId\":\"" + UUID.randomUUID() + "\","
                + "\"definitionKey\":\"switch.relay\","
                + "\"adapterVersion\":1,"
                + "\"sentAt\":1757000000000,"
                + "\"configVersion\":1,"
                + "\"futureField\":\"from a newer producer\""
                + "}";

        AdapterExecutionCommand command = mapper.readValue(json, AdapterExecutionCommand.class);

        assertEquals(AdapterCommandType.START_ADAPTER_EXECUTION, command.getCommandType());
        assertEquals(1L, command.getConfigVersion());
    }

    @Test
    void definitionChangedEventRoundTrips() throws Exception {
        AdapterDefinitionChangedEvent event = AdapterDefinitionChangedEvent.builder()
                .definitionKey("switch.relay")
                .activationTopic("automation.switch.relay.activation")
                .eventTopic("automation.switch.relay.event")
                .build();

        String json = mapper.writeValueAsString(event);
        AdapterDefinitionChangedEvent roundTripped =
                mapper.readValue(json, AdapterDefinitionChangedEvent.class);

        assertEquals(event, roundTripped);
        assertNull(mapper.readTree(json).get("changeType"));
    }
}
