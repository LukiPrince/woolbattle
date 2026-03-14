# WoolBattle Full Modernization Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Modernize the WoolBattle plugin by migrating all deprecated APIs to their modern replacements: Adventure Components, current MongoDB driver, Gson, and cleaned-up Maven config.

**Architecture:** Replace all string-based chat/display APIs (`ChatColor`, `setDisplayName(String)`, `broadcastMessage(String)`, etc.) with Adventure Component API. Replace `json-simple` with `Gson`. Update MongoDB driver from legacy 3.x to sync 5.x. The plugin has no tests; verification is `mvn clean compile`.

**Tech Stack:** Paper API 1.21.11, Adventure Components (bundled with Paper), Gson 2.13, MongoDB Java Driver Sync 5.3.x, Maven

---

## Migration Reference

### ChatColor → Adventure Component Mapping

| Old (ChatColor)          | New (Adventure)                                |
|--------------------------|------------------------------------------------|
| `ChatColor.RED`          | `NamedTextColor.RED`                           |
| `ChatColor.GREEN`        | `NamedTextColor.GREEN`                         |
| `ChatColor.BLUE`         | `NamedTextColor.BLUE`                          |
| `ChatColor.YELLOW`       | `NamedTextColor.YELLOW`                        |
| `ChatColor.GOLD`         | `NamedTextColor.GOLD`                          |
| `ChatColor.GRAY`         | `NamedTextColor.GRAY`                          |
| `ChatColor.WHITE`        | `NamedTextColor.WHITE`                         |
| `ChatColor.AQUA`         | `NamedTextColor.AQUA`                          |
| `ChatColor.DARK_BLUE`    | `NamedTextColor.DARK_BLUE`                     |
| `ChatColor.DARK_GREEN`   | `NamedTextColor.DARK_GREEN`                    |
| `ChatColor.DARK_RED`     | `NamedTextColor.DARK_RED`                      |
| `ChatColor.DARK_PURPLE`  | `NamedTextColor.DARK_PURPLE`                   |
| `ChatColor.LIGHT_PURPLE` | `NamedTextColor.LIGHT_PURPLE`                  |
| `ChatColor.BOLD`         | `Style.style(TextDecoration.BOLD)`             |
| `ChatColor.RESET`        | Not needed (Components scope styles naturally) |
| `"§6text"`               | `Component.text("text", NamedTextColor.GOLD)`  |

### API Method Replacements

| Old                                        | New                                              |
|--------------------------------------------|--------------------------------------------------|
| `ChatColor.X + "text"`                     | `Component.text("text", NamedTextColor.X)`       |
| `str1 + ChatColor.X + str2`               | `comp1.append(Component.text(str2, color))`      |
| `player.sendMessage(String)`               | `player.sendMessage(Component)`                  |
| `sender.sendMessage(String)`               | `sender.sendMessage(Component)`                  |
| `Bukkit.broadcastMessage(String)`          | `Bukkit.broadcast(Component)`                    |
| `meta.setDisplayName(String)`              | `meta.displayName(Component)`                    |
| `meta.getDisplayName()`                    | `meta.displayName()` (returns Component)         |
| `meta.setLore(List<String>)`              | `meta.lore(List<Component>)`                     |
| `player.getDisplayName()`                  | `player.getName()` (for plain-text contexts)     |
| `Bukkit.createInventory(h, s, String)`     | `Bukkit.createInventory(h, s, Component)`        |
| `AsyncPlayerChatEvent`                     | `io.papermc.paper.event.player.AsyncChatEvent`   |
| `event.getMessage()`                       | `PlainTextComponentSerializer.plainText().serialize(event.message())` |
| `setDurability()`                          | `event.setCancelled(true)`                       |

### Required Adventure Imports

```java
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
```

---

## Task 1: Update pom.xml Dependencies

**Files:**
- Modify: `pom.xml`

**Step 1: Update dependencies**

Replace the dependencies section with:

