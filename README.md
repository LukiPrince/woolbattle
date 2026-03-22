# WoolBattle Plugin

Fork of [niklasmoell/woolbattle](https://github.com/niklasmoell/woolbattle), extended for modern Paper servers with ultimates, passive perks, and improved lobby flow.

- Minecraft: `1.21.11`
- Java: `21+`
- License: `MIT`

## What It Is

WoolBattle is a fast team minigame centered around wool economy:

- break wool to gain resources
- spend wool to use perks
- drain enemy team lives to win
- falling into void costs lives

## Core Features

- 4-team gameplay (Red, Blue, Green, Yellow)
- active + passive perk loadouts
- selectable ultimates
- map-specific configuration
- MongoDB persistence (stats, perks, achievements)
- custom model data support for perk textures

## Perks And Ultimates

- Active perks: movement, mobility, control, utility and combat tools
- Passive perks: always-on gameplay modifiers
- Ultimates include:
  - Time Anchor
  - Gravity Core
  - Perk Hijack
  - Mirror Avatar
  - Chain Mark
  - Overclock
  - Minigun

Ultimate charging is intentionally combat-focused:

- passive charge is slow
- extra charge on combat hits (e.g. arrow hits, Bow/Shears melee)

## Quick Start

1. Build or download latest jar.
2. Put jar into `plugins/`.
3. Start server once to generate `plugins/WoolBattle/config.json`.
4. Set MongoDB connection in config.
5. Select map (`/setmap Splend` or `/setmap Vimo`).
6. Start match with `/gstart`.

Ready-to-run server repo: [LukiPrince/woolbattle-server](https://github.com/LukiPrince/woolbattle-server)

## Commands

- `/gstart` start game
- `/gstop` stop game
- `/setmap <name>` switch map
- `/stats [player]` show stats
- `/mapblocks` map block tools
- `/blockregistration` register wool blocks
- `/mapdefine` define map areas

## Build

```bash
mvn -DskipTests clean package
```

Generated plugin jar is placed in `target/`.

## License

See [LICENSE](LICENSE).
