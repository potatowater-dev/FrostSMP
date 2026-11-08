# AutoGG

Client-side Fabric mod for Minecraft 1.21.11.

Press a keybind to run a fully configurable command to the **last player you hit**, or if none, the **last player who hit you** (default `/msg {player} GG`). Includes anti-spam cooldown.

See [MODRINTH_DESCRIPTION.md](MODRINTH_DESCRIPTION.md) for the full Modrinth listing text.

## Build

```bat
gradlew remapJar
```

Output: `build/libs/AutoGG-1.2.0.jar`
