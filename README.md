# Smart Home IoT Integration — Adapter Pattern

## Student Information

| Field | Value |
|---|---|
| Student Name | **Mukhammed Sariyev** |
| Student ID | **250103053** |
| Personal Parameter | **K = 3** (last digit of the Student ID) |
| Design Pattern | **Object Adapter Pattern** (GoF, via Composition) |
| Language | **Java** |

---

## Project Overview

This lab is about **OmniHome IoT**, a smart home system.

- The central controller `ModernHub` works **only** with objects that implement the `SmartDevice` interface.
- Two legacy appliances from external vendors, `LegacyBulb` and `LegacyThermostat`, do **not** implement `SmartDevice`. Their method names, units and state representations are different:
  - `LegacyBulb` uses a raw brightness scale of `0–255` and has a filament that can break.
  - `LegacyThermostat` uses text dial states: `"IDLE"`, `"LOW"`, `"MEDIUM"`, `"MAX"`.
- The **Adapter Pattern** solves this. Each legacy device is wrapped in an adapter that implements `SmartDevice` and translates every call to the legacy API.
- The **Object Adapter** variant is used: the adapter *implements* the target interface and *holds* the legacy object in a private field (**Composition**), instead of inheriting from it.

The legacy classes and `ModernHub` are never modified.

---

## Architecture

```text
            SmartDevice  (Target)
                 ↑
                 |
      +----------+-----------+
      |                      |
 BulbAdapter          ThermostatAdapter      (Adapters)
      |                      |
  LegacyBulb          LegacyThermostat       (Adaptees)
```

`ModernHub` (Client) holds a `List<SmartDevice>` and works with any `SmartDevice`, without knowing what is behind it.

| Pattern Role | Class | Responsibility |
|---|---|---|
| **Target** | `SmartDevice` | Uniform interface: `turnOn()`, `turnOff()`, `isOn()`, `getPowerPercent()` (0–100) |
| **Client** | `ModernHub` | Calls `activateAll()`, `emergencyShutdown()` and `calculateAveragePowerUsage()` on a list of `SmartDevice` |
| **Adaptee A** | `LegacyBulb` | Legacy bulb with a 0–255 brightness scale and a filament state |
| **Adaptee B** | `LegacyThermostat` | Legacy thermostat controlled with String dial positions |
| **Adapter A** | `BulbAdapter` | Implements `SmartDevice`, delegates to `LegacyBulb` with calibrated math |
| **Adapter B** | `ThermostatAdapter` | Implements `SmartDevice`, translates dial strings to `boolean` / `int` |

The driver `Main` is not a pattern role. It only builds the system and demonstrates it.

---

## Files

All seven files are in the **default package**.

### SmartDevice.java
*Provided interface. Not modified.*
Declares `turnOn()`, `turnOff()`, `isOn()` and `getPowerPercent()` (range 0 to 100).

### LegacyBulb.java
*Provided legacy class. Not modified.*
Stores brightness in the range `0–255` (`setBrightness`, `readBrightness`) and has a filament flag (`breakFilament`, `hasPower`).

### LegacyThermostat.java
*Provided legacy class. Not modified.*
Stores the dial position as a `String` (`rotateDial`, `checkDial`). The initial state is `"IDLE"`.

### ModernHub.java
*Provided client class. Not modified.*
Works with a `List<SmartDevice>`: turns all devices on, turns all devices off, and calculates the average power percentage.

### BulbAdapter.java
*Student implementation.*

