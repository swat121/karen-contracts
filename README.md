# karen-contracts

Canonical Kafka contract POJOs for the Karen home-automation ecosystem.

**JitPack coordinate:** `com.github.swat121:karen-contracts:v0.7.0`

Epic: TASK-26001

---

## Usage

### 1. Add JitPack repository

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### 2. Add dependency

```xml
<dependency>
    <groupId>com.github.swat121</groupId>
    <artifactId>karen-contracts</artifactId>
    <version>v0.7.0</version>
</dependency>
```

---

## Contracts (`com.karen.contracts.kafka`)

| Class | Direction | Description |
|-------|-----------|-------------|
| `DeviceRegistrationConnectEvent` | device → control | Device connected / disconnected |
| `DeviceRegistrationMetaEvent` | device → registration | Full device configuration on registration |
| `FeatureEvent` | control → bot | Result of executing a feature command |
| `FeatureCommand` | bot → control | Request to execute a feature on a device |

### `FeatureEvent.payload` shape

Starting from **v0.4.0**, `FeatureEvent` carries a structured `payload: JsonNode` alongside the
existing `message` string. For sensor `READ` and `READ_ALL` results, `payload` is always the same
wrapper object — one reading for `READ`, all readings for `READ_ALL`:

```json
{
  "readings": [
    {
      "address": "28FF641E9C160423",
      "type": "temperature",
      "unit": "celsius",
      "value": 21.5,
      "timestamp": 1754034300
    }
  ]
}
```

`timestamp` is Unix epoch **seconds**, not an ISO-8601 string: the firmware emits
`time(nullptr)` directly (`KarenDevicePresence.cpp:517`). `READ_ALL` entries carry an extra
per-reading `ok` flag (`KarenDevicePresence.cpp:550`), since one failed sensor must not drop the
whole batch; a single `READ` reports failure through `commandStatus` instead.

The object wrapper (not a bare array) leaves room for future top-level fields (e.g. `deviceId`,
`errorMessage`) without breaking the contract. `payload` is absent (`null`) for non-sensor
features until they adopt a shape of their own.

---

## Automation contracts (`com.karen.contracts.kafka.automation`)

Starting from **v0.6.0**, this package carries the Chain Controller ↔ adapter protocol (design
doc `karen-automation-design.md`, §23). It has no schema-catalog entries of its own: these are
plain Kafka messages, validated only by the classes themselves.

| Class | Direction | Topic | Key |
|-------|-----------|-------|-----|
| `AdapterExecutionCommand` | Controller → adapter | `activationTopic` (per adapter definition, from the catalog) | `executionId` |
| `AdapterExecutionEvent` | adapter → Controller | `eventTopic` (per adapter definition, from the catalog) | `executionId` |
| `AdapterDefinitionChangedEvent` | Builder → Controller | `automation.definition.changed` | `definitionKey` |

`executionId` is the partition key for the first two topics: `adapterVersion` only *detects*
out-of-order delivery, it does not prevent it, so ordering within one execution has to come from
the partition (§23.3). `definitionKey` is the key for `automation.definition.changed` so that
republish and update overwrite the previous value under log-compaction, and a definition delete is
a tombstone (`null` value, same key) without a contract change (§23.7 item 2).

### Starting from v0.7.0: `hasExecutionTimeout`

`AdapterDefinitionChangedEvent` carries `hasExecutionTimeout: Boolean` -- whether this definition's
execution has a hard timeout enforced by Controller (§10.1), e.g. `true` for `switch`/`delay`
definitions, `false` for `sensor` ones. There is no `@JsonInclude(NON_NULL)` on it: like every
other field in this class it is mandatory for a v0.7.0 producer, so an unset value must appear on
the wire as `null` rather than vanish.

`null` on the wire means the message came from a producer older than v0.7.0, not that the
definition has no timeout -- a v0.7.0 consumer must not treat `null` the same as `false`.
Concretely: after upgrading Builder to a version that sets this field, every definition already
sitting in the compacted `automation.definition.changed` topic still has the old shape until
Builder's `republish` endpoint is called once; until then Controller keeps seeing `null` for those
definitions.

### Wire values

