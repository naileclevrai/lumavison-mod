<div align="center">

# LumaVision

### LED screens & live video for Minecraft

**Bring concert-grade LED walls into your worlds — built on Forge, designed for real media pipelines.**

<br/>

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=for-the-badge&logo=minecraft)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.3.0-orange?style=for-the-badge)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17-red?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

[Features](#-features) · [Roadmap](#-roadmap) · [Installation](#-installation) · [Development](#-development) · [Architecture](#-architecture) · [Contributing](#-contributing)

</div>

---

## 📖 About

**LumaVision** is a Minecraft **Forge 1.20.1** mod focused on one thing: **LED screens and video playback in-game**.

Place wall-mounted panels, feed them with dynamic content, and build stage setups, control rooms, or immersive environments — without relying on external display mods.

> **Standalone project.** LumaVision is developed independently from [TheatricalExtraLights](https://github.com/naileclevrai) and targets its own architecture from the ground up.

The mod is built for long-term growth: a clean rendering pipeline, extensible content sources, and room for multiplayer sync and performance work — not a quick prototype.

---

## ✨ Features

### Available now `v0.1.0`

| Area | Status | Description |
|------|--------|-------------|
| 🧱 **LED Screen block** | ✅ | Wall-mounted panel with 6-axis orientation |
| 🖥️ **Dynamic rendering** | ✅ | Animated test pattern (checkerboard + gradient) |
| 🏗️ **Video pipeline** | ✅ | `VideoSource` → `VideoFrame` → `DynamicTexture` → `ScreenRenderer` |
| 📦 **Registries** | ✅ | Blocks, items, block entities, creative tab |
| ⚙️ **Configuration** | ✅ | Common config via `lumavision-common.toml` |
| 🌐 **Networking** | 🔧 | Hook in place — packets not implemented yet |

### Not yet available

- 🎬 Video / image file playback
- 📡 NDI input (Devolay or equivalent)
- 🧩 Merged screen walls (multi-block logical displays)
- 🔄 Multiplayer content synchronization
- ⚡ Advanced rendering optimizations

---

## 🗺️ Roadmap

| Phase | Focus | Status |
|:-----:|-------|:------:|
| **1** | Single LED screen block + dynamic texture pipeline | 🟢 Done |
| **2** | Merged screen walls (several blocks → one logical display) | 🟡 Planned |
| **3** | Static images & video file playback | ⚪ Planned |
| **4** | NDI / live stream support | ⚪ Planned |
| **5** | Network synchronization (server ↔ clients) | ⚪ Planned |
| **6** | Rendering & memory optimizations | ⚪ Planned |

---

## 🔮 Planned capabilities

LumaVision is designed around a **source-agnostic** renderer. Future `VideoSource` implementations will plug into the same pipeline — the screen never needs to know where pixels come from.

| Capability | Description |
|------------|-------------|
| 📺 **LED screens** | Single panels and large tiled walls for stages & venues |
| 🎞️ **Dynamic textures** | GPU-backed frames updated every tick |
| 🎬 **Video playback** | Local files (MP4, etc.) via a dedicated source implementation |
| 🖼️ **Static images** | PNG / JPEG as screen content |
| 📡 **NDI** | Live professional video input through [Devolay](https://github.com/open-dpp/devolay-java) or similar |
| 🌐 **Network sync** | Consistent screen state across multiplayer sessions |
| ⚡ **Performance** | Texture pooling, resolution scaling, culling & batching |

---

## 📋 Requirements

| Requirement | Version |
|-------------|---------|
| **Minecraft** | `1.20.1` |
| **Forge** | `47.3.0` or compatible (`47.x`) |
| **Java (JDK)** | `17` |
| **Gradle** | Wrapper included — no separate install needed |

---

## 📥 Installation

### Players

1. Install [Minecraft Forge 1.20.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) (`47.3.0` recommended).
2. Download the latest `lumavision-*.jar` from [Releases](https://github.com/naileclevrai/lumavison-mod/releases) *(when published)*, or build it locally (see below).
3. Place the JAR in your Minecraft `mods` folder:
   - **Windows:** `%appdata%\.minecraft\mods`
   - **macOS:** `~/Library/Application Support/minecraft/mods`
   - **Linux:** `~/.minecraft/mods`
4. Launch the Forge 1.20.1 profile.

### In-game

Open the **LumaVision** creative tab and place an **LED Screen** on any block face. The panel displays an animated test pattern to validate the rendering pipeline.

---

## 🛠️ Development

### Clone the repository

```bash
git clone https://github.com/naileclevrai/lumavison-mod.git
cd lumavison-mod
```

### Build

```bash
# Windows
gradlew.bat build

# macOS / Linux
./gradlew build
```

Output JAR: `build/libs/lumavision-0.1.0.jar`

### Run a dev environment

```bash
# Client
gradlew.bat runClient      # Windows
./gradlew runClient        # macOS / Linux

# Dedicated server
gradlew.bat runServer
./gradlew runServer
```

> **Note:** The project uses Gradle toolchains. If JDK 17 is not installed locally, Gradle can download it automatically via the Foojay resolver configured in `settings.gradle`.

### IDE setup

1. Open the project folder in **IntelliJ IDEA** or **Eclipse** (ForgeGradle plugins supported).
2. Set the project SDK to **Java 17**.
3. Run `gradlew genIntellijRuns` or `gradlew genEclipseRuns` if run configurations are missing.
4. Use `runClient` to start the game with the mod loaded.

---

## 🏛️ Architecture

### Rendering pipeline

The renderer is intentionally **decoupled** from content producers. Adding NDI, video files, or GIFs means implementing a new `VideoSource` — not rewriting the screen renderer.

```
┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐    ┌────────────────┐
│ VideoSource │ ─► │ VideoFrame  │ ─► │ DynamicTextureHandle │ ─► │ ScreenRenderer │
└─────────────┘    └─────────────┘    └─────────────────────┘    └────────────────┘
       ▲                                                                    │
       │                                                                    ▼
  Test pattern                                                     LED Screen block
  Video file (future)                                              (BlockEntityRenderer)
  NDI stream (future)
  Static image (future)
```

| Layer | Package | Role |
|-------|---------|------|
| `VideoSource` | `fr.lumavision.video` | Produces frames from any media backend |
| `VideoFrame` | `fr.lumavision.video` | ARGB pixel buffer for a single frame |
| `DynamicTextureHandle` | `fr.lumavision.client.texture` | Uploads frames to a Minecraft `DynamicTexture` |
| `ScreenTextureManager` | `fr.lumavision.client.texture` | Per-screen pipeline lifecycle (tick, cleanup) |
| `ScreenRenderer` | `fr.lumavision.client.render` | Draws the bezel + display quad in world space |

### Project layout

```
lumavison-mod/
├── build.gradle                 # ForgeGradle build script
├── gradle.properties            # Versions & mod metadata
├── settings.gradle
│
└── src/main/
    ├── java/fr/lumavision/
    │   ├── LumaVisionMod.java              # Mod entry point
    │   ├── LumaVisionModClient.java        # Common client setup
    │   │
    │   ├── block/                          # Block definitions & shapes
    │   │   ├── LedScreenBlock.java
    │   │   └── LedScreenShapes.java
    │   │
    │   ├── blockentity/                    # Server-side screen state
    │   │   └── LedScreenBlockEntity.java
    │   │
    │   ├── video/                          # Loader-agnostic video abstractions
    │   │   ├── VideoSource.java
    │   │   ├── VideoFrame.java
    │   │   └── StubVideoSource.java
    │   │
    │   ├── client/
    │   │   ├── LumaVisionClientMod.java    # BER registration
    │   │   ├── LumaVisionClientForgeEvents.java
    │   │   ├── video/
    │   │   │   └── TestPatternVideoSource.java
    │   │   ├── texture/
    │   │   │   ├── DynamicTextureHandle.java
    │   │   │   └── ScreenTextureManager.java
    │   │   └── render/
    │   │       └── ScreenRenderer.java
    │   │
    │   ├── config/
    │   │   └── ModConfig.java
    │   │
    │   ├── registry/                       # Deferred registers
    │   │   ├── ModBlocks.java
    │   │   ├── ModItems.java
    │   │   ├── ModBlockEntities.java
    │   │   └── ModCreativeTabs.java
    │   │
    │   └── network/
    │       └── ModNetworking.java          # Future packet layer
    │
    └── resources/
        ├── META-INF/mods.toml
        └── assets/lumavision/
            ├── blockstates/
            ├── lang/
            ├── models/
            └── textures/
```

### Configuration

On first launch, a common config file is generated at:

```
config/lumavision-common.toml
```

| Option | Default | Description |
|--------|---------|-------------|
| `debugLogging` | `false` | Verbose logs for textures, networking, rendering |
| `maxTextureResolution` | `1024` | Max longest side for dynamic screen textures (64–4096) |
| `enableNdi` | `false` | Reserved for future NDI support |

---

## 🤝 Contributing

Contributions are welcome — whether it's code, bug reports, design feedback, or documentation.

1. **Fork** the repository.
2. **Create a branch** from `main`:
   ```bash
   git checkout -b feat/my-feature
   ```
3. **Make your changes** — follow existing package structure and naming conventions.
4. **Test locally** with `gradlew runClient` and ensure `gradlew build` passes.
5. **Commit** with a clear message (`feat:`, `fix:`, `docs:`, etc.).
6. **Open a Pull Request** against `main` with a short description and test steps.

### Guidelines

- Keep the **video pipeline abstraction** intact — new media types implement `VideoSource`, not renderer hacks.
- Prefer **small, focused PRs** over large rewrites.
- Write code and comments in **English**.
- Target **Java 17** and **Forge 1.20.1** APIs.

For major changes (new subsystems, breaking architecture), open an **issue** first to discuss the approach.

---

## 📄 License

This project is released under the **[MIT License](LICENSE)**.

You are free to use, modify, and distribute this software with minimal restrictions. See the `LICENSE` file for the full text.

---

## 🙏 Credits

| | |
|---|---|
| **Author** | [Nailec](https://github.com/naileclevrai) |
| **Project** | [lumavison-mod](https://github.com/naileclevrai/lumavison-mod) |
| **Platform** | [Minecraft Forge](https://minecraftforge.net/) |
| **Inspiration** | Stage & concert setups in Minecraft — built for creators who need real display workflows |

---

<div align="center">

**LumaVision** — *Light up your builds.*

</div>
