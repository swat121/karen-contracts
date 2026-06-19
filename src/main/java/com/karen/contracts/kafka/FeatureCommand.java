package com.karen.contracts.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka command published by karen_telegram_bot and consumed by karen-device-control
 * to trigger a feature action on a device.
 *
 * <p>{@code payload} is an arbitrary JSON object whose structure depends on the feature;
 * consumers must deserialize it themselves after reading {@code featureId}.
 *
 * <p>{@code version} is {@link Integer} (not {@code int}) for null-safe Jackson deserialization,
 * consistent with {@code SupportedCommand.version} in {@link DeviceRegistrationMetaEvent}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureCommand {

    private String commandId;

    private String featureId;

    private String deviceId;

    /** Schema version of the command payload. Canonical type: Integer (not int). */
    private Integer version;

    private String localRef;

    /** Arbitrary feature-specific payload; structure determined by featureId. */
    private JsonNode payload;
}
