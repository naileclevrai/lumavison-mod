# LumaVision

**Forge 1.20.1** Minecraft mod dedicated to LED screens and video content playback in-game.

Independent from TheatricalExtraLights — this repository provides the foundation for an extensible architecture.

## Current status (v0.1.0)

- Gradle project + Forge 47.3.0
- Package `fr.lumavision`
- Ready registries: blocks, items, block entities, creative tab
- Common configuration (`lumavision-common.toml`)
- Network hook prepared (no packets yet)

**No video features** are implemented at this stage.

## Roadmap

| Phase | Content |
|-------|---------|
| 1 | LED screens (blocks + block entities) |
| 2 | Dynamic textures and client rendering |
| 3 | Video / image playback |
| 4 | NDI support (Devolay or equivalent) |
| 5 | Multiplayer network synchronization |
| 6 | Rendering optimizations |

## Requirements

- **JDK 17**
- Gradle (wrapper included)

## Build

```bash
./gradlew build
```

The JAR is output to `build/libs/`.

## Development

```bash
./gradlew runClient
./gradlew runServer
```

## Code structure

```
src/main/java/fr/lumavision/
├── LumaVisionMod.java          # Entry point
├── LumaVisionModClient.java    # Client setup + networking
├── config/
│   └── ModConfig.java          # Forge configuration
├── registry/
│   ├── ModBlocks.java
│   ├── ModItems.java
│   ├── ModBlockEntities.java
│   └── ModCreativeTabs.java
└── network/
    └── ModNetworking.java      # Packets (coming soon)
```

## License

MIT
