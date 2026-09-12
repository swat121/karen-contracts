package com.karen.contracts.kafka.automation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Discriminates {@link AdapterExecutionCommand}: both actions travel on the same
 * {@code activationTopic}, distinguished by this field (design doc section 23.4).
 *
 * <p>Wire values are PascalCase ({@code StartAdapterExecution} / {@code TerminateAdapterExecution}),
 * not the Java constant names; each constant carries its wire value via {@link JsonProperty} so
 * Jackson serializes and deserializes it in both directions.
 */
public enum AdapterCommandType {

    /**
     * Starts the adapter's participation in one chain execution. Named to make clear that it is
     * not the adapter service itself that starts, but its role in a specific execution.
     */
    @JsonProperty("StartAdapterExecution")
    START_ADAPTER_EXECUTION,

    /** Terminates the adapter's participation in one chain execution. */
    @JsonProperty("TerminateAdapterExecution")
    TERMINATE_ADAPTER_EXECUTION
}
