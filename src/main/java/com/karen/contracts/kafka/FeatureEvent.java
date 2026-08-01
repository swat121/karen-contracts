package com.karen.contracts.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Kafka event published by karen-device-control after executing a feature command.
 * Consumed by karen_telegram_bot to report command results back to users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureEvent {

    private UUID requestId;

    private String deviceId;

    /** "SUCCESS", "FAILURE", etc. */
    private String commandStatus;

    private String commandId;

    private String message;

    /** Structured result payload; shape determined by the originating feature/command. */
    private JsonNode payload;
}
