package com.karen.contracts.kafka.automation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Discriminates {@link AdapterExecutionEvent}: one class carries every response on a definition's
 * {@code eventTopic}, and Controller branches on this field rather than on a Kafka header (design
 * doc section 23.6).
 *
 * <p>Wire values are PascalCase ({@code AdapterExecutionStarted} etc.), not the Java constant
 * names; each constant carries its wire value via {@link JsonProperty} so Jackson serializes and
 * deserializes it in both directions.
 *
 * <p>A response is required for every command, including {@code TerminateAdapterExecution}: the
 * adapter always reports its actual status, never leaves Controller guessing (design doc section
 * 23.6).
 */
public enum AdapterEventType {

    /** The adapter has started executing this node for this execution. */
    @JsonProperty("AdapterExecutionStarted")
    ADAPTER_EXECUTION_STARTED,

    /** The node's execution completed; see {@code AdapterExecutionEvent.actionId}. */
    @JsonProperty("AdapterExecutionCompleted")
    ADAPTER_EXECUTION_COMPLETED,

    /** The node's execution failed; see {@code AdapterExecutionEvent.errorCode}. */
    @JsonProperty("AdapterExecutionFailed")
    ADAPTER_EXECUTION_FAILED,

    /**
     * Response to a {@code TerminateAdapterExecution} that arrived before the matching start
     * (the "tombstone" ordering from design doc section 21.8).
     */
    @JsonProperty("AdapterExecutionTerminated")
    ADAPTER_EXECUTION_TERMINATED
}
