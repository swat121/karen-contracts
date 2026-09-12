package com.karen.contracts.kafka.automation;

/**
 * Machine-readable failure reason carried by {@code AdapterExecutionEvent.errorCode} when {@code
 * eventType} is {@code ADAPTER_EXECUTION_FAILED} (design doc section 23.6).
 *
 * <p>Wire values are the UPPER_SNAKE_CASE constant names themselves, unlike {@link
 * AdapterCommandType} / {@link AdapterEventType} — no {@code @JsonProperty} mapping is needed.
 *
 * <p>{@code TIMEOUT} is deliberately not a constant here: the adapter never sends it, only
 * Controller sets it, when a response never arrives (design doc section 21.5).
 */
public enum AdapterErrorCode {

    /** The board did not respond. */
    DEVICE_UNAVAILABLE,

    /** The board responded with an error. */
    DEVICE_REJECTED,

    /** {@code configVersion} on the command did not match the one saved by the adapter (design doc section 23.5). */
    CONFIG_OUTDATED,

    /** A start arrived for an {@code adapterId} the adapter has no config for, e.g. after losing its own database. */
    CONFIG_NOT_FOUND,

    /** Everything else. */
    INTERNAL
}
