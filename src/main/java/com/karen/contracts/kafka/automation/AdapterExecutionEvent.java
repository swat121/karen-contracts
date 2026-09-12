package com.karen.contracts.kafka.automation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Kafka event published by an adapter on a definition's {@code eventTopic} and consumed by Chain
 * Controller (design doc section 23.6). One class mirrors {@link AdapterExecutionCommand}: the
 * response kind is the {@code eventType} field, not a separate class, so Controller reads one
 * class from the topic and branches on it.
 *
 * <p>{@code adapterVersion} is always present here, and it is an echo of the command's {@code
 * adapterVersion} this event answers -- not a new sequence number, and not a message-format
 * version. It lets Controller tell which command a response belongs to; the two responses to one
 * start ({@code ADAPTER_EXECUTION_STARTED} then {@code ADAPTER_EXECUTION_COMPLETED}) carry the
 * same version, their ordering fixed by the matrix in design doc section 21.5.
 *
 * <p>{@code actionId} identifies which action or branch fired, only present on {@code
 * ADAPTER_EXECUTION_COMPLETED}. The adapter reports what fired; Controller alone decides where to
 * go next in its graph (design doc section 21.5), so there is no {@code nextAdapterId} or {@code
 * terminal} field here.
 *
 * <p>{@code errorCode} / {@code errorMessage} are only present on {@code
 * ADAPTER_EXECUTION_FAILED}. {@code @JsonInclude(NON_NULL)} on those three fields keeps them
 * physically absent from the JSON when not applicable, rather than sending them as {@code null}.
 * The annotation is deliberately not on the class: a mandatory field left unset is a producer bug,
 * and it must show up on the wire as {@code null} rather than silently vanish into a shape
 * indistinguishable from a valid message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdapterExecutionEvent {

    private AdapterEventType eventType;

    /** Chain execution identifier (design doc section 23.1); same name across all Controller <-> adapter messages. */
    private UUID executionId;

    /** UUID of the chain node. */
    private UUID adapterId;

    /** Adapter-definition catalog key, e.g. {@code switch.relay}, {@code sensor.temperature}, {@code delay.timer}. */
    private String definitionKey;

    /** Echo of the command's {@code adapterVersion}, not the format version of this message; see class Javadoc. */
    private Long adapterVersion;

    /** Epoch milliseconds when the event was sent. */
    private Long sentAt;

    /** Only present when {@code eventType} is {@code ADAPTER_EXECUTION_COMPLETED}; see class Javadoc. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID actionId;

    /** Only present when {@code eventType} is {@code ADAPTER_EXECUTION_FAILED}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AdapterErrorCode errorCode;

    /** Only present when {@code eventType} is {@code ADAPTER_EXECUTION_FAILED}; free-form text for troubleshooting. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;
}