```xml
<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21.11-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.13.1</version>
    </dependency>
    <dependency>
        <groupId>org.mongodb</groupId>
        <artifactId>mongodb-driver-sync</artifactId>
        <version>5.3.1</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

Note: `json-simple` is removed entirely. `mongodb-driver` (legacy) is replaced with `mongodb-driver-sync`.

Also update `maven-shade-plugin` to `3.6.0` and `maven-compiler-plugin` to `3.14.0`.

**Step 2: Verify build resolves dependencies**

Run: `mvn clean compile`
Expected: BUILD SUCCESS (may have errors in Java files since we changed MongoDB artifact — those get fixed in later tasks)

**Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: update dependencies - Gson, MongoDB 5.x, shade plugin"
```

---

## Task 2: Migrate json-simple → Gson in Config.java and MapConfig.java

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/Config.java`
- Modify: `src/main/java/woolbattle/woolbattle/MapConfig.java`

**Step 1: Migrate Config.java**

Replace all `org.json.simple.*` imports with:
```java
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
```

Replace JSON parsing pattern:
```java
// Old:
JSONParser jsonParser = new JSONParser();
JSONObject json = (JSONObject) jsonParser.parse(new FileReader(CONFIG_FILE));
String value = (String) json.get("key");
long longValue = (long) json.get("longKey");

// New:
JsonObject json = JsonParser.parseReader(new FileReader(CONFIG_FILE)).getAsJsonObject();
String value = json.get("key").getAsString();
long longValue = json.get("longKey").getAsLong();
int intValue = json.get("intKey").getAsInt();
```

Note: `json-simple` returns `Long` for all numbers. Gson has explicit `getAsInt()`, `getAsLong()`, `getAsString()`. Read Config.java carefully to map each `json.get()` cast to the correct Gson getter.

**Step 2: Migrate MapConfig.java**

Same pattern as Config.java. Replace `org.json.simple.*` imports with Gson. Update all JSON parsing calls.

Additionally: MapConfig uses `JSONObject` for nested objects — use `json.getAsJsonObject("key")` for nested objects, `json.getAsJsonArray("key")` for arrays.

**Step 3: Migrate Main.java onEnable JSON parsing**

In `Main.java:83`, there's JSON parsing for map pre-loading:
```java
JSONObject json = (JSONObject) new JSONParser().parse(new FileReader(mapFile));
String gameWorld = (String) json.get("gameWorld");
```

Replace with:
```java
JsonObject json = JsonParser.parseReader(new FileReader(mapFile)).getAsJsonObject();
String gameWorld = json.get("gameWorld").getAsString();
```

Update imports in Main.java accordingly (remove `org.json.simple.*`, add `com.google.gson.*`).

**Step 4: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/Config.java src/main/java/woolbattle/woolbattle/MapConfig.java src/main/java/woolbattle/woolbattle/Main.java
git commit -m "refactor: migrate json-simple to Gson"
```

---

## Task 3: Fix MongoDB Driver Import Changes

**Files:**
- Modify: All files using MongoDB imports (Main.java, StatsSystem.java, AchievementSystem.java, AchievementUI.java, ItemSystem.java, BlockBreakingSystem.java, MapSystem.java, ActivePerk.java, PassivePerk.java, MapBlocksCommand.java, AllPassivePerks.java)

**Step 1: Update MongoDB imports**

The MongoDB Sync Driver 5.x changed the `MongoClientSettings` import:

```java
// Old (3.x):
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;

// New (5.x):
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
// These remain the same! The sync driver 5.x keeps the same package paths.
```

Most `com.mongodb.client.*` imports remain identical. The main change is the Maven artifact name (already done in Task 1). Verify each file compiles. If any import path changed, fix it.

Key files with MongoDB usage:
- `Main.java` — MongoClients.create(), MongoClientSettings
- `StatsSystem.java` — MongoCollection, Document, Updates, Filters
- `AchievementSystem.java` — MongoCollection, Document, Updates, Filters
- `AchievementUI.java` — MongoCollection, Document, Filters
- `ItemSystem.java` — MongoCollection, Document, Filters
- `BlockBreakingSystem.java` — MongoDatabase, Document, Filters
- `MapSystem.java` — Document, Filters
- `ActivePerk.java` — MongoCollection, Document, Filters
- `PassivePerk.java` — MongoCollection, MongoDatabase, Document
- `AllPassivePerks.java` — MongoCollection, MongoDatabase, MongoCursor, FindIterable
- `MapBlocksCommand.java` — BsonValue, Document, Filters

