# AutoClicker Mod — Fabric 1.21.1

A client-side autoclicker mod for Minecraft 1.21.1 (Fabric).  
Controlled entirely via **CapsLock** with intelligent entity-detection to avoid server kicks.

---

## Features

| Action | Trigger | Behavior |
|---|---|---|
| **Left-click autoclicker** | Single tap CapsLock | 20 CPS, only fires when a living entity is in attack range |
| **Right-click autoclicker** | Double-tap CapsLock (within 1 second) | 20 CPS, always fires (block placing / item use) |
| **Toggle off** | Tap CapsLock again | Turns off whichever autoclicker is active |

### Why entity-detection for left-click?
Many servers (anti-cheat systems) detect when a player sends attack packets without a valid target, and will kick or flag the player. This mod checks that a **living entity** is within your attack reach (3 blocks) before sending any attack packet — so you won't get kicked for swinging at air.

---

## Controls

```
CapsLock (single press)      → Toggle left-click autoclicker ON/OFF
CapsLock (double-press <1s)  → Toggle right-click autoclicker ON/OFF
```

Status messages appear in the **action bar** (above your hotbar) so you always know what's active.

---

## Building from Source

### Prerequisites
- Java 21 JDK ([Adoptium](https://adoptium.net/) recommended)
- Internet connection (Gradle downloads dependencies automatically)

### Steps

1. **Clone / download** this project folder.

2. **Open a terminal** in the project root (where `build.gradle` lives).

3. **Build the mod:**
   ```bash
   # On Linux/macOS:
   ./gradlew build

   # On Windows:
   gradlew.bat build
   ```
   > First build will take a few minutes to download Minecraft, mappings, and Fabric API.

4. **Find your JAR:**
   ```
   build/libs/autoclicker-1.0.0.jar
   ```
   *(Ignore the `-sources.jar` file — that's just source code.)*

5. **Install:**  
   Copy `autoclicker-1.0.0.jar` into your `.minecraft/mods/` folder.  
   Make sure you also have **Fabric Loader** and **Fabric API** installed.

---

## Dependencies (auto-downloaded by Gradle)

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.16.9+ |
| Fabric API | 0.102.0+1.21.1 |
| Java | 21 |

---

## Project Structure

```
autoclicker/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── README.md
└── src/
    ├── main/
    │   ├── java/com/autoclicker/mod/
    │   │   └── AutoClickModInit.java     ← Empty server-side initializer
    │   └── resources/
    │       └── fabric.mod.json           ← Mod metadata
    └── client/
        └── java/com/autoclicker/mod/
            └── AutoClickMod.java         ← All the logic lives here
```

---

## How it Works (Technical)

- **CapsLock detection**: Polled every game tick via `GLFW.glfwGetKey()` with rising-edge detection (fires once per press, not repeatedly while held).
- **Double-tap detection**: Timestamps each CapsLock press; if two presses occur within 1000ms, it's treated as a double-tap.
- **Left-click**: Every 3 ticks (= 20 CPS at 20 TPS), calls `InteractionManager#attackEntity()` directly on the crosshair entity — only if `crosshairTarget` is an `EntityHitResult` pointing at a living entity within 3 blocks.
- **Right-click**: Every 3 ticks, temporarily sets `useKey` pressed and calls `MinecraftClient#doItemUse()` — the same codepath the game uses for normal right-clicks.
- **No Mixins needed**: Everything is done through Fabric's `ClientTickEvents` and direct API calls.

---

## Notes

- This mod is **client-side only** — it does not need to be installed on the server.
- Use responsibly. Autoclickers may violate the rules of some servers.
- The 20 CPS rate is exact at 20 TPS (normal server tick rate). At lower TPS the rate will be proportionally lower.
