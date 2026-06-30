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
| 🧩 **Merged screen walls** | ✅ | Adjacent panels form one logical display with shared texture |
| 📡 **NDI input (Devolay)** | ✅ | Network source discovery + live frame capture (client) |
| 🖥️ **Dynamic rendering** | ✅ | Test pattern fallback + NDI live video |
| 🏗️ **Video pipeline** | ✅ | `VideoSource` → `VideoFrame` → `DynamicTexture` → `ScreenRenderer` |
| 📦 **Registries** | ✅ | Blocks, items, block entities, creative tab |
| ⚙️ **Configuration** | ✅ | Common config via `lumavision-common.toml` |
| 🌐 **Networking** | 🔧 | Hook in place — packets not implemented yet |

### Not yet available

- 🎬 Video / image file playback
- 🔄 Multiplayer content synchronization
- ⚡ Advanced rendering optimizations

---

## 🗺️ Roadmap

| Phase | Focus | Status |
|:-----:|-------|:------:|
| **1** | Single LED screen block + dynamic texture pipeline | 🟢 Done |
| **2** | Merged screen walls (several blocks → one logical display) | 🟢 Done |
| **3** | Static images & video file playback | 🟡 Next |
| **4** | NDI / live stream support | 🟢 Done (v1) |
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
| 📡 **NDI** | Live professional video via [Devolay](https://github.com/WalkerKnapp/devolay) (bundled natives) |
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

Open the **LumaVision** creative tab and place **LED Screen** blocks. **Right-click** any screen to open the configuration GUI and choose a media provider and source. By default, walls show a test pattern until a source is applied.

### Screen configuration (in-game)

1. Place one or more **LED Screen** blocks (adjacent panels merge into one logical wall).
2. **Right-click** any panel in the wall.
3. Choose a **Provider** (NDI, Test Pattern, …).
4. Select a **Source** from the list and click **Apply**.

The binding is stored on the wall's **group origin** block and persists across saves. Merged walls share one source — configuring any panel configures the entire wall.

The GUI reads exclusively from `VideoSourceCatalog` — no direct NDI or Devolay imports — so new providers (MP4, GIF, browser, …) appear automatically once implemented.

### NDI setup

1. Install an NDI sender on your LAN (OBS NDI, vMix, NDI Tools, etc.).
2. Edit `config/lumavision-common.toml` after first launch:

```toml
enableNdi = true
ndiDefaultSource = "YOUR-PC (OBS)"   # exact name from discovery logs
debugLogging = true                  # lists discovered sources in the log
```

3. **Per-wall override:** use the in-game GUI, or set the origin block's `sourceId` to `ndi:YOUR-PC (OBS)` via NBT.

**Source resolution order:** wall `sourceId` → `ndiDefaultSource` → first discovered source (if `ndiAutoSelectFirst = true`) → test pattern.

> NDI usage is subject to the [NDI SDK license terms](https://ndi.video). Devolay is bundled via **jarJar** (`lumavision-*-all.jar`) and loaded through Forge's `minecraftLibrary` in development.

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

### Media server architecture

LumaVision is designed as a **media server**, not an NDI-only mod. The configuration GUI and texture pipeline talk to a central catalog; individual backends are pluggable providers.

```
┌──────────────┐
│  ScreenConfigScreen  │  catalog-only; no backend imports
└──────┬───────┘
       │  list providers / sources / options
       ▼
┌──────────────────────┐
│  VideoSourceCatalog  │  ClientVideoSourceCatalog
└──────────┬───────────┘
           │ aggregates
     ┌─────┴─────┬─────────┬──────────┬─────────┐
     ▼           ▼         ▼          ▼         ▼
 NdiProvider  File…    Gif…     Browser…   TestPattern…
     │           (stubs)                    (fallback)
     ▼
VideoSourceDescriptor
     ▼
 VideoSource → VideoFrame → DynamicTextureHandle → ScreenRenderer
```

| Layer | Package | Role |
|-------|---------|------|
| `VideoSourceProvider` | `fr.lumavision.video.provider` | Discovers sources, creates `VideoSource`, exposes GUI metadata |
| `VideoSourceCatalog` | `fr.lumavision.video.provider` | Aggregates providers; resolves wall bindings |
| `ClientVideoSourceCatalog` | `fr.lumavision.client.video.catalog` | Client registry of all providers |
| `NdiProvider` | `fr.lumavision.client.ndi` | NDI discovery + Devolay capture |
| `VideoSource` | `fr.lumavision.video` | Produces frames from any media backend |
| `VideoFrame` | `fr.lumavision.video` | ARGB pixel buffer for a single frame |
| `DynamicTextureHandle` | `fr.lumavision.client.texture` | Uploads frames to a Minecraft `DynamicTexture` |
| `ScreenConfigScreen` | `fr.lumavision.client.gui` | In-game provider/source picker (catalog-only) |
| `SetScreenSourcePacket` | `fr.lumavision.network` | Server-side `sourceId` persistence |
| `ScreenTextureManager` | `fr.lumavision.client.texture` | One pipeline per merged wall (tick, cleanup) |
| `ScreenRenderer` | `fr.lumavision.client.render` | Draws the bezel + display quad in world space |

**Registered providers (v0.1):** NDI (implemented), Test Pattern (fallback), File / GIF / Image / Browser / Webcam / Network / Spout / Syphon / Screen Capture (registered stubs for future work).

### Rendering pipeline

The renderer is intentionally **decoupled** from content producers. Adding MP4, GIF, or browser capture means implementing a `VideoSourceProvider` — not rewriting the screen renderer.

```
┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐    ┌────────────────┐
│ VideoSource │ ─► │ VideoFrame  │ ─► │ DynamicTextureHandle │ ─► │ ScreenRenderer │
└─────────────┘    └─────────────┘    └─────────────────────┘    └────────────────┘
       ▲                                                                    │
       │                                                                    ▼
  VideoSourceCatalog                                              LED Screen block
  └── NdiProvider, FileProvider, …                                 (BlockEntityRenderer)
```

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
    │   │   ├── VideoSourceDescriptor.java
    │   │   ├── VideoSourceFactory.java
    │   │   ├── VideoFrame.java
    │   │   └── provider/                   # VideoSourceProvider + VideoSourceCatalog
    │   │
    │   ├── client/
    │   │   ├── LumaVisionClientMod.java
    │   │   ├── ndi/
    │   │   │   ├── NdiProvider.java
    │   │   │   ├── NdiDiscoveryService.java
    │   │   │   └── NdiVideoSource.java
    │   │   ├── video/
    │   │   │   ├── catalog/
    │   │   │   │   └── ClientVideoSourceCatalog.java
    │   │   │   ├── provider/               # Per-backend providers (+ stubs)
    │   │   │   └── TestPatternVideoSource.java
    │   │   ├── gui/
    │   │   │   ├── ScreenConfigMenu.java
    │   │   │   ├── ScreenConfigScreen.java
    │   │   │   └── SourceSelectionList.java
    │   │   ├── texture/
    │   │   │   ├── DynamicTextureHandle.java
    │   │   │   ├── FrameHasher.java
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
    │   │   ├── ModMenuTypes.java
    │   │   └── ModCreativeTabs.java
    │   │
    │   └── network/
    │       ├── ModNetworking.java
    │       └── SetScreenSourcePacket.java
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
| `debugLogging` | `false` | Verbose logs (textures, NDI discovery, rendering) |
| `maxTextureResolution` | `1024` | Max longest side for dynamic screen textures (64–4096) |
| `enableNdi` | `false` | Enable NDI input on the client |
| `ndiDefaultSource` | `""` | Global fallback NDI source name |
| `ndiAutoSelectFirst` | `false` | Use first discovered NDI source when nothing else is set |
| `ndiReceiveTimeoutMs` | `5` | NDI frame receive timeout (ms) |
| `ndiDiscoveryIntervalMs` | `2000` | NDI network scan interval (ms) |
| `baseCellResolution` | `96` | Pixels per LED block before max resolution cap |
| `maxTextureUpdatesPerSecond` | `20` | Max GPU texture uploads per second per wall (0 = unlimited) |

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

- Keep the **video pipeline abstraction** intact — new media types implement `VideoSourceProvider`, not renderer hacks.
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
| **Inspiration** | Stage & concert setups in Minecraft |
| **NDI** | [Devolay](https://github.com/WalkerKnapp/devolay) by Walker Knapp |

---

<div align="center">

**LumaVision** — *Light up your builds.*

</div>