**Step 2: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add -u src/
git commit -m "refactor: update MongoDB driver imports for 5.x"
```

---

## Task 4: Fix Listener.java — setDurability and Other Deprecations

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/Listener.java`

**Step 1: Replace setDurability**

Line 152-153, replace:
```java
@EventHandler
public void onPlayerItemDamage(PlayerItemDamageEvent event) {
    event.getItem().setDurability(event.getItem().getType().getMaxDurability());
}
```

With:
```java
@EventHandler
public void onPlayerItemDamage(PlayerItemDamageEvent event) {
    event.setCancelled(true);
}
```

The original code tried to prevent item damage by resetting durability. Cancelling the event is the modern, correct approach.

**Step 2: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/Listener.java
git commit -m "fix: replace deprecated setDurability with event cancellation"
```

---

## Task 5: Migrate TeamSystem.java — Central to Many Files

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/team/TeamSystem.java`

This file is critical because `getTeamColour()` is used by LivesSystem, AllActivePerks, Base, and LobbySystem. The return type changes from `ChatColor` to `TextColor`.

**Step 1: Change getTeamColour return type and implementation**

Change `getTeamColour()` from returning `ChatColor` to `TextColor`:

```java
// Old:
public static ChatColor getTeamColour(String teamName) {
    return switch (teamName) {
        case "Blue" -> ChatColor.DARK_BLUE;
        ...
    };
}

// New:
public static TextColor getTeamColour(String teamName) {
    return switch (teamName) {
        case "Blue" -> NamedTextColor.DARK_BLUE;
        case "Green" -> NamedTextColor.GREEN;
        case "Yellow" -> NamedTextColor.YELLOW;
        case "Red" -> NamedTextColor.DARK_RED;
        default -> NamedTextColor.WHITE;
    };
}
```

**Step 2: Migrate all ChatColor usage in TeamSystem**

Replace all imports:
```java
// Remove:
import org.bukkit.ChatColor;

// Add:
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
```

Migrate all sendMessage, setDisplayName, setLore, createInventory calls. Example patterns:

```java
// Old:
teamLessPlayers.get(i).sendMessage(ChatColor.GRAY + "You didn't enter a team so you were put into team " + ChatColor.RED + "red" + ChatColor.GRAY + "!");

// New:
teamLessPlayers.get(i).sendMessage(
    Component.text("You didn't enter a team so you were put into team ", NamedTextColor.GRAY)
        .append(Component.text("red", NamedTextColor.RED))
        .append(Component.text("!", NamedTextColor.GRAY))
);
```

```java
// Old:
Inventory voting = Bukkit.createInventory(null, 27, ChatColor.YELLOW + "Team Selecting");

// New:
Inventory voting = Bukkit.createInventory(null, 27, Component.text("Team Selecting", NamedTextColor.YELLOW));
```

```java
// Old:
GlassMeta.setDisplayName(" ");

// New:
GlassMeta.displayName(Component.text(" "));
```

```java
// Old:
voteRedMeta.setDisplayName(ChatColor.RED + "Team Red");
voteRedMeta.setLore(voteRedLore); // List<String>

// New:
voteRedMeta.displayName(Component.text("Team Red", NamedTextColor.RED));
voteRedMeta.lore(voteRedLore); // Change voteRedLore to List<Component>
```

For lore lists, change from `ArrayList<String>` to `ArrayList<Component>`:
```java
// Old:
ArrayList<String> voteRedLore = new ArrayList<>();
voteRedLore.add(ChatColor.GRAY + ">> " + player.getDisplayName());

// New:
ArrayList<Component> voteRedLore = new ArrayList<>();
voteRedLore.add(Component.text(">> " + player.getName(), NamedTextColor.GRAY));
```

**Step 3: Verify build**

Run: `mvn clean compile`
Expected: Errors in files that call `getTeamColour()` expecting `ChatColor` — that's expected, fixed in following tasks.

**Step 4: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/team/TeamSystem.java
git commit -m "refactor: migrate TeamSystem to Adventure Components"
```

---

## Task 6: Migrate Base.java — AsyncPlayerChatEvent + broadcastMessage

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/base/Base.java`

