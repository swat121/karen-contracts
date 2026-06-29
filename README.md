# karen-contracts

Canonical Kafka contract POJOs for the Karen home-automation ecosystem.

**JitPack coordinate:** `com.github.swat121:karen-contracts:v0.1.0`

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
    <version>v0.1.0</version>
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
| `FORSE_SWITCH_STATE` | 1 | switch | relay | `schemas/switch/relay/FORSE_SWITCH_STATE.v1.json` |
| `READ` | 1 | sensor | temperature | `schemas/sensor/temperature/READ.v1.json` |
| `READ_ALL` | 1 | sensor | temperature | `schemas/sensor/temperature/READ_ALL.v1.json` |

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
