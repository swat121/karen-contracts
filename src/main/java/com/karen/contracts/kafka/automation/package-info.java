/**
 * Kafka contracts for the Chain Controller and adapter protocol (design doc section 23,
 * {@code karen-automation-design.md}).
 *
 * <p>Three topics carry these messages:
 *
 * <ul>
 *   <li>{@code activationTopic} (per adapter definition) --
 *       {@link com.karen.contracts.kafka.automation.AdapterExecutionCommand}: Controller writes,
 *       the adapter of that definition reads.
 *   <li>{@code eventTopic} (per adapter definition) --
 *       {@link com.karen.contracts.kafka.automation.AdapterExecutionEvent}: the adapter writes,
 *       Controller reads.
 *   <li>{@code automation.definition.changed} (catalog-wide) --
 *       {@link com.karen.contracts.kafka.automation.AdapterDefinitionChangedEvent}: Builder writes
 *       on adapter-definition create/update, Controller reads to learn {@code activationTopic} /
 *       {@code eventTopic} from Kafka rather than from the start request; see the class Javadoc
 *       for the restart caveat.
 * </ul>
 */
package com.karen.contracts.kafka.automation;
