package com.karen.contracts.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event published by karen-device-control on {@code karen.sensor.data} for each
 * successfully read sensor reading, keyed by {@code deviceId}. Consumed by Sensor Adapter
 * (Karen Automation) and any other telemetry subscriber.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorDataEvent {

    private String deviceId;

    private String macAddress;

    private String sensorAddress;

    /** Sensor type, upper-cased (e.g. {@code TEMPERATURE}). */
    private String type;

    private String unit;

    private Double value;

    /** Server time when this event was published, epoch milliseconds. */
    private Long measuredAt;

    private String requestId;
}