- **Composition:** the legacy bulb is stored in `private final LegacyBulb bulb`. The constructor `BulbAdapter(LegacyBulb bulb)` throws `IllegalArgumentException` if `bulb` is `null`.
- **`turnOn()`:** calls `bulb.setBrightness(255)`.
- **`turnOff()`:** calls `bulb.setBrightness(0)`.
- **`isOn()`:** returns `true` only if `bulb.hasPower()` is `true` **and** `bulb.readBrightness() > 0`.
- **`getPowerPercent()`:** converts the raw `0–255` value to a `0–100` percentage using the personal calibration (`K = 3`, see [Student ID Calibration](#student-id-calibration)). The result is an `int`, capped at `100`. Zero brightness returns `0`.
- **Broken filament:** if `bulb.hasPower()` is `false`, `getPowerPercent()` returns `0` and `isOn()` returns `false`, whatever `readBrightness()` reports.

### ThermostatAdapter.java
*Student implementation.*

- **Composition:** the legacy thermostat is stored in `private final LegacyThermostat thermostat`. The constructor throws `IllegalArgumentException` if the argument is `null`.
- **Conversion of states:**

  | Dial state | `isOn()` | `getPowerPercent()` |
  |---|---|---|
  | `IDLE` | `false` | `0` |
  | `LOW` | `true` | `33` |
  | `MEDIUM` | `true` | `66` |
  | `MAX` | `true` | `100` |
  | anything else (`STUCK`, `OVERHEAT`, `""`, `null`, ...) | `false` | `-1` |

- **`turnOn()`:** if the dial is `"IDLE"`, it calls `rotateDial("LOW")`. In every other state it does nothing, so the operation is idempotent (an active `MEDIUM` or `MAX` setting is never overwritten).
- **`turnOff()`:** always calls `rotateDial("IDLE")`.
- **`isOn()`:** returns `true` when `getPowerPercent() > 0`, so only `LOW`, `MEDIUM` and `MAX` count as on.
- **`getPowerPercent()`:** reads `checkDial()`, checks for `null` first (a `switch` on `null` would throw a `NullPointerException`), then maps the known states. Any unknown or `null` state returns `-1` (error sentinel).

### Main.java
*Student driver / test class.*

1. Creates one `LegacyBulb` and one `LegacyThermostat`.
2. Wraps them in `BulbAdapter` and `ThermostatAdapter`.
3. Builds `List<SmartDevice> deviceList = List.of(bulbAdapter, thermostatAdapter)`.
4. Creates `ModernHub hub = new ModernHub(deviceList)`.
5. Contains the mandatory commented-out line `// ModernHub badHub = new ModernHub(List.of(rawBulb)); // COMPILE ERROR` with an explanatory comment below it.
6. **Activation:** calls `hub.activateAll()` and prints the bulb brightness, the thermostat dial and whether all devices are on.
7. **Average power:** prints `hub.calculateAveragePowerUsage()` (66.50% with `K = 3`).
8. **Calibration check:** a separate bulb with raw brightness `128` is checked to return `53%`.
9. **Fault injection (Stage 4):** breaks the bulb filament, then sets the thermostat dial to `"STUCK"`, `"OVERHEAT"`, `""` and `null`, and checks the adapter results.
10. **Emergency shutdown:** calls `hub.emergencyShutdown()`, then checks that the bulb brightness is `0`, the thermostat dial is `IDLE` and the average power is `0.00%`.
11. Prints `ALL INTEGRATION TESTS PASSED (100/100)` if every check passed, otherwise the number of failed checks.

---

## Student ID Calibration

Each student's bulb conversion depends on the last digit of their Student ID.

```text
Student ID: 250103053
K = 3
```

Formula:

```text
Raw Percent        = floor((rawBrightness * 100) / 255)
Calibrated Percent = Raw Percent + 3
```

Rules:

- The result is capped at **100**: `min(100, Calibrated Percent)`.
- If `rawBrightness == 0`, the result is strictly **0** (no offset is added).
- The return type is `int` (integer division gives the floor for non-negative values).

Implementation:

```java
int raw = bulb.readBrightness();
int base = (raw * 100) / 255;
int calibrated = base + K;          // K = 3
return Math.min(100, calibrated);
```

Examples:

| rawBrightness | Raw Percent | + K (3) | Returned |
|---|---|---|---|
| 0 | 0 | — (zero rule) | **0** |
| 1 | 0 | 3 | **3** |
| 128 | 50 | 53 | **53** |
| 255 | 100 | 103 | **100** (capped) |

Detailed example for `rawBrightness = 128`:

```text
Base   = floor((128 * 100) / 255) = floor(50.19) = 50
Output = 50 + 3 = 53%
```

---

## Adapter Pattern Explanation

### Why `LegacyBulb` cannot be passed to `ModernHub` directly

`ModernHub` expects a `List<SmartDevice>`. `LegacyBulb` does not implement `SmartDevice`, so `List.of(rawBulb)` is a `List<LegacyBulb>`, which is an incompatible type. The Java compiler rejects it at compile time:

```java
// ModernHub badHub = new ModernHub(List.of(rawBulb)); // COMPILE ERROR
```

Also, the legacy class cannot be edited (it is vendor code), so the fix must be outside of it.

### The solution

Each legacy device is wrapped in an adapter that *is* a `SmartDevice`:

```text
LegacyBulb
    ↓
BulbAdapter
    ↓
SmartDevice
    ↓
ModernHub
```

```text
LegacyThermostat
    ↓
ThermostatAdapter
    ↓
SmartDevice
    ↓
ModernHub
```

### Why this is an Object Adapter (Composition)

- Each adapter **implements** the target interface `SmartDevice`.
- Each adapter **holds** the adaptee in a `private final` field and forwards calls to it (`bulb.setBrightness(...)`, `thermostat.rotateDial(...)`).
- The adapters do **not** extend `LegacyBulb` or `LegacyThermostat`, so this is not a Class Adapter (which would need multiple inheritance, unsupported in Java).
- The adaptee stays unchanged, and the client (`ModernHub`) stays unchanged.

---

## Fault Tolerance

Stage 4 makes the adapters safe against faulty hardware, so `ModernHub` never crashes.

### Broken Bulb Filament

When this is called:

```java
bulb.breakFilament();
```

`bulb.hasPower()` becomes `false`, but `bulb.readBrightness()` may still return an old value such as `255`. `BulbAdapter` checks `hasPower()` first and reports:

```text
isOn()            = false
getPowerPercent() = 0
```

regardless of what the brightness register contains.

### Corrupted Thermostat Dial

For dial states such as:

```text
STUCK
OVERHEAT
""
null
```

`ThermostatAdapter` reports:

```text
isOn()            = false
getPowerPercent() = -1
```

`-1` is the error sentinel. No `NullPointerException`, `IllegalArgumentException` or other unhandled exception is thrown. `null` is possible because `LegacyThermostat.rotateDial(null)` accepts it.

---

## How to Compile and Run

All **7 Java files** must be in the same working directory (default package):

```text
SmartDevice.java
LegacyBulb.java
LegacyThermostat.java
ModernHub.java
BulbAdapter.java
ThermostatAdapter.java
Main.java
```

Then run:

```bash
mkdir -p bin
javac -d bin *.java
java -cp bin Main
```

### Expected Output

```text
============================================================
 OMNIHOME SMART CONTROLLER: SYSTEM STARTUP
============================================================
[Init] LegacyBulb and LegacyThermostat initialized and wrapped.
[Hub] Registering 2 adapted devices into ModernHub...

--- OPERATION: ACTIVATE ALL DEVICES ---
[Action] ModernHub.activateAll() invoked.
 -> BulbAdapter: Brightness set to 255.
 -> ThermostatAdapter: Dial set to 'LOW'.
[Status] All devices reported active: true
[Power] Fleet Average Power Usage: 66.50% (Bulb: 100%, Thermostat: 33%)

--- CALIBRATION CHECK (K = 3) ---
 -> raw 128 -> percent: 53% [PASSED]

--- AUDIT: HARDWARE FAULT INJECTION (STAGE 4) ---
[Fault 1] Filament physically severed on LegacyBulb...
 -> BulbAdapter.isOn(): false [PASSED]
 -> BulbAdapter.getPowerPercent(): 0% [PASSED]
[Fault 2] Dial encoder set to illegal 'STUCK' state on LegacyThermostat...
 -> ThermostatAdapter.isOn(): false [PASSED]
 -> ThermostatAdapter.getPowerPercent(): -1 [PASSED]
[Fault 3] Dial encoder set to illegal 'OVERHEAT' state on LegacyThermostat...
 -> ThermostatAdapter.isOn(): false [PASSED]
 -> ThermostatAdapter.getPowerPercent(): -1 [PASSED]
[Fault 4] Dial encoder set to illegal '' state on LegacyThermostat...
 -> ThermostatAdapter.isOn(): false [PASSED]
 -> ThermostatAdapter.getPowerPercent(): -1 [PASSED]
[Fault 5] Dial encoder set to illegal null state on LegacyThermostat...
 -> ThermostatAdapter.isOn(): false [PASSED]
 -> ThermostatAdapter.getPowerPercent(): -1 [PASSED]

--- OPERATION: EMERGENCY SHUTDOWN ---
[Action] ModernHub.emergencyShutdown() invoked.
 -> Bulb raw brightness: 0 [PASSED]
 -> Thermostat dial: IDLE [PASSED]
[Power] Fleet Average Power Usage: 0.00%
============================================================
 ALL INTEGRATION TESTS PASSED (100/100)
============================================================
```

The fleet average is `(100 + 33) / 2 = 66.50%`. The bulb value `100 + 3 = 103` is capped at `100`.

---

## AI Usage Disclosure

AI (Claude by Anthropic) was used as an assistant while completing this lab. It helped to:

- write the first version of  `Main.java`;
- write this `README.md`.
---
