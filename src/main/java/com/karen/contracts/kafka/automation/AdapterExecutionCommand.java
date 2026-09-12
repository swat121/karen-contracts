package com.karen.contracts.kafka.automation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Kafka command published by Chain Controller on a definition's {@code activationTopic} and
 * consumed by that adapter (design doc section 23.4). One class carries both start and
 * termination; they differ only in {@code commandType}, since both travel on the same topic.
 *
 * <p>The field is named {@code commandType}, not {@code command}: a node action already has a
 * {@code command} (e.g. {@code FORCE_SWITCH_STATE} in {@link
 * com.karen.contracts.kafka.FeatureCommand}), and two {@code command} fields on one model would
 * be confusing about which layer they belong to.
 *
 * <p>{@code configVersion} is only present when {@code commandType} is {@code
 * START_ADAPTER_EXECUTION}: it is the chain-config version the adapter must compare against its
 * own saved config before executing (design doc section 23.5). Termination carries no config.
 * {@code @JsonInclude(NON_NULL)} on that one field keeps it physically absent from the JSON on
 * terminate rather than sending it as {@code null}. The annotation is deliberately not on the
 * class: a mandatory field left unset is a producer bug, and it must show up on the wire as
 * {@code null} rather than silently vanish into a shape indistinguishable from a valid message.
 *
 * <p>Deliberately absent from this command: a termination reason (only {@code stop} triggers
 * termination today), the node's own config (the adapter already holds it from chain creation,
 * section 23.5), and {@code deadlineMs} / {@code attempt} (Controller's watchdog owns deadlines).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdapterExecutionCommand {

    private AdapterCommandType commandType;

    /** Chain execution identifier (design doc section 23.1); same name across all Controller <-> adapter messages. */
    private UUID executionId;

    /** UUID of the chain node, assigned when the chain was created. */
    private UUID adapterId;

    /** Adapter-definition catalog key, e.g. {@code switch.relay}, {@code sensor.temperature}, {@code delay.timer}. */
    private String definitionKey;

    /** Sequence number within this execution (design doc section 23.3); orders messages, not a payload-format version. */
    private Long adapterVersion;

    /** Epoch milliseconds when the command was sent. */
    private Long sentAt;

    /** Only present when {@code commandType} is {@code START_ADAPTER_EXECUTION}; see class Javadoc. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long configVersion;
}
