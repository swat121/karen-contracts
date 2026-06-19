package com.karen.contracts.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kafka event published by a Karen device on registration, describing its full
 * configuration: MQTT topics, switches and sensors with their supported commands.
 *
 * <p>Canonical decisions (TASK-26001):
 * <ul>
 *   <li>{@code Switch.switchId} is {@link Integer} (not {@code int}) to allow null-safe
 *       deserialization in consumers that use Jackson without required-field enforcement.</li>
 *   <li>The inner command class is named {@code SupportedCommand} (not {@code Command}) for
 *       semantic clarity. Consumers migrating from {@code Command} must update ModelMapper
 *       references (tracked in TASK-26002).</li>
 *   <li>{@code SupportedCommand.version} is {@link Integer} for the same null-safety reason.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationMetaEvent {

    private String deviceId;
    private Settings settings;
    private Errors errors;
    private List<Switch> switches;
    private List<Sensor> sensors;

    @Data
    public static class Settings {
        private String commandTopic;
        private String eventTopic;
    }

    @Data
    public static class Errors {
        private String eventTopic;
    }

    @Data
    public static class Switch {
        /** Canonical type: Integer (not int) — see class-level Javadoc. */
        private Integer switchId;
        private String subtype;
        private List<SupportedCommand> supportedCommands;
        private String eventTopic;
        private String commandTopic;
        private String errorTopic;
    }

    @Data
    public static class Sensor {
        private String address;
        private String type;
        private String unit;
        private String subtype;
        private List<SupportedCommand> supportedCommands;
        private String eventTopic;
        private String commandTopic;
        private String errorTopic;
    }

    /**
     * A command supported by a switch or sensor.
     * Named {@code SupportedCommand} (canonical, TASK-26001) rather than {@code Command}.
     */
    @Data
    public static class SupportedCommand {
        private String id;
        /** Canonical type: Integer (not int) — see class-level Javadoc. */
        private Integer version;
    }
}
