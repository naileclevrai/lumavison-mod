# LumaVision — Architecture multijoueur

Ce document décrit le modèle multiplayer actuel, les fondations ownership, et les options d'évolution vers un vrai serveur média synchronisé.

## Modèle actuel

```
Serveur (autoritaire)          Clients (capture locale)
─────────────────────          ────────────────────────
sourceId (NBT)        ──────►  ClientVideoSourceCatalog.resolve()
displaySettings (NBT) ──────►  DisplayUvMapper + DisplayColorGrading
ownerUuid (NBT)       ──────►  ScreenWallPermissions (GUI + packets)
membership mur        ──────►  ScreenRenderer (UV par cellule)
                               NDI / TestPattern capture locale
```

Le serveur Minecraft est **source de vérité pour la configuration** : chaque mur LED fusionné stocke sur son bloc origine :

- `sourceId` — identifiant logique de la source (`ndi:OBS`, `test`, …)
- `ScreenDisplaySettings` — rotation, miroir, mode Fit/Fill/Stretch, luminosité, contraste, gamma, température
- `ownerUuid` — joueur qui a posé le bloc (contrôle des modifications)

La sync utilise le mécanisme vanilla `ClientboundBlockEntityDataPacket` après chaque changement validé côté serveur.

## Séparation pipeline vidéo / affichage

| Couche | Rôle |
|--------|------|
| `VideoSource` | Produit des frames brutes (NDI, test pattern, …) |
| `ScreenTextureManager` | Tick source, grading couleur optionnel, upload GPU |
| `ScreenRenderer` | UV (fit/rotation/miroir) + couleur sommet |
| `LedScreenBlockEntity` | Persiste config mur sur l'origine |

Les réglages d'affichage **ne modifient jamais** les classes `VideoSource` / `NdiVideoSource`.

## Ownership (sender)

Le joueur qui **pose** un écran LED devient propriétaire (`claimOwnership` au placement).

Règles :

- Seul le propriétaire (ou OP niveau 2 / créatif) peut envoyer `SetScreenSourcePacket` et `SetScreenDisplayPacket`
- Les autres joueurs peuvent ouvrir la GUI en **lecture seule**
- L'`ownerUuid` est stocké sur le bloc ; seul le bloc **origine du mur** fait foi

Limitation connue : si deux murs fusionnent et que l'origine change, l'owner reste celui du bloc à la position origine après rebuild. Une migration d'ownership lors des fusions est prévue pour une version ultérieure.

## Config sync ≠ pixel sync

Aujourd'hui, deux clients avec le même `sourceId` (`ndi:MaSource`) **peuvent afficher des images différentes** :

- NDI est capturé **localement** sur chaque machine
- La config NDI client (`enableNdi`, réseau) peut diverger
- Une source peut être indisponible sur un client distant

La Phase 1+2 garantit que **tous les joueurs voient la même configuration** (source + affichage), pas nécessairement les mêmes pixels.

## Options d'évolution

### Option A — Config only (actuel amélioré)

| | |
|---|---|
| **Principe** | Serveur sync config ; chaque client capture localement |
| **Avantages** | Simple, zéro bande passante vidéo extra |
| **Inconvénients** | Pixels peuvent diverger |
| **Quand** | LAN homogène, tous les clients ont accès aux sources NDI |

### Option B — Sender relay

| | |
|---|---|
| **Principe** | Seul le client owner capture ; frames compressées → serveur → fan-out clients |
| **Avantages** | Vrai pixel sync ; viewers sans NDI local |
| **Inconvénients** | Bande passante serveur, latence, encodage CPU |
| **Quand** | Owner a la source, spectateurs distants |

### Option C — Server media host

| | |
|---|---|
| **Principe** | Runtime NDI/média sur la machine serveur dédiée |
| **Avantages** | Centralisé, indépendant des joueurs connectés |
| **Inconvénients** | NDI headless complexe ; serveur doit accéder au réseau AV |
| **Quand** | Infrastructure dédiée type régie |

**Recommandation** : Option A en production immédiate ; concevoir l'interface `ScreenFrameProvider` pour brancher B sans refactor majeur.

```java
// fr.lumavision.video.ScreenFrameProvider
Optional<VideoFrame> pollFrame(BlockPos groupOrigin);
// Aujourd'hui : capture locale via ScreenTextureManager
// Demain : RelayedFrameProvider (frames serveur)
```

## Paquets réseau

| Paquet | Direction | Statut |
|--------|-----------|--------|
| `SetScreenSourcePacket` | C→S | Implémenté + check owner |
| `SetScreenDisplayPacket` | C→S | Implémenté + check owner |
| `ScreenWallSnapshotPacket` | S→C | Futur (optionnel si NBT suffit) |
| `ScreenFramePacket` | C→S→C | Futur (Option B) |
| `ScreenControlLockPacket` | S→C | Futur (notifier owner dans GUI) |

Protocole réseau : version `2` (ajout `SetScreenDisplayPacket`).

## Roadmap

1. **Fait** — Display settings + ownership + permissions packets
2. **Court terme** — Migration owner lors fusion de murs ; snapshot S→C au join
3. **Moyen terme** — Prototype Option B (relay frames owner → serveur → clients)
4. **Long terme** — Évaluer Option C si besoin serveur dédié headless

## Test plan multijoueur

- [ ] Joueur A pose mur, configure NDI → Joueur B voit même sourceId et display settings
- [ ] Joueur B ne peut pas Apply (lecture seule)
- [ ] Joueur A change luminosité → B voit le changement après sync BE
- [ ] OP peut modifier n'importe quel mur
- [ ] Reconnexion : config persistée dans le monde
