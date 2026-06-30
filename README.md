# LumaVision

Mod Minecraft **Forge 1.20.1** dédié aux écrans LED et à la diffusion de contenu vidéo dans le jeu.

Indépendant de TheatricalExtraLights — ce dépôt pose les fondations pour une architecture évolutive.

## État actuel (v0.1.0)

- Projet Gradle + Forge 47.3.0
- Package `fr.lumavision`
- Registres prêts : blocs, items, block entities, onglet créatif
- Configuration commune (`lumavision-common.toml`)
- Hook réseau préparé (sans packets pour l'instant)

**Aucune fonctionnalité vidéo** n'est implémentée à ce stade.

## Roadmap

| Phase | Contenu |
|-------|---------|
| 1 | Écrans LED (blocs + block entities) |
| 2 | Textures dynamiques et rendu client |
| 3 | Lecture vidéo / images |
| 4 | Support NDI (Devolay ou équivalent) |
| 5 | Synchronisation réseau multi-joueur |
| 6 | Optimisations de rendu |

## Prérequis

- **JDK 17**
- Gradle (wrapper inclus)

## Compilation

```bash
./gradlew build
```

Le JAR se trouve dans `build/libs/`.

## Développement

```bash
./gradlew runClient
./gradlew runServer
```

## Structure du code

```
src/main/java/fr/lumavision/
├── LumaVisionMod.java          # Point d'entrée
├── LumaVisionModClient.java    # Setup client + réseau
├── config/
│   └── ModConfig.java          # Configuration Forge
├── registry/
│   ├── ModBlocks.java
│   ├── ModItems.java
│   ├── ModBlockEntities.java
│   └── ModCreativeTabs.java
└── network/
    └── ModNetworking.java      # Packets (à venir)
```

## Licence

MIT