Unlike the older classes in `com.karen.contracts.kafka` (`status`, `commandStatus` — plain
strings), this package uses typed enums: the vocabulary is closed and symmetric on both ends of
the wire, and error codes in particular need a fixed set Controller can switch on. Enum wire
values are PascalCase, set through `@JsonProperty` on each constant, not the Java constant name
itself:

| Enum | Constant | Wire value |
|------|----------|------------|
| `AdapterCommandType` | `START_ADAPTER_EXECUTION` | `StartAdapterExecution` |
| `AdapterCommandType` | `TERMINATE_ADAPTER_EXECUTION` | `TerminateAdapterExecution` |
| `AdapterEventType` | `ADAPTER_EXECUTION_STARTED` | `AdapterExecutionStarted` |
| `AdapterEventType` | `ADAPTER_EXECUTION_COMPLETED` | `AdapterExecutionCompleted` |
| `AdapterEventType` | `ADAPTER_EXECUTION_FAILED` | `AdapterExecutionFailed` |
| `AdapterEventType` | `ADAPTER_EXECUTION_TERMINATED` | `AdapterExecutionTerminated` |

`AdapterErrorCode` (`DEVICE_UNAVAILABLE`, `DEVICE_REJECTED`, `CONFIG_OUTDATED`,
`CONFIG_NOT_FOUND`, `INTERNAL`) has no `@JsonProperty` mapping: its wire value is the constant
name itself. `TIMEOUT` is not a constant here — the adapter never sends it, only Controller sets
it when a response never arrives (§21.5).

`configVersion`, `actionId`, `errorCode` and `errorMessage` are conditional on `commandType` /
`eventType` (§23.4, §23.6) and must be physically absent from the JSON when not applicable, not
present as `null`. Each of those four fields carries `@JsonInclude(NON_NULL)` individually — the
annotation is deliberately **not** on the class. Every other field is mandatory, and a mandatory
field left unset is a producer bug: it has to appear on the wire as `null` rather than vanish into
a shape a consumer cannot tell apart from a valid message.

> **No `UNKNOWN` fallback constant.** None of these enums declares an `UNKNOWN` /
> `@JsonEnumDefaultValue` catch-all — §23 does not define one, and adding one here would be
> guessing at a future contract. A consumer on an older version of this library that receives a
> wire value introduced by a newer producer (e.g. a `v0.7.0` error code) will fail deserialization
> by default. Consumers that need to tolerate that should either enable Jackson's
> `DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE` together with
> `@JsonEnumDefaultValue`, or catch and handle the deserialization error themselves.

See §23 of `karen-automation-design.md` for the full design rationale (message ordering,
`configVersion`, the adapter-definition sync flow, and the open questions tracked for later
tickets).

---

## Canonical decisions

These decisions were made during TASK-26001 to resolve drift between
`karen-device-registration` and `karen-device-control`:

| Decision | Canonical value | Rationale |
|----------|-----------------|-----------|
| Inner command class name | `SupportedCommand` | Semantic clarity; avoids name collision with outer-scope `Command` patterns |
| `Switch.switchId` type | `Integer` (not `int`) | Null-safe Jackson deserialization without required-field enforcement |
| `SupportedCommand.version` type | `Integer` (not `int`) | Same null-safety reason; consistent with `FeatureCommand.version` |
| `FeatureCommand.version` type | `Integer` (not `int`) | Eliminates latent NPE in consumers that receive partial payloads |

> **Migration note:** `karen-device-registration` uses `Command` (not `SupportedCommand`) as
> the inner class name. ModelMapper references to `.Command.class` must be updated when
> migrating to this library (tracked in TASK-26002).

---

## Command schemas (`com.karen.contracts.schema`)

Starting from **v0.2.0**, karen-contracts is the single source of truth for device command JSON
schemas. Schema files ship inside the jar under:

```
schemas/<feature>/<subtype>/<commandId>.v<version>.json
```

A machine-readable manifest `schemas/catalog.json` lists every entry so consumers do not need
classpath scanning.

### Shipped schemas