**Step 1: Replace AsyncPlayerChatEvent with AsyncChatEvent**

```java
// Old:
import org.bukkit.event.player.AsyncPlayerChatEvent;

@EventHandler
public void onPlayerChat(AsyncPlayerChatEvent event) {
    event.setCancelled(true);
    if (LobbySystem.gameStarted) {
        Bukkit.broadcastMessage(TeamSystem.getTeamColour(...) + "[" + ... + "] " + event.getPlayer().getDisplayName() + ChatColor.GRAY + ": " + ChatColor.WHITE + event.getMessage());
    } else {
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + event.getPlayer().getDisplayName() + ChatColor.GRAY + ": " + ChatColor.WHITE + event.getMessage());
    }
}

// New:
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

@EventHandler
public void onPlayerChat(AsyncChatEvent event) {
    event.setCancelled(true);
    String message = PlainTextComponentSerializer.plainText().serialize(event.message());
    Player player = event.getPlayer();
    if (LobbySystem.gameStarted) {
        String team = TeamSystem.getPlayerTeam(player, false);
        TextColor teamColor = TeamSystem.getTeamColour(TeamSystem.getPlayerTeam(player, true));
        Bukkit.broadcast(
            Component.text("[" + team + "] " + player.getName(), teamColor)
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE))
        );
    } else {
        Bukkit.broadcast(
            Component.text(player.getName(), NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE))
        );
    }
}
```

**Step 2: Remove all ChatColor imports, add Adventure imports**

Remove `import org.bukkit.ChatColor;`. The rest of Base.java doesn't use ChatColor.

**Step 3: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/base/Base.java
git commit -m "refactor: migrate Base.java to AsyncChatEvent and Adventure Components"
```

---

## Task 7: Migrate LivesSystem.java

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/lives/LivesSystem.java`

**Step 1: Replace ChatColor with Adventure Components**

Replace all `ChatColor` imports with Adventure imports. Migrate all `sendMessage(String)` and `broadcastMessage(String)` calls.

Key patterns in this file:
- Kill messages with team colors using `getTeamColour()` (now returns `TextColor`)
- `player.getDisplayName()` → `player.getName()`
- `Bukkit.broadcastMessage(killMessage)` → `Bukkit.broadcast(killComponent)`

Build the kill/streak messages as Components:
```java
// Old:
String killMessage = ChatColor.GRAY + "The player " + TeamSystem.getTeamColour(team) + player.getDisplayName() + ChatColor.GRAY + " died.";
Bukkit.broadcastMessage(killMessage);

// New:
Component killMessage = Component.text("The player ", NamedTextColor.GRAY)
    .append(Component.text(player.getName(), TeamSystem.getTeamColour(team)))
    .append(Component.text(" died.", NamedTextColor.GRAY));
Bukkit.broadcast(killMessage);
```

**Step 2: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/lives/LivesSystem.java
git commit -m "refactor: migrate LivesSystem to Adventure Components"
```

---

## Task 8: Migrate LobbySystem.java

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/lobby/LobbySystem.java`

This is the largest file (~1000+ lines). Read the full file before making changes.

**Step 1: Replace imports**

Remove `import org.bukkit.ChatColor;`, add Adventure imports.

**Step 2: Migrate all chat/display API calls**

This file has:
- `broadcastMessage()` calls (line ~551)
- `setDisplayName()` on ItemMeta (multiple locations)
- `Enchantment.KNOCKBACK` in ItemMeta (lines 740, 846, 854, 958, 1017) — these are fine, Enchantment constants are not deprecated
- `ChatColor` in inventory item names
- `createInventory` with String title
- `ItemFlag.HIDE_ENCHANTS` usage — still valid
- `setLore()` on ItemMeta
- `player.sendMessage(String)` calls

Apply the same migration patterns from the reference table above. Convert all String-based chat to Component-based.

**Step 3: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/lobby/LobbySystem.java
git commit -m "refactor: migrate LobbySystem to Adventure Components"
```

---

## Task 9: Migrate AchievementUI.java + AchievementSystem.java

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/achievements/AchievementUI.java`
- Modify: `src/main/java/woolbattle/woolbattle/achievements/AchievementSystem.java`

**Step 1: Migrate AchievementUI.java**

