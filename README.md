<h1 align="center">
  <br>
  <img src="images/sheep.png" alt="WoolBattle logo" width="160"/>
  <br>
  WoolBattle Plugin
</h1>

<p align="center">
  Fork of <a href="https://github.com/niklasmoell/woolbattle">niklasmoell/woolbattle</a>, extended for modern Paper servers with ultimates, passive perks, and improved lobby flow.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/minecraft-1.21.11-brightgreen?style=for-the-badge" alt="minecraft version">
  <img src="https://img.shields.io/badge/java-21%2B-orange?style=for-the-badge" alt="java version">
  <img src="https://img.shields.io/badge/license-MIT-blue?style=for-the-badge" alt="license">
</p>

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
- embedded SQLite persistence (stats, perks, achievements) — no database server required
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
3. Start server once to generate `plugins/WoolBattle/config.json` and the SQLite database.
4. (Optional) Adjust the `database` path in config — defaults to `plugins/WoolBattle/woolbattle.db`, created automatically. No database server needed.
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
