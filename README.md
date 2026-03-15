<h1 align="center">
  <br>
  <img src="images/sheep.png" alt="logo" width="160"/>
  <br>
  WoolBattle Plugin
  <br>
</h1>

<h4 align="center">Fork of <a href="https://github.com/niklasmoell/woolbattle">niklasmoell/woolbattle</a> — extended with ultimates, passive perks, and more</h4>

<p align="center">
  <img src="https://img.shields.io/badge/minecraft-1.21.11-brightgreen?style=for-the-badge" alt="minecraft version">
  <img src="https://img.shields.io/badge/made%20with-java-orange?style=for-the-badge" alt="language">
  <img src="https://img.shields.io/badge/license-MIT-blue?style=for-the-badge" alt="license">
</p>

<p align="center">
  <a href="#overview">Overview</a> •
  <a href="#features">Features</a> •
  <a href="#perks">Perks</a> •
  <a href="#ultimates">Ultimates</a> •
  <a href="#maps">Maps</a> •
  <a href="#setup">Setup</a> •
  <a href="#commands">Commands</a>
</p>

---

## Overview

WoolBattle is a Minecraft minigame where everything revolves around wool. Break wool to gain resources, spend wool to use perks, and eliminate enemy teams by depleting their lives. Maps float in the air — fall off and you lose a life.

This is a fork of the original [niklasmoell/woolbattle](https://github.com/niklasmoell/woolbattle) with significant extensions:

- **Selectable Ultimates** — each player chooses one of 6 ultimate abilities
- **Passive Perk System** — background effects that trigger automatically
- **Configurable perk textures** via custom model data
- **Per-map configuration**
- **Improved lobby UX** for perk and ultimate selection
- **Paper 1.21.11** support

A ready-to-use server setup is available at [LukiPrince/woolbattle-server](https://github.com/LukiPrince/woolbattle-server).

---

## Features

- Wool-based economy — everything costs wool, broken wool refills your supply
- Team-based gameplay with 4 teams (Red, Blue, Green, Yellow)
- Shared team lives — lose all lives and the team is eliminated
- Spawn protection after death and at game start
- Automatic block restoration when the map resets
- Inventory sorting system
- Achievement system (5 achievements)
- MongoDB persistence for stats, perks, and achievements
- Custom model data support for all perk items

---

## Perks

Each player selects **2 active perks** and **1 passive perk** before the game starts.

### Active Perks

| Perk | Cost | Cooldown | Description |
|------|------|----------|-------------|
| Ender Pearl | 5 | 5s | Teleport to where it lands |
| Rescue Platform | 15 | 25s | Place blocks below you in a cross pattern |
| Exchanger | 15 | 10s | Swap locations with another player |
| Jump Platform | 15 | 25s | Place a 3×3 bounce platform |
| Grappling Hook | 5 | 5s | Fishing rod-based fast movement |
| Home Teleport | 30 | 25s | Teleport back to your team spawn |
| Rescue Pod | 15 | 30s | Surround yourself with blocks in an emergency |
| Duel | 30 | 10s | Challenge another player to a 1v1 |
| Impulse Wave | 26 | 10s | Push nearby enemies back |
| Stasis Trap | 28 | 12s | Place an invisible proximity trap |
| Rescue Anchor | 40 | 12s | Save a location and teleport back to it |
| Smoke Grenade | 32 | 15s | Create fog and blind nearby enemies |
| Disarm Pulse | 55 | 20s | Silence enemy active perks for 4 seconds |
| Bridge Push | 35 | 10s | Build a short wool bridge forward |
| Egg | 0 | 1s | Throwable egg projectile |

### Passive Perks

| Perk | Effect |
|------|--------|
| Wool Duplication | Bonus wool when breaking wool blocks |
| Savings Fox | 20% chance active perks cost no wool |
| Anchor Boots | 12% knockback reduction |
| Home Advantage | +10% walk speed on own team's wool |
| Supply | +1 team wool every 5 seconds |
| Builder | Every 6th placed wool block is refunded |
| Rescue Instinct | Save from void fall once per life |
| Steadfast | 5% chance to completely negate knockback |
| Rebound | 5% chance to reflect knockback to attacker |

---

## Ultimates

Each player selects **1 ultimate** before the game. Ultimates charge at 2 per second during gameplay and can only be used at full charge (100).

| Ultimate | Description |
|----------|-------------|
| Time Anchor | After a short delay, jump back and release an impulse wave |
| Gravity Core | Pull enemies in, then launch them outward |
| Perk Hijack | Block an enemy's active perk for 12 seconds |
| Mirror Avatar | Spawn a movement clone that mimics your actions for 6 seconds |
| Chain Mark | Mark that jumps between nearby enemies |
| Overclock | Half perk costs and cooldowns for 8 seconds, then 3s overheat |

---

## Maps

Two maps are included in [`plugins/WoolBattle/maps/`](plugins/WoolBattle/maps/).

| Map | Spawn | Height Range | Wool per Break |
|-----|-------|--------------|----------------|
| Splend | 0, 100, 0 | 0 – 120 | 2 |
| Vimo | 0, 71, 28 | 0 – 100 | 2 |

Both maps support 4 teams with individual spawn points.

---

## Setup

### Requirements

- Java 21+
- [PaperMC](https://papermc.io/) 1.21.11
- MongoDB (local or [MongoDB Atlas](https://www.mongodb.com/atlas))

### Installation

1. Build the plugin or download the latest JAR from [Releases](https://github.com/LukiPrince/woolbattle/releases)
2. Drop the JAR into your server's `plugins/` folder
3. Start the server once to generate `plugins/WoolBattle/config.json`
4. Fill in your MongoDB connection string in `config.json`
5. Set the map with `/setmap Splend` or `/setmap Vimo`
6. Start the game with `/gstart`

Alternatively, clone [LukiPrince/woolbattle-server](https://github.com/LukiPrince/woolbattle-server) for a ready-to-use server with worlds and configs included.

### config.json

| Key | Description | Default |
|-----|-------------|---------|
| `mongodb` | MongoDB connection string | — |
| `mapName` | Map to load on startup | `"Splend"` |
| `defaultLives` | Lives per team | `10` |
| `teamSize` | Players per team | `2` |
| `spawnProtectionAtGameStart` | Seconds of protection at start | `15` |
| `spawnProtectionAfterDeath` | Seconds of protection after respawn | `5` |
| `startCooldown` | Seconds before game starts | `60` |
| `deathCooldown` | Seconds before respawn | `10` |
| `woolReplaceDelay` | Seconds before broken wool regenerates | `10` |
| `givenWoolAmount` | Wool given per block break | `1` |
| `maxStacks` | Max wool stacks in inventory | `3` |
| `perkCustomModelData` | Custom model data IDs for perk textures | `{}` |

---

## Commands

| Command | Description |
|---------|-------------|
| `/gstart` | Start the game immediately |
| `/gstop` | Stop the game immediately |
| `/setmap <name>` | Switch the active map |
| `/stats [player]` | View stats for yourself or another player |
| `/mapblocks` | Manage map blocks |
| `/blockregistration` | Register wool blocks |
| `/mapdefine` | Define map areas |

---

## License

MIT — see [LICENSE](LICENSE) for details.