This file has heavy use of:
- `createInventory(null, 27, ChatColor.GOLD + "Achievements")` → `createInventory(null, 27, Component.text("Achievements", NamedTextColor.GOLD))`
- `setDisplayName("§6Strategist")` → `displayName(Component.text("Strategist", NamedTextColor.GOLD))`
- `setLore(list)` with ChatColor strings → `lore(componentList)` with Component items
- Section sign color codes (`§c`, `§a`, `§6`) → use `NamedTextColor` equivalents

**Step 2: Migrate AchievementSystem.java**

Simpler — just a few `sendMessage(ChatColor.GREEN + "...")` calls. Also has the `broadcastMessage` for winner announcement (line 141).

**Step 3: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/achievements/
git commit -m "refactor: migrate achievements to Adventure Components"
```

---

## Task 10: Migrate ItemSystem.java + Perks (ActivePerk, PassivePerk, AllActivePerks)

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/itemsystem/ItemSystem.java`
- Modify: `src/main/java/woolbattle/woolbattle/perks/ActivePerk.java`
- Modify: `src/main/java/woolbattle/woolbattle/perks/PassivePerk.java`
- Modify: `src/main/java/woolbattle/woolbattle/perks/AllActivePerks.java`

**Step 1: Migrate ActivePerk.java**

The `setItemName(String name)` method internally calls `itemMeta.setDisplayName(name)`. Change to:
```java
// Old:
public ActivePerk setItemName(String name) {
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.setDisplayName(name);
    ...
}

// New:
public ActivePerk setItemName(Component name) {
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.displayName(name);
    ...
}
```

Also update `this.itemName` field. The `itemName` is currently a `String` — consider whether callers need it as String or Component. The field is used for slot caching in MongoDB (stored as plain text). Keep a `String itemName` for DB storage but accept `Component` for display.

**Step 2: Migrate PassivePerk.java**

Same pattern — `meta.setDisplayName(name)` → `meta.displayName(name)`. The constructor accepts a `String name` for display; change parameter to `Component`.

**Step 3: Migrate AllActivePerks.java**

Update all `setItemName(ChatColor.AQUA + "Name")` calls:
```java
// Old:
.setItemName(ChatColor.AQUA + "Shears")

// New:
.setItemName(Component.text("Shears", NamedTextColor.AQUA))
```

Also update `sendMessage` calls and the ChatColor usage in `onEntityDamageByEntity`.

**Step 4: Migrate ItemSystem.java**

- `meta.setDisplayName(ChatColor.AQUA + "Leather Boots")` → `meta.displayName(Component.text("Leather Boots", NamedTextColor.AQUA))`
- Same for all armor piece names and cooldown item name

**Step 5: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 6: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/itemsystem/ src/main/java/woolbattle/woolbattle/perks/
git commit -m "refactor: migrate item and perk systems to Adventure Components"
```

---

## Task 11: Migrate StatsSystem.java + StatsCommand.java

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/stats/StatsSystem.java`
- Modify: `src/main/java/woolbattle/woolbattle/stats/StatsCommand.java`

**Step 1: Migrate StatsSystem.java**

The `getPlayerStatsFormatted()` method returns a formatted String with ChatColors. Change return type to `Component`:

```java
// Old:
public static String getPlayerStatsFormatted(OfflinePlayer player) {
    return ChatColor.GRAY + "-= " + ChatColor.YELLOW + "Statistics from " + ...;
}

// New:
public static Component getPlayerStatsFormatted(OfflinePlayer player) {
    return Component.text("-= ", NamedTextColor.GRAY)
        .append(Component.text("Statistics from ", NamedTextColor.YELLOW))
        .append(Component.text(player.getName(), NamedTextColor.GOLD))
        ...;
}
```

**Step 2: Migrate StatsCommand.java**

Update `sendMessage()` calls to use Components.

**Step 3: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/stats/
git commit -m "refactor: migrate stats to Adventure Components"
```

---

## Task 12: Migrate Command Files

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/lobby/StartGameCommand.java`
- Modify: `src/main/java/woolbattle/woolbattle/lobby/StopGameCommand.java`
- Modify: `src/main/java/woolbattle/woolbattle/lobby/SetMapCommand.java`
- Modify: `src/main/java/woolbattle/woolbattle/maprestaurationsystem/MapCommand.java`