| commandId | version | feature | subtype | file |
|-----------|---------|---------|---------|------|
| `TOGGLE_LOCK` | 1 | switch | relay | `schemas/switch/relay/TOGGLE_LOCK.v1.json` |
| `FORCE_SWITCH_STATE` | 1 | switch | relay | `schemas/switch/relay/FORCE_SWITCH_STATE.v1.json` |
| `FORSE_SWITCH_STATE` | 1 | switch | relay | `schemas/switch/relay/FORSE_SWITCH_STATE.v1.json` |
| `READ` | 1 | sensor | temperature | `schemas/sensor/temperature/READ.v1.json` |
| `READ_ALL` | 1 | sensor | temperature | `schemas/sensor/temperature/READ_ALL.v1.json` |

> **`FORCE_SWITCH_STATE` and `FORSE_SWITCH_STATE` are the same command.** The original id carries
> a typo that reached the firmware and was kept there deliberately. Since **v0.5.0** the catalog
> also ships the corrected spelling, pointing at an identical contract; `KarenDevicePresence`
> v2.4.0 registers both ids under one handler and advertises both in
> `switches[].supportedCommands`. Boards flashed before that advertise only `FORSE_*`, so
> **consumers must normalise the name before comparing** rather than matching the string exactly.
> The old id is not removed — doing so would break every board already in the field.
>
> A new id rather than `FORCE_SWITCH_STATE.v2`: `version` here means the version of the payload
> schema, and renaming a command does not change its payload.

> **Envelope shape is inconsistent by design.** `sensor` commands (`READ`) nest their
> command-specific fields under a `payload` object, because the firmware reads
> `doc["payload"]["sensorAddress"]` and cannot be changed without physical re-flashing. `switch`
> commands (`TOGGLE_LOCK`, `FORCE_SWITCH_STATE`/`FORSE_SWITCH_STATE`) keep a flat root instead. Do not "align" one
> style onto the other without checking the firmware first — see `READ.v1.json`'s `description`.

> **`READ.v1` was reshaped in place in v0.4.0 — this is not a precedent.** The version published
> in `v0.3.0` declared a flat `sensorAddress` that no board could ever accept, and no consumer had
> adopted it yet, so nothing could break. Bumping to `READ.v2` was not an option either: the
> firmware advertises `supportedCommands: [{"READ", 1}]` and `karen-device-control` rejects any
> other version, so a v2 schema would require re-flashing every board. **A published schema that
> real producers already use must get a new `.vN` file instead.**

### `catalog.json` manifest shape

```json
[
  {
    "commandId": "TOGGLE_LOCK",
    "version": 1,
    "feature": "switch",
    "subtype": "relay",
    "path": "schemas/switch/relay/TOGGLE_LOCK.v1.json"
  }
]
```

### `SchemaCatalog` accessor

```java
// Shared lazy default — safe for concurrent use
SchemaCatalog catalog = SchemaCatalog.getDefault();

// Or instantiate directly (useful as a Spring @Bean)
SchemaCatalog catalog = new SchemaCatalog();

// Look up a schema as JsonNode (throws NoSuchElementException if absent)
JsonNode schema = catalog.getSchema("TOGGLE_LOCK", 1);

// The feature group the command belongs to ("switch" / "sensor") — for routing
String feature = catalog.getFeature("TOGGLE_LOCK", 1); // -> "switch"
```

The constructor is **fail-fast**: a missing or unparseable `catalog.json`, or any referenced
schema file being absent, throws `IllegalStateException` immediately — not lazily at lookup time.

> **No validation engine is bundled.**  `SchemaCatalog` only loads and indexes schemas using
> `jackson-databind` (already a transitive dependency). Each consumer brings its own
> JSON-Schema validator (e.g. networknt/json-schema-validator, fge/json-schema-validator) and
> feeds it the `JsonNode` returned by this class.

---

## Build

This library requires Java 17 and does **not** inherit `spring-boot-starter-parent`.
Dependency versions are pinned to match Spring Boot 3.2.3 BOM:

- `jackson-databind:2.15.4` (Spring Boot 3.2.3 manages `jackson-bom.version=2.15.4`)
- `lombok:1.18.30` (provided / optional — does not leak into consumers)

```bash
mvn -q -DskipTests package
```
