package com.karen.contracts.kafka.automation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event published by Builder on topic {@code automation.definition.changed} on adapter
 * -definition create and update, consumed by Chain Controller (design doc section 23.2).
 *
 * <p>Controller learns an adapter definition's Kafka topics from this topic instead of from the
 * start request; it subscribes to {@code eventTopic}s only once, at service startup, from its own
 * database. A definition added after startup needs a Controller restart to be picked up -- a
 * deliberate decision, not a gap (design doc section 23.2). Builder's {@code republish} endpoint
 * re-publishes every existing definition for first-time and manual resync.
 *
 * <p>Message key is {@code definitionKey}, and this is not incidental: it orders messages for one
 * definition, republish overwrites the previous value under log compaction, and a future delete
 * (not yet defined, design doc section 23.7 item 2) is meant to be a tombstone -- a {@code null}
 * value under the same key -- without any contract change.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdapterDefinitionChangedEvent {

    /** Adapter-definition catalog key, e.g. {@code switch.relay}, {@code sensor.temperature}, {@code delay.timer}. Also the Kafka message key. */
    private String definitionKey;

    /** Topic Controller publishes {@link AdapterExecutionCommand} to for this definition's adapter. */
    private String activationTopic;

    /** Topic the adapter publishes {@link AdapterExecutionEvent} to for this definition. */
    private String eventTopic;

    /**
     * Whether this adapter definition's execution has a hard timeout enforced by Controller
     * (design doc §10.1) -- e.g. {@code true} for {@code switch}/{@code delay} definitions,
     * {@code false} for {@code sensor} ones. Present starting from v0.7.0.
     *
     * <p>{@code null} means the message came from a producer older than v0.7.0, not that the
     * definition has no timeout -- Controller must treat {@code null} and {@code false}
     * differently (no deadline plus a WARN log, versus a deadline that is deliberately absent).
     *
     * <p>No {@code @JsonInclude(NON_NULL)} on this field: for a v0.7.0 producer it applies to
     * every definition, so an unset value is a producer bug and must appear on the wire as
     * {@code null} rather than vanish into a shape indistinguishable from a pre-v0.7.0 message.
     */
    private Boolean hasExecutionTimeout;
}
