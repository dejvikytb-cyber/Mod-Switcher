# ModSwitch — Fabric 1.21.11

This is a starter project for an in-game Fabric mod manager.

## Important limitation
Fabric does **not** provide a safe generic API to unload arbitrary mods while Minecraft is running. This project therefore queues file moves and attempts to apply them when Minecraft shuts down. It does not instantly stop a mod during the current session.

## Current behavior
- Open the screen with **Right Shift**.
- If Mod Menu is installed, it also provides a config-screen entry.
- Click a mod row to queue it for disabling/enabling.
- On shutdown, the project attempts to move matching JAR files between `mods/` and `mods-disabled/`.
- It deliberately protects `minecraft`, `fabricloader`, and `modswitch`.

## Limitations / safety
- Many JAR filenames do not contain the mod ID. Such files are not moved automatically.
- Mods may depend on other mods. Disabling a dependency can make the next launch fail.
- This does not runtime-disable Xaero's Minimap or any other already-loaded mod.
- Back up your `mods` folder before testing. Keep a separate instance/profile for experiments.

## Build
Requires JDK 21 and an internet connection for Gradle dependencies.

From the project directory, with Gradle installed:
```bash
gradle build
```

The output JAR will be in `build/libs/`.

This source project intentionally does not claim to be a verified, ready-to-use JAR. Build it and test it in a separate Minecraft instance before relying on it.
