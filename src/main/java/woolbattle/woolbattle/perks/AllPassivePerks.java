/**
 MIT License

 Copyright (c) 2022-present SimsumMC, Servaturus and Flashtube

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package woolbattle.woolbattle.perks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import woolbattle.woolbattle.Cache;
import woolbattle.woolbattle.Config;
import woolbattle.woolbattle.Main;
import woolbattle.woolbattle.MapConfig;
import woolbattle.woolbattle.WoolHelper;
import woolbattle.woolbattle.lobby.LobbySystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static woolbattle.woolbattle.team.TeamSystem.findTeamDyeColor;
import static woolbattle.woolbattle.team.TeamSystem.getPlayerTeam;

public class AllPassivePerks {

    private static final String WOOL_DUPLICATION = "Wool Duplication";
    private static final String SPARFUCHS = "Sparfuchs";
    private static final String ANKERSTIEFEL = "Ankerstiefel";
    private static final String HEIMVORTEIL = "Heimvorteil";
    private static final String NACHSCHUB = "Nachschub";
    private static final String BAUMEISTER = "Baumeister";
    private static final String RETTUNGSINSTINKT = "Rettungsinstinkt";
    private static final String STANDHAFT = "Standhaft";
    private static final String RUECKPRALL = "Rueckprall";
    private static final String WOOL_ARCHER = "Wool Archer";

    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float HEIMVORTEIL_WALK_SPEED = 0.22f;
    private static final int BAUMEISTER_INTERVAL = 6;
    private static final long KNOCKBACK_CONTEXT_MS = 250L;

    private static final HashMap<UUID, Integer> baumeisterPlacements = new HashMap<>();
    private static final HashMap<UUID, Location> lastSafeLocations = new HashMap<>();
    private static final Set<UUID> rescueInstinctUsed = new HashSet<>();
    private static final HashMap<UUID, KnockbackContext> pendingKnockback = new HashMap<>();

    private static boolean passiveTasksStarted = false;

    private record KnockbackContext(UUID attackerId, long createdAtMillis) {}

    private static final PassivePerk<BlockEvent, BlockBreakEvent> woolMultiplication = new PassivePerk<BlockEvent, BlockBreakEvent>(
            new ItemStack(Material.WHITE_WOOL),
            Component.text(WOOL_DUPLICATION, NamedTextColor.AQUA),
            false,
            "Beim Abbau von Wolle erhaeltst du zusaetzliche Team-Wolle"
    ){
        @Override
        public <S extends Event, H extends S> void functionality(H event) {
            if(!(event instanceof BlockBreakEvent)) {
                return;
            }
            Player player = ((BlockBreakEvent) event).getPlayer();
            giveTeamWool(player, Config.givenWoolAmount);
        }
    };

    private static final PassivePerk<Event, Event> sparfuchs = new PassivePerk<Event, Event>(
            new ItemStack(Material.GOLD_INGOT),
            Component.text(SPARFUCHS, NamedTextColor.AQUA),
            false,
            "20% Chance: Aktive Perks kosten keine Wolle"
    ){};

    private static final PassivePerk<Event, Event> ankerstiefel = new PassivePerk<Event, Event>(
            new ItemStack(Material.IRON_BOOTS),
            Component.text(ANKERSTIEFEL, NamedTextColor.AQUA),
            true,
            "12% weniger eingehender Knockback"
    ){
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
            if(!LobbySystem.gameStarted || !(event.getEntity() instanceof Player victim)) {
                return;
            }

            Player attacker = null;
            if(event.getDamager() instanceof Player) {
                attacker = (Player) event.getDamager();
            }
            else if(event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player) {
                attacker = (Player) arrow.getShooter();
            }

            if(attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
                return;
            }

            pendingKnockback.put(victim.getUniqueId(), new KnockbackContext(attacker.getUniqueId(), System.currentTimeMillis()));
            cleanupKnockbackContext();
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onPlayerVelocity(PlayerVelocityEvent event) {
            if(!LobbySystem.gameStarted) {
                return;
            }

            Player victim = event.getPlayer();
            KnockbackContext context = pendingKnockback.remove(victim.getUniqueId());
            if(context == null) {
                return;
            }

            if(System.currentTimeMillis() - context.createdAtMillis() > KNOCKBACK_CONTEXT_MS) {
                return;
            }

            Vector incoming = event.getVelocity();
            if(incoming.lengthSquared() <= 0.0001) {
                return;
            }

            if(hasPassivePerk(victim, RUECKPRALL) && rollChance(0.05)) {
                event.setVelocity(new Vector(0, 0, 0));

                Player attacker = Bukkit.getPlayer(context.attackerId());
                if(attacker != null && attacker.isOnline()) {
                    Vector reflected = incoming.clone();
                    reflected.setX(-reflected.getX());
                    reflected.setZ(-reflected.getZ());
                    attacker.setVelocity(reflected);
                }
                return;
            }

            if(hasPassivePerk(victim, STANDHAFT) && rollChance(0.05)) {
                event.setVelocity(new Vector(0, 0, 0));
                return;
            }

            if(hasPassivePerk(victim, ANKERSTIEFEL)) {
                event.setVelocity(incoming.multiply(0.88));
            }
        }
    };

    private static final PassivePerk<Event, Event> heimvorteil = new PassivePerk<Event, Event>(
            new ItemStack(Material.LIME_WOOL),
            Component.text(HEIMVORTEIL, NamedTextColor.AQUA),
            true,
            "Auf eigener Team-Wolle bekommst du 10% Laufgeschwindigkeit"
    ){
        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onPlayerMove(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            if(!hasPassivePerk(player, HEIMVORTEIL) || !LobbySystem.gameStarted) {
                if(player.getWalkSpeed() != DEFAULT_WALK_SPEED) {
                    player.setWalkSpeed(DEFAULT_WALK_SPEED);
                }
                return;
            }

            float targetSpeed = isOnOwnTeamWool(player) ? HEIMVORTEIL_WALK_SPEED : DEFAULT_WALK_SPEED;
            if(player.getWalkSpeed() != targetSpeed) {
                player.setWalkSpeed(targetSpeed);
            }
        }
    };

    private static final PassivePerk<Event, Event> nachschub = new PassivePerk<Event, Event>(
            new ItemStack(Material.HOPPER),
            Component.text(NACHSCHUB, NamedTextColor.AQUA),
            false,
            "Alle 5 Sekunden +1 Team-Wolle, wenn dein Inventar Platz hat"
    ){};

    private static final PassivePerk<Event, Event> baumeister = new PassivePerk<Event, Event>(
            new ItemStack(Material.BRICK),
            Component.text(BAUMEISTER, NamedTextColor.AQUA),
            true,
            "Jede 6. platzierte Wolle wird direkt erstattet"
    ){
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockPlace(BlockPlaceEvent event) {
            if(!LobbySystem.gameStarted || !WoolHelper.isWool(event.getBlockPlaced().getType())) {
                return;
            }

            Player player = event.getPlayer();
            if(!hasPassivePerk(player, BAUMEISTER)) {
                return;
            }

            UUID uuid = player.getUniqueId();
            int placements = baumeisterPlacements.getOrDefault(uuid, 0) + 1;
            baumeisterPlacements.put(uuid, placements);

            if(placements % BAUMEISTER_INTERVAL == 0) {
                giveTeamWool(player, 1);
            }
        }
    };

    private static final PassivePerk<Event, Event> rettungsinstinkt = new PassivePerk<Event, Event>(
            new ItemStack(Material.TOTEM_OF_UNDYING),
            Component.text(RETTUNGSINSTINKT, NamedTextColor.AQUA),
            true,
            "Einmal pro Leben rettet dich ein Void-Fall zur letzten sicheren Position (1 Herz Schaden)"
    ){
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onPlayerMove(PlayerMoveEvent event) {
            if(!LobbySystem.gameStarted) {
                return;
            }

            Player player = event.getPlayer();
            if(!hasPassivePerk(player, RETTUNGSINSTINKT)) {
                return;
            }

            if(isSafeGround(player)) {
                lastSafeLocations.put(player.getUniqueId(), player.getLocation().clone());
            }

            if(player.getLocation().getY() > MapConfig.minHeight) {
                return;
            }

            UUID uuid = player.getUniqueId();
            if(rescueInstinctUsed.contains(uuid)) {
                return;
            }

            rescueInstinctUsed.add(uuid);

            Location rescueLocation = lastSafeLocations.get(uuid);
            if(rescueLocation == null || rescueLocation.getWorld() == null) {
                rescueLocation = getTeamSpawnLocation(player);
            }

            if(rescueLocation.getY() <= MapConfig.minHeight) {
                rescueLocation.setY(MapConfig.minHeight + 3.0);
            }

            player.teleport(rescueLocation);
            player.setFallDistance(0);
            player.setHealth(Math.max(1.0, player.getHealth() - 2.0));
            player.sendMessage(Component.text("Rettungsinstinkt hat dich vor dem Void gerettet!", NamedTextColor.GOLD));
        }
    };

    private static final PassivePerk<Event, Event> standhaft = new PassivePerk<Event, Event>(
            new ItemStack(Material.SHIELD),
            Component.text(STANDHAFT, NamedTextColor.AQUA),
            false,
            "5% Chance, eingehenden Knockback komplett zu negieren"
    ){};

    private static final PassivePerk<Event, Event> rueckprall = new PassivePerk<Event, Event>(
            new ItemStack(Material.AMETHYST_SHARD),
            Component.text(RUECKPRALL, NamedTextColor.AQUA),
            false,
            "5% Chance, Knockback abzuwehren und auf den Angreifer zurueckzugeben"
    ){};

    private static final PassivePerk<Event, Event> woolArcher = new PassivePerk<Event, Event>(
            new ItemStack(Material.BOW),
            Component.text(WOOL_ARCHER, NamedTextColor.AQUA),
            true,
            "No shears needed. You can mine wool with your bow."
    ){
        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onBlockDamage(BlockDamageEvent event) {
            if(!LobbySystem.gameStarted) {
                return;
            }

            Player player = event.getPlayer();
            if(!hasPassivePerk(player, WOOL_ARCHER)) {
                return;
            }

            ItemStack inMainHand = player.getInventory().getItemInMainHand();
            if(inMainHand == null || inMainHand.getType() != Material.BOW) {
                return;
            }

            if(!WoolHelper.isWool(event.getBlock().getType())) {
                return;
            }

            event.setInstaBreak(true);
        }
    };

    /**Method setting up the system of passive perks. Over the course of the method, instances of the passive perk are added to the HashMap of
     * passive perks in Cache.java and assigned to potential owners through a query
     * toward the db respectively
     * @author Servaturus
     */

    public static void load(){
        registerPerk(woolMultiplication);
        registerPerk(sparfuchs);
        registerPerk(ankerstiefel);
        registerPerk(heimvorteil);
        registerPerk(nachschub);
        registerPerk(baumeister);
        registerPerk(rettungsinstinkt);
        registerPerk(standhaft);
        registerPerk(rueckprall);
        registerPerk(woolArcher);

        registerPerkListener(ankerstiefel);
        registerPerkListener(heimvorteil);
        registerPerkListener(baumeister);
        registerPerkListener(rettungsinstinkt);
        registerPerkListener(woolArcher);

        if(!passiveTasksStarted) {
            passiveTasksStarted = true;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if(!LobbySystem.gameStarted) {
                        return;
                    }

                    for(Player player : Bukkit.getOnlinePlayers()) {
                        if(hasPassivePerk(player, NACHSCHUB)) {
                            giveTeamWool(player, 1);
                        }
                    }
                }
            }.runTaskTimer(Main.getInstance(), 100L, 100L);
        }
    }

    public static void assignPlayersToPerks(){
        HashMap<String, PassivePerk<? extends Event, ?>> passivePerks = Cache.getPassivePerks();

        baumeisterPlacements.clear();
        lastSafeLocations.clear();
        rescueInstinctUsed.clear();
        pendingKnockback.clear();

        HashMap<String, ArrayList<Player>> playersByPerk = new HashMap<>();
        for(String perkName : passivePerks.keySet()) {
            playersByPerk.put(perkName, new ArrayList<>());
        }

        for(Player player : Bukkit.getOnlinePlayers()) {
            Document document = Main.getStore().find("playerPerks", player.getUniqueId().toString());
            if(document == null) {
                continue;
            }

            Object passiveName = document.get("passive");
            if(!(passiveName instanceof String)) {
                continue;
            }

            ArrayList<Player> perkPlayers = playersByPerk.get((String) passiveName);
            if(perkPlayers != null) {
                perkPlayers.add(player);
            }
        }

        for(PassivePerk<? extends Event, ?> perk : passivePerks.values()){
            ArrayList<Player> players = playersByPerk.getOrDefault(perk.getName(), new ArrayList<>());
            perk.setPlayers(players);
        }

        for(Player player : Bukkit.getOnlinePlayers()) {
            if(player.getWalkSpeed() != DEFAULT_WALK_SPEED && !hasPassivePerk(player, HEIMVORTEIL)) {
                player.setWalkSpeed(DEFAULT_WALK_SPEED);
            }
        }

        Cache.setPassivePerks(passivePerks);

        cleanupPerkState();
    }

    public static boolean shouldSkipActivePerkWoolCost(Player player) {
        if(!LobbySystem.gameStarted || !hasPassivePerk(player, SPARFUCHS)) {
            return false;
        }

        return rollChance(0.20);
    }

    public static void resetPerLifeState(Player player) {
        UUID uuid = player.getUniqueId();
        rescueInstinctUsed.remove(uuid);
        lastSafeLocations.remove(uuid);
        pendingKnockback.remove(uuid);
    }

    private static void registerPerk(PassivePerk<? extends Event, ?> perk) {
        Integer customModelData = Config.getPerkCustomModelData(perk.getName());
        if(customModelData != null && customModelData > 0) {
            perk.setCustomModelData(customModelData);
        }
        perk.register();
    }

    private static void registerPerkListener(PassivePerk<? extends Event, ?> perk) {
        try {
            Bukkit.getPluginManager().registerEvents(perk, Main.getInstance());
        }
        catch (IllegalPluginAccessException ignored) {}
    }

    private static boolean hasPassivePerk(Player player, String perkName) {
        PassivePerk<? extends Event, ?> perk = Cache.getPassivePerks().get(perkName);
        return perk != null && perk.hasPlayer(player);
    }

    private static boolean rollChance(double chance) {
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    private static int getCurrentWoolAmount(Player player) {
        int amount = 0;
        for(ItemStack itemStack : player.getInventory().getContents()) {
            if(itemStack != null && WoolHelper.isWool(itemStack.getType())) {
                amount += itemStack.getAmount();
            }
        }
        return amount;
    }

    private static void giveTeamWool(Player player, int amount) {
        if(amount <= 0) {
            return;
        }

        int maxAmount = Config.maxStacks * 64;
        int existingAmount = getCurrentWoolAmount(player);
        if(existingAmount >= maxAmount) {
            return;
        }

        int woolAmountToGive = Math.min(amount, maxAmount - existingAmount);
        player.getInventory().addItem(new ItemStack(WoolHelper.getWoolMaterial(findTeamDyeColor(player)), woolAmountToGive));
    }

    private static void cleanupKnockbackContext() {
        long now = System.currentTimeMillis();
        pendingKnockback.entrySet().removeIf(entry -> now - entry.getValue().createdAtMillis() > KNOCKBACK_CONTEXT_MS);
    }

    private static boolean isOnOwnTeamWool(Player player) {
        Material ownWool = WoolHelper.getWoolMaterial(findTeamDyeColor(player));
        Location baseLocation = player.getLocation().clone().subtract(0, 1, 0);

        Block center = baseLocation.getBlock();
        if(center.getType() == ownWool) {
            return true;
        }

        for(int xOffset = -1; xOffset <= 1; xOffset++) {
            for(int zOffset = -1; zOffset <= 1; zOffset++) {
                Block nearby = baseLocation.clone().add(xOffset, 0, zOffset).getBlock();
                if(nearby.getType() == ownWool) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isSafeGround(Player player) {
        if(player.getLocation().getY() <= MapConfig.minHeight + 1) {
            return false;
        }

        Block blockUnderPlayer = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        return blockUnderPlayer.getType().isSolid();
    }

    private static Location getTeamSpawnLocation(Player player) {
        String team = getPlayerTeam(player, true);
        return switch (team) {
            case "Blue" -> MapConfig.blueLocation != null ? MapConfig.blueLocation.clone() : player.getWorld().getSpawnLocation();
            case "Red" -> MapConfig.redLocation != null ? MapConfig.redLocation.clone() : player.getWorld().getSpawnLocation();
            case "Green" -> MapConfig.greenLocation != null ? MapConfig.greenLocation.clone() : player.getWorld().getSpawnLocation();
            case "Yellow" -> MapConfig.yellowLocation != null ? MapConfig.yellowLocation.clone() : player.getWorld().getSpawnLocation();
            default -> player.getWorld().getSpawnLocation();
        };
    }

    private static void cleanupPerkState() {
        Set<UUID> onlinePlayerIds = new HashSet<>();
        for(Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayerIds.add(onlinePlayer.getUniqueId());
        }

        baumeisterPlacements.keySet().retainAll(onlinePlayerIds);
        lastSafeLocations.keySet().retainAll(onlinePlayerIds);
        rescueInstinctUsed.retainAll(onlinePlayerIds);
        pendingKnockback.keySet().retainAll(onlinePlayerIds);

        for(Map.Entry<UUID, KnockbackContext> entry : new HashMap<>(pendingKnockback).entrySet()) {
            if(!onlinePlayerIds.contains(entry.getValue().attackerId())) {
                pendingKnockback.remove(entry.getKey());
            }
        }
    }
}