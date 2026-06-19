package com.karen.contracts.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event published when a device connects or disconnects from the broker.
 * Consumed by karen-device-control to track device presence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationConnectEvent {

    private String deviceId;

    /** "CONNECTED" or "DISCONNECTED" */
    private String status;
}