**Step 1: Migrate all command files**

These are straightforward — each has `sendMessage(ChatColor.X + "message")` calls. Replace with Component versions.

SetMapCommand also has `Bukkit.broadcastMessage()` → `Bukkit.broadcast()`.

**Step 2: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/lobby/StartGameCommand.java src/main/java/woolbattle/woolbattle/lobby/StopGameCommand.java src/main/java/woolbattle/woolbattle/lobby/SetMapCommand.java src/main/java/woolbattle/woolbattle/maprestaurationsystem/MapCommand.java
git commit -m "refactor: migrate command files to Adventure Components"
```

---

## Task 13: Migrate Wool System Files

**Files:**
- Modify: `src/main/java/woolbattle/woolbattle/woolsystem/BlockBreakingSystem.java`
- Modify: `src/main/java/woolbattle/woolbattle/woolsystem/BlockRegistrationCommand.java`
- Modify: `src/main/java/woolbattle/woolbattle/woolsystem/MapBlocksCommand.java`

**Step 1: Migrate BlockBreakingSystem.java**

This file uses ChatColor for formatting debug/display strings (mapBlocks/removedBlocks toString methods). Migrate to Component-based formatting or use plain strings for internal debug output.

Since `mapBlocksToString()` and `removedBlocksToString()` are debug/display methods that return formatted strings for commands, change them to return `Component`.

**Step 2: Migrate BlockRegistrationCommand.java**

- `Bukkit.broadcastMessage("§a...")` → `Bukkit.broadcast(Component.text("...", NamedTextColor.GREEN))`
- `sender.sendMessage(ChatColor.X + "...")` → `sender.sendMessage(Component.text("...", color))`
- Replace section-sign codes (`§c`, `§a`, `§9`) with NamedTextColor equivalents

**Step 3: Migrate MapBlocksCommand.java**

Heavy sendMessage usage with ChatColor. Convert all to Component. This file has complex formatted output — build Components carefully.

**Step 4: Verify build**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git add src/main/java/woolbattle/woolbattle/woolsystem/
git commit -m "refactor: migrate wool system to Adventure Components"
```

---

## Task 14: Final Verification and Cleanup

**Step 1: Full clean build**

Run: `mvn clean compile -Dmaven.compiler.showDeprecation=true 2>&1 | grep -i "veraltet\|deprecated"`
Expected: No deprecation warnings related to ChatColor, setDisplayName, broadcastMessage, json-simple, or old MongoDB driver.

**Step 2: Verify no remaining old API usage**

Run grep searches:
```bash
grep -r "ChatColor" src/
grep -r "org.json.simple" src/
grep -r "setDisplayName" src/
grep -r "broadcastMessage" src/
grep -r "AsyncPlayerChatEvent" src/
grep -r "setDurability" src/
grep -r "setLore" src/  # should only find lore() calls now
```

All should return empty (or only comments/non-code references).

**Step 3: Build final JAR**

Run: `mvn clean package`
Expected: BUILD SUCCESS, JAR in target/

**Step 4: Commit any remaining fixes**

```bash
git add -u
git commit -m "chore: final cleanup after full modernization"
```

---

## Dependency Graph

```
Task 1 (pom.xml)
  ├── Task 2 (json-simple → Gson)
  ├── Task 3 (MongoDB imports)
  └── Task 4 (Listener.java setDurability)

Task 5 (TeamSystem) ← must be done before Tasks 6-8 because getTeamColour return type changes
  ├── Task 6 (Base.java)
  ├── Task 7 (LivesSystem)
  └── Task 8 (LobbySystem)

Task 10 (Perks + ItemSystem) ← independent of TeamSystem migration
Task 9 (Achievements) ← independent
Task 11 (Stats) ← independent
Task 12 (Commands) ← independent
Task 13 (Wool System) ← independent

Task 14 (Final verification) ← depends on all above
```

Tasks 2, 3, 4 can run in parallel after Task 1.
Tasks 6, 7, 8 can run in parallel after Task 5.
Tasks 9, 10, 11, 12, 13 can run in parallel (independent of each other, but need Task 1 done).
