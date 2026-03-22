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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bson.Document;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import woolbattle.woolbattle.WoolHelper;
import woolbattle.woolbattle.woolsystem.BlockBreakingSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import woolbattle.woolbattle.Cache;
import woolbattle.woolbattle.Config;
import woolbattle.woolbattle.Main;
import woolbattle.woolbattle.PlayerDataCache;
import woolbattle.woolbattle.lobby.LobbySystem;
import woolbattle.woolbattle.stats.StatsSystem;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static woolbattle.woolbattle.base.Base.addEnderPearl;
import static woolbattle.woolbattle.itemsystem.ItemSystem.setItemCooldown;
import static woolbattle.woolbattle.itemsystem.ItemSystem.subtractWool;
import static woolbattle.woolbattle.lives.LivesSystem.teleportPlayerTeamSpawn;
import static woolbattle.woolbattle.team.TeamSystem.*;

public class AllActivePerks implements Listener {

    private static final long GRAPPLING_DUPLICATE_WINDOW_MS = 200L;
    private static final long SILENCE_FEEDBACK_THROTTLE_MS = 750L;
    private static final long STASIS_TRAP_DURATION_MS = 18000L;
    private static final double STASIS_TRAP_TRIGGER_RADIUS_SQ = 1.44D;
    private static final long STASIS_OWNER_MARKER_INTERVAL_TICKS = 4L;
    private static final long RESCUE_ANCHOR_WINDOW_MS = 6000L;
    private static final long DISARM_PULSE_DURATION_MS = 4000L;
    private static final int ULTIMATE_MAX_CHARGE = 100;
    private static final int ULTIMATE_PASSIVE_CHARGE_AMOUNT = 1;
    private static final int ULTIMATE_PASSIVE_CHARGE_INTERVAL_SECONDS = 6;
    private static final int ULTIMATE_CHARGE_ON_ARROW_HIT = 6;
    private static final int ULTIMATE_CHARGE_ON_MELEE_BOW_OR_SHEARS_HIT = 4;
    private static final int ULTIMATE_HUD_FULL_LEVEL = 1;
    private static final long TIME_ANCHOR_DELAY_TICKS = 80L;
    private static final long HIJACK_DURATION_MS = 12000L;
    private static final long CHAIN_MARK_STEP_DELAY_TICKS = 14L;
    private static final long OVERCLOCK_DURATION_MS = 8000L;
    private static final long OVERCLOCK_OVERHEAT_MS = 3000L;
    private static final int GRAPPLING_STRAIGHT_FLIGHT_TICKS = 20;
    private static final double GRAPPLING_STRAIGHT_SPEED_MULTIPLIER = 1.25D;
    private static final double GRAPPLING_MIN_SPEED = 1.8D;
    private static final double GRAVITY_CORE_RANGE = 7.0;
    private static final long GRAVITY_CORE_PREVIEW_TIMEOUT_TICKS = 80L;
    private static final double GRAVITY_CORE_PREVIEW_RING_RADIUS = 0.9D;
    private static final int MIRROR_DURATION_TICKS = 120;
    private static final byte FULL_SKIN_LAYER_MASK = 0x7F;
    private static final String DEFAULT_ULTIMATE_NAME = "Zeitanker";
    private static final String MINIGUN_ARROW_METADATA = "wb_minigun_arrow";
    private static final int MINIGUN_BURSTS = 24;
    private static final int MINIGUN_BURST_INTERVAL_TICKS = 2;
    private static final int MINIGUN_ARROWS_PER_BURST = 3;

    public static final class UltimateDefinition {
        private final String displayName;
        private final Material iconMaterial;
        private final String description;

        private UltimateDefinition(String displayName, Material iconMaterial, String description) {
            this.displayName = displayName;
            this.iconMaterial = iconMaterial;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIconMaterial() {
            return iconMaterial;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final LinkedHashMap<String, UltimateDefinition> ULTIMATE_DEFINITIONS = new LinkedHashMap<>() {{
        put("Zeitanker", new UltimateDefinition("Zeitanker", Material.CLOCK, "Nach kurzer Zeit springst du zurueck und loest eine Impulswelle aus."));
        put("Gravitationskern", new UltimateDefinition("Gravitationskern", Material.HEART_OF_THE_SEA, "Zieht Gegner an und schleudert sie danach auseinander."));
        put("Perk-Hijack", new UltimateDefinition("Perk-Hijack", Material.AMETHYST_SHARD, "Sperrt einen gegnerischen aktiven Perk fuer kurze Zeit."));
        put("Spiegelavatar", new UltimateDefinition("Spiegelavatar", Material.ARMOR_STAND, "Erzeugt einen tauschend echten Bewegungs-Clone."));
        put("Kettenmarkierung", new UltimateDefinition("Kettenmarkierung", Material.IRON_BARS, "Markierung springt auf weitere Gegner in der Naehe ueber."));
        put("Overclock", new UltimateDefinition("Overclock", Material.REDSTONE, "Kurzzeitig halbierte Kosten/Cooldowns, danach Ueberhitzung."));
        put("Minigun", new UltimateDefinition("Minigun", Material.CROSSBOW, "Feuert kurzzeitig eine Salve aus vielen Pfeilen."));
    }};

    private static final HashMap<UUID, Long> lastGrapplingUse = new HashMap<>();
    private static final HashMap<UUID, Location> anchoredHookLocations = new HashMap<>();
    private static final HashMap<UUID, Long> silencedActivePerksUntil = new HashMap<>();
    private static final HashMap<UUID, Long> silenceFeedbackTimestamps = new HashMap<>();
    private static final HashMap<UUID, StasisTrap> stasisTraps = new HashMap<>();
    private static final HashMap<UUID, Location> rescueAnchorLocations = new HashMap<>();
    private static final HashMap<UUID, Long> rescueAnchorExpiresAt = new HashMap<>();
    private static final HashMap<UUID, Long> rescueAnchorCooldowns = new HashMap<>();
    private static final HashMap<UUID, Integer> ultimateCharges = new HashMap<>();
    private static final HashMap<UUID, Boolean> ultimateReadyNotified = new HashMap<>();
    private static final HashMap<UUID, String> selectedUltimateByPlayer = new HashMap<>();
    private static final HashMap<UUID, BukkitTask> timeAnchorTasks = new HashMap<>();
    private static final HashMap<UUID, BukkitTask> gravityCorePreviewTasks = new HashMap<>();
    private static final HashMap<UUID, Location> gravityCorePreviewCenters = new HashMap<>();
    private static final HashMap<UUID, Long> overclockActiveUntil = new HashMap<>();
    private static final HashMap<UUID, Long> overclockOverheatUntil = new HashMap<>();
    private static final HashMap<UUID, HashMap<String, Long>> blockedPerksByPlayer = new HashMap<>();
    private static final HashMap<UUID, Integer> syntheticArrowSlots = new HashMap<>();
    private static final HashMap<UUID, BukkitTask> minigunTasks = new HashMap<>();
    private static boolean ultimateHudTaskRunning = false;
    private static boolean ultimateSessionInitialized = false;
    private static int passiveChargeSecondCounter = 0;

    private static class StasisTrap {
        private final UUID trapId;
        private final UUID ownerId;
        private final String ownerTeam;
        private final Location location;
        private final long expiresAt;

        private StasisTrap(UUID trapId, UUID ownerId, String ownerTeam, Location location, long expiresAt) {
            this.trapId = trapId;
            this.ownerId = ownerId;
            this.ownerTeam = ownerTeam;
            this.location = location;
            this.expiresAt = expiresAt;
        }
    }

    private static class PacketMirrorDecoy {
        private final UUID profileId;
        private final int entityId;
        private final Object handle;
        private Location location;

        private PacketMirrorDecoy(UUID profileId, int entityId, Object handle, Location location) {
            this.profileId = profileId;
            this.entityId = entityId;
            this.handle = handle;
            this.location = location;
        }
    }

    public AllActivePerks() {
        startUltimateChargeTask();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(!LobbySystem.gameStarted) {
            return;
        }

        if(event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if(action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if(!isBowPerkItem(item)) {
            return;
        }

        if(hasAnyArrow(player)) {
            // Let vanilla shooting handle it when the player already has real arrows.
            return;
        }

        if (denyPerkUseIfSilenced(player) || denyPerkUseByUltimateStates(player, "Bow")) {
            event.setCancelled(true);
            return;
        }

        UUID playerId = player.getUniqueId();
        if (syntheticArrowSlots.containsKey(playerId)) {
            return;
        }

        int slot = findSyntheticArrowSlot(player);
        if (slot < 0) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("No free inventory slot for bow ammo.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.4f);
            return;
        }

        injectSyntheticArrow(player, slot);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> maintainSyntheticArrowForBow(player));
    }

    private void maintainSyntheticArrowForBow(Player player) {
        if (!player.isOnline() || !LobbySystem.gameStarted) {
            removeSyntheticArrow(player);
            return;
        }

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (!isBowPerkItem(mainHandItem) || hasAnyArrow(player)) {
            removeSyntheticArrow(player);
            return;
        }

        if (syntheticArrowSlots.containsKey(player.getUniqueId())) {
            return;
        }

        int slot = findSyntheticArrowSlot(player);
        if (slot < 0) {
            return;
        }

        injectSyntheticArrow(player, slot);
    }

    private boolean isBowPerkItem(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) {
            return false;
        }

        Component displayName = item.getItemMeta().displayName();
        if (displayName == null) {
            return false;
        }

        String itemName = PlainTextComponentSerializer.plainText().serialize(displayName);
        return "Bow".equals(itemName);
    }

    public static LinkedHashMap<String, UltimateDefinition> getUltimateDefinitions() {
        return new LinkedHashMap<>(ULTIMATE_DEFINITIONS);
    }

    public static boolean isUltimateName(String ultimateName) {
        return ultimateName != null && ULTIMATE_DEFINITIONS.containsKey(ultimateName);
    }

    private boolean hasAnyArrow(Player player) {
        PlayerInventory inventory = player.getInventory();
        for(ItemStack stack : inventory.getContents()) {
            if(stack == null) {
                continue;
            }

            Material material = stack.getType();
            if(material == Material.ARROW || material == Material.SPECTRAL_ARROW || material == Material.TIPPED_ARROW) {
                return true;
            }
        }

        return false;
    }

    private int findSyntheticArrowSlot(Player player) {
        PlayerInventory inventory = player.getInventory();

        for (int slot = 35; slot >= 9; slot--) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) {
                return slot;
            }
        }

        return -1;
    }

    private void injectSyntheticArrow(Player player, int slot) {
        player.getInventory().setItem(slot, new ItemStack(Material.ARROW, 1));
        syntheticArrowSlots.put(player.getUniqueId(), slot);
    }

    private void removeSyntheticArrow(Player player) {
        Integer slot = syntheticArrowSlots.remove(player.getUniqueId());
        if (slot == null) {
            return;
        }

        ItemStack stack = player.getInventory().getItem(slot);
        if (stack == null) {
            return;
        }

        if (stack.getType() == Material.ARROW && stack.getAmount() == 1) {
            player.getInventory().setItem(slot, null);
        }
    }

    public static String getDefaultUltimateName() {
        return DEFAULT_ULTIMATE_NAME;
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if(!(event.getEntity().getShooter() instanceof Player)){
            return;
        }

        Player player = (Player) projectile.getShooter();

        if (projectile instanceof Arrow arrow && arrow.hasMetadata(MINIGUN_ARROW_METADATA)) {
            return;
        }

        if (projectile.getType() == EntityType.ARROW) {
            removeSyntheticArrow(player);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> maintainSyntheticArrowForBow(player));
        }

        if(projectile instanceof FishHook hook && isHoldingGrapplingHook(player)) {
            if (denyPerkUseIfSilenced(player) || denyPerkUseByUltimateStates(player, "Grappling Hook")) {
                event.setCancelled(true);
                hook.remove();
                return;
            }

            launchHookStraight(player, hook);
            return;
        }

        if(projectile.getType() == EntityType.SNOWBALL || projectile.getType() == EntityType.ENDER_PEARL ||
                projectile.getType() == EntityType.ARROW || projectile.getType() == EntityType.EGG){
            String perkName;
            if(projectile.getType() ==  EntityType.SNOWBALL){
                perkName = "Exchanger";
            }
            else if(projectile.getType() == EntityType.ARROW){
                perkName = "Bow";
            }
            else if(projectile.getType() == EntityType.EGG){
                perkName = "Egg";
            }
            else{
                perkName = "Ender Pearl";
            }

            if (denyPerkUseIfSilenced(player) || denyPerkUseByUltimateStates(player, perkName)) {
                event.setCancelled(true);
                projectile.remove();
                return;
            }

            ActivePerk perk = Cache.getActivePerks().get(perkName);
            if (perk == null) {
                return;
            }

            ItemStack itemStack = perk.getItemStack();
            itemStack.setAmount(1);

            int woolCost = applyUltimateWoolCostModifier(player, perk.getWoolCost());
            int cooldown = applyUltimateCooldownModifier(player, perk.getCooldown());
            int perkSlot = perk.getSlotCache(player);

            if(!(projectile.getType() == EntityType.ARROW)) {
                ItemStack restoreItem = itemStack.clone();
                int restoreSlot = perkSlot;
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    player.getInventory().setItem(restoreSlot, restoreItem);
                });
            }

            if(!subtractWool(player, woolCost)){
                event.setCancelled(true);
                projectile.remove();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.4f);
            }
            else{
                if(cooldown != 0) {
                    setItemCooldown(player, perkSlot, itemStack, cooldown);
                    StatsSystem.addActivePerkUsage(player);
                }

                if(perkName.equals("Ender Pearl")){
                    addEnderPearl((EnderPearl) projectile);
                }

                if (projectile.getType() == EntityType.ARROW && projectile instanceof Arrow arrow) {
                    arrow.setCritical(false);
                    startArrowTrail(arrow, player);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof FishHook hook)) {
            return;
        }

        if (!(hook.getShooter() instanceof Player player)) {
            return;
        }

        ActivePerk perk = Cache.getActivePerks().get("Grappling Hook");
        if (perk == null || !isHoldingGrapplingHook(player)) {
            return;
        }

        Block hitBlock = event.getHitBlock();
        if (hitBlock == null) {
            return;
        }

        Location anchorLocation = resolveHookAnchorLocation(hitBlock, event.getHitBlockFace(), hook.getLocation());
        anchorHook(hook, anchorLocation);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof Player)){
            return;
        }
        Player damagedPlayer = (Player) event.getEntity();

        if(event.getDamager() instanceof Player){
            Player player = (Player) event.getDamager();
            Component itemDisplayName = player.getInventory().getItemInMainHand().hasItemMeta() ? player.getInventory().getItemInMainHand().getItemMeta().displayName() : null;
            String itemPlainName = itemDisplayName != null ? PlainTextComponentSerializer.plainText().serialize(itemDisplayName) : "";

            if(itemPlainName.equals("Bow") || itemPlainName.equals("Shears")) {
                addCombatUltimateCharge(player, damagedPlayer, ULTIMATE_CHARGE_ON_MELEE_BOW_OR_SHEARS_HIT);
            }

            if(itemPlainName.equals("Duel")){
                event.setCancelled(true);
                ActivePerk perk = Cache.getActivePerks().get("Duel");
                if (perk == null) {
                    return;
                }

                if (denyPerkUseIfSilenced(player)) {
                    return;
                }

                if (denyPerkUseByUltimateStates(player, "Duel")) {
                    return;
                }

                ItemStack itemStack = perk.getItemStack();
                itemStack.setAmount(1);
                int perkSlot = perk.getSlotCache(player);
                player.getInventory().setItem(perkSlot, itemStack);

                HashMap<Player, Player> playerDuels = Cache.getPlayerDuels();

                if(playerDuels.containsKey(damagedPlayer)){
                    Component damagedPlayerDuelName = Component.text(damagedPlayer.getName(),
                            getTeamColour(getPlayerTeam(playerDuels.get(damagedPlayer), true)));
                    player.sendMessage(Component.text("This player is already in a duel with ", NamedTextColor.RED)
                            .append(damagedPlayerDuelName).append(Component.text("!", NamedTextColor.RED)));
                    return;
                }

                if(playerDuels.containsKey(player)){
                    Component playerDuelName = Component.text(player.getName(),
                            getTeamColour(getPlayerTeam(playerDuels.get(player), true)));
                    player.sendMessage(Component.text("You are already in a duel with ", NamedTextColor.RED)
                            .append(playerDuelName).append(Component.text("!", NamedTextColor.RED)));
                    return;
                }

                int woolCost = applyUltimateWoolCostModifier(player, perk.getWoolCost());
                int cooldown = applyUltimateCooldownModifier(player, perk.getCooldown());

                if(!subtractWool(player, woolCost)){
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.4f);
                    return;
                }
                else{
                    setItemCooldown(player, perkSlot, itemStack, cooldown);

                    playerDuels.put(damagedPlayer, player);
                    playerDuels.put(player, damagedPlayer);

                    Cache.setPlayerDuels(playerDuels);

                    Component playerName = Component.text(player.getName(),
                            getTeamColour(getPlayerTeam(player, true)));
                    Component damagedPlayerName = Component.text(damagedPlayer.getName(),
                            getTeamColour(getPlayerTeam(damagedPlayer, true)));

                    player.sendMessage(Component.text("You are now in a duel with ", NamedTextColor.GOLD)
                            .append(damagedPlayerName).append(Component.text("!", NamedTextColor.GOLD)));
                    damagedPlayer.sendMessage(Component.text("You are now in a duel with ", NamedTextColor.GOLD)
                            .append(playerName).append(Component.text("!", NamedTextColor.GOLD)));

                    StatsSystem.addActivePerkUsage(player);
                }

            }
        }
        if(!(event.getDamager() instanceof Projectile)){
            return;
        }
        Projectile projectile = (Projectile) event.getDamager();
        if(projectile.getType() == EntityType.ARROW && projectile.getShooter() instanceof Player shooter) {
            addCombatUltimateCharge(shooter, damagedPlayer, ULTIMATE_CHARGE_ON_ARROW_HIT);
        }

        if(projectile.getType() == EntityType.SNOWBALL) {
            event.setCancelled(true);
            Player shooterPlayer;
            if (!(projectile.getShooter() instanceof Player)) {
                return;
            }
            shooterPlayer = (Player) projectile.getShooter();

            Player hitPlayer;

            hitPlayer = (Player) event.getEntity();

            Location hitPlayerLocation = hitPlayer.getLocation();
            Location shooterPlayerLocation = shooterPlayer.getLocation();

            shooterPlayer.teleport(hitPlayerLocation);
            hitPlayer.teleport(shooterPlayerLocation);

            projectile.remove();
        }
        if(projectile.getType() == EntityType.EGG) {
            Player player = (Player) event.getEntity();
            Vector velocity = player.getVelocity().multiply(2);
            player.setVelocity(velocity);
        }

    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (isHoldingGrapplingHook(player) && (denyPerkUseIfSilenced(player) || denyPerkUseByUltimateStates(player, "Grappling Hook"))) {
            event.setCancelled(true);
            clearHookAnchor(event.getHook());
            event.getHook().remove();
            return;
        }

        if (event.getState() != PlayerFishEvent.State.IN_GROUND && event.getState() != PlayerFishEvent.State.REEL_IN) {
            if (event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT) {
                clearHookAnchor(event.getHook());
            }
            return;
        }

        ActivePerk perk = Cache.getActivePerks().get("Grappling Hook");
        if (perk == null || !isHoldingGrapplingHook(player)) {
            return;
        }

        FishHook hook = event.getHook();
        Location hookLocation = getHookLocationForPull(hook);
        if (!isHookAnchored(hook, hookLocation) || isDuplicateGrapplingTrigger(player)) {
            return;
        }

        ItemStack itemStack = perk.getItemStack();
        int woolCost = applyUltimateWoolCostModifier(player, perk.getWoolCost());
        int cooldown = applyUltimateCooldownModifier(player, perk.getCooldown());
        int perkSlot = perk.getSlotCache(player);

        if(!subtractWool(player, woolCost)){
            event.setCancelled(true);
            clearHookAnchor(hook);
            hook.remove();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.4f);
            return;
        }

        if(cooldown != 0) {
            setItemCooldown(player, perkSlot, itemStack, cooldown);
        }

        pullPlayerTowardsHook(player, hookLocation);
        clearHookAnchor(hook);
        hook.remove();

        StatsSystem.addActivePerkUsage(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCustomPerkInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta.displayName() == null) {
            return;
        }

        String itemName = PlainTextComponentSerializer.plainText().serialize(itemMeta.displayName());

        if (!itemName.equals("Rettungsanker") && !itemName.equals("Stasisfalle")) {
            return;
        }

        ActivePerk perk = Cache.getActivePerks().get(itemName);
        if (perk == null) {
            return;
        }

        if (denyPerkUseByUltimateStates(event.getPlayer(), itemName)) {
            event.setCancelled(true);
            return;
        }

        if (itemName.equals("Rettungsanker")) {
            handleRescueAnchorUse(event, event.getPlayer(), perk);
            return;
        }

        handleStasisTrapUse(event, event.getPlayer(), perk);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!LobbySystem.gameStarted || player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // Reserve the F key for ultimate usage during active matches.
        event.setCancelled(true);

        int charge = getUltimateCharge(player);
        String selectedUltimate = getSelectedUltimateName(player);

        if ("Gravitationskern".equals(selectedUltimate)) {
            if (handleGravityCorePreviewInput(player, charge)) {
                return;
            }
        } else {
            stopGravityCorePreview(player.getUniqueId());
        }

        if (charge < ULTIMATE_MAX_CHARGE) {
            return;
        }

        if (activateSelectedUltimate(player)) {
            setUltimateCharge(player, 0);
        }
    }

    private boolean handleGravityCorePreviewInput(Player player, int charge) {
        UUID playerId = player.getUniqueId();
        BukkitTask previewTask = gravityCorePreviewTasks.get(playerId);

        if (previewTask != null) {
            Location previewCenter = gravityCorePreviewCenters.get(playerId);
            stopGravityCorePreview(playerId);

            if (charge < ULTIMATE_MAX_CHARGE) {
                return true;
            }

            if (previewCenter == null) {
                previewCenter = resolveGravityCoreCenter(player);
            }

            if (activateGravityCoreUltimate(player, previewCenter)) {
                setUltimateCharge(player, 0);
            }
            return true;
        }

        if (charge < ULTIMATE_MAX_CHARGE) {
            return true;
        }

        startGravityCorePreview(player);
        return true;
    }

    private void startGravityCorePreview(Player player) {
        UUID playerId = player.getUniqueId();
        stopGravityCorePreview(playerId);

        player.sendActionBar(Component.text("Gravitationskern Preview aktiv. Druecke F erneut zum Ausloesen.", NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.35f);

        BukkitTask previewTask = new BukkitRunnable() {
            long livedTicks = 0L;

            @Override
            public void run() {
                if (!LobbySystem.gameStarted || !player.isOnline() || player.getGameMode() != GameMode.SURVIVAL) {
                    stopGravityCorePreview(playerId);
                    cancel();
                    return;
                }

                if (getUltimateCharge(player) < ULTIMATE_MAX_CHARGE) {
                    stopGravityCorePreview(playerId);
                    cancel();
                    return;
                }

                Location center = resolveGravityCoreCenter(player);
                gravityCorePreviewCenters.put(playerId, center.clone());
                renderGravityCorePreview(player, center);

                livedTicks += 1L;
                if (livedTicks < GRAVITY_CORE_PREVIEW_TIMEOUT_TICKS) {
                    return;
                }

                player.sendActionBar(Component.text("Gravitationskern Preview beendet.", NamedTextColor.GRAY));
                stopGravityCorePreview(playerId);
                cancel();
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        gravityCorePreviewTasks.put(playerId, previewTask);
    }

    private void renderGravityCorePreview(Player player, Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        Location markerCenter = center.clone().add(0, 0.1, 0);
        Particle.DustOptions ringDust = new Particle.DustOptions(Color.fromRGB(88, 26, 124), 1.1f);
        Particle.DustOptions lineDust = new Particle.DustOptions(Color.fromRGB(28, 28, 36), 0.9f);

        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2.0 * i) / 8.0;
            double x = Math.cos(angle) * GRAVITY_CORE_PREVIEW_RING_RADIUS;
            double z = Math.sin(angle) * GRAVITY_CORE_PREVIEW_RING_RADIUS;
            Location ringPoint = markerCenter.clone().add(x, 0.02, z);
            world.spawnParticle(Particle.DUST, ringPoint, 1, 0.0, 0.0, 0.0, 0.0, ringDust);
        }

        world.spawnParticle(Particle.END_ROD, markerCenter.clone().add(0, 0.25, 0), 2, 0.04, 0.07, 0.04, 0.0);
        world.spawnParticle(Particle.DUST, markerCenter.clone().add(0, 0.45, 0), 2, 0.03, 0.08, 0.03, 0.0, ringDust);

        Location eye = player.getEyeLocation();
        Vector toCenter = markerCenter.toVector().subtract(eye.toVector());
        double distance = toCenter.length();
        if (distance <= 0.01) {
            return;
        }

        int points = Math.max(4, Math.min(12, (int) Math.ceil(distance * 1.2)));
        Vector step = toCenter.clone().multiply(1.0 / points);
        Location stepLocation = eye.clone();
        for (int i = 0; i < points; i++) {
            stepLocation.add(step);
            world.spawnParticle(Particle.DUST, stepLocation, 1, 0.0, 0.0, 0.0, 0.0, lineDust);
        }
    }

    private static void stopGravityCorePreview(UUID playerId) {
        if (playerId == null) {
            return;
        }

        BukkitTask previewTask = gravityCorePreviewTasks.remove(playerId);
        if (previewTask != null) {
            previewTask.cancel();
        }

        gravityCorePreviewCenters.remove(playerId);
    }

    private static void stopAllGravityCorePreviews() {
        for (BukkitTask previewTask : gravityCorePreviewTasks.values()) {
            if (previewTask != null) {
                previewTask.cancel();
            }
        }

        gravityCorePreviewTasks.clear();
        gravityCorePreviewCenters.clear();
    }

    private void anchorHook(FishHook hook, Location anchorLocation) {
        hook.teleport(anchorLocation);
        hook.setGravity(false);
        hook.setVelocity(new Vector(0, 0, 0));
        anchoredHookLocations.put(hook.getUniqueId(), anchorLocation.clone());
    }

    private void launchHookStraight(Player player, FishHook hook) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double launchSpeed = Math.max(GRAPPLING_MIN_SPEED, hook.getVelocity().length() * GRAPPLING_STRAIGHT_SPEED_MULTIPLIER);

        hook.setGravity(false);
        hook.setVelocity(direction.clone().multiply(launchSpeed));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!hook.isValid() || hook.isDead() || anchoredHookLocations.containsKey(hook.getUniqueId()) || ticks >= GRAPPLING_STRAIGHT_FLIGHT_TICKS) {
                    cancel();
                    return;
                }

                // Keep the hook trajectory straight for the first moments after launch.
                hook.setGravity(false);
                hook.setVelocity(direction.clone().multiply(launchSpeed));
                ticks++;
            }
        }.runTaskTimer(Main.getInstance(), 1L, 1L);
    }

    private Location resolveHookAnchorLocation(Block hitBlock, BlockFace hitFace, Location fallback) {
        Location anchorLocation = hitBlock.getLocation().add(0.5, 0.5, 0.5);
        if (hitFace != null) {
            Vector offset = hitFace.getDirection().multiply(0.5);
            anchorLocation.add(offset);
        }

        World world = fallback.getWorld();
        if (world == null) {
            return fallback;
        }

        anchorLocation.setWorld(world);
        return anchorLocation;
    }

    private Location getHookLocationForPull(FishHook hook) {
        Location anchoredLocation = anchoredHookLocations.get(hook.getUniqueId());
        if (anchoredLocation != null) {
            return anchoredLocation.clone();
        }

        return hook.getLocation();
    }

    private boolean isHoldingGrapplingHook(Player player) {
        return hasDisplayName(player.getInventory().getItemInMainHand(), "Grappling Hook")
                || hasDisplayName(player.getInventory().getItemInOffHand(), "Grappling Hook");
    }

    private boolean hasDisplayName(ItemStack itemStack, String displayName) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }

        Component itemDisplayName = itemStack.getItemMeta().displayName();
        if (itemDisplayName == null) {
            return false;
        }

        String plainName = PlainTextComponentSerializer.plainText().serialize(itemDisplayName);
        return plainName.equals(displayName);
    }

    private boolean isHookAnchored(FishHook hook, Location hookLocation) {
        if (anchoredHookLocations.containsKey(hook.getUniqueId())) {
            return true;
        }

        Block centerBlock = hookLocation.getBlock();
        if (centerBlock.getType().isSolid()) {
            return true;
        }

        World world = hookLocation.getWorld();
        if (world == null) {
            return false;
        }

        int blockX = hookLocation.getBlockX();
        int blockY = hookLocation.getBlockY();
        int blockZ = hookLocation.getBlockZ();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    Block nearbyBlock = world.getBlockAt(blockX + x, blockY + y, blockZ + z);
                    if (nearbyBlock.getType().isSolid()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void clearHookAnchor(FishHook hook) {
        if (hook == null) {
            return;
        }

        anchoredHookLocations.remove(hook.getUniqueId());
    }

    private boolean isDuplicateGrapplingTrigger(Player player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        Long lastUse = lastGrapplingUse.get(playerId);

        if (lastUse != null && now - lastUse < GRAPPLING_DUPLICATE_WINDOW_MS) {
            return true;
        }

        lastGrapplingUse.put(playerId, now);
        return false;
    }

    private void pullPlayerTowardsHook(Player player, Location hookLocation) {
        Vector pullVector = hookLocation.toVector().subtract(player.getLocation().toVector());
        double distance = pullVector.length();
        if (distance < 0.2) {
            return;
        }

        double horizontalStrength = Math.min(2.3, 0.8 + (distance * 0.15));
        double verticalBoost = Math.min(1.15, 0.35 + (distance * 0.08));

        Vector velocity = pullVector.normalize().multiply(horizontalStrength);
        velocity.setY(Math.max(velocity.getY() + verticalBoost, 0.25));

        player.setFallDistance(0f);
        player.setVelocity(velocity);
    }

    private static void startArrowTrail(Arrow arrow, Player shooter) {
        Color teamColor = resolveArrowTrailColor(shooter);
        Particle.DustOptions trailDust = new Particle.DustOptions(teamColor, 1.25f);

        new BukkitRunnable() {
            int ticksLived = 0;

            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead() || arrow.isInBlock() || ticksLived > 240) {
                    cancel();
                    return;
                }

                Location location = arrow.getLocation();
                World world = location.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }

                Vector velocity = arrow.getVelocity().clone();
                if (velocity.lengthSquared() > 0.0001) {
                    velocity.normalize().multiply(-0.34);
                }

                Location trailLocation = location.clone().add(velocity);
                world.spawnParticle(Particle.DUST, trailLocation, 1, 0.0, 0.0, 0.0, 0.0, trailDust);

                ticksLived++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);
    }

    private static Color resolveArrowTrailColor(Player shooter) {
        DyeColor teamDye = findTeamDyeColor(shooter);
        if (teamDye != null && teamDye.getColor() != null) {
            return teamDye.getColor();
        }

        return Color.WHITE;
    }

    private static void startUltimateChargeTask() {
        if (ultimateHudTaskRunning) {
            return;
        }

        ultimateHudTaskRunning = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!LobbySystem.gameStarted) {
                    ultimateSessionInitialized = false;
                    passiveChargeSecondCounter = 0;
                    stopAllGravityCorePreviews();
                    return;
                }

                cleanupPerkBlocks();
                cleanupOverclockStates();

                if (!ultimateSessionInitialized) {
                    loadUltimateSelectionsForOnlinePlayers();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getGameMode() != GameMode.SURVIVAL) {
                            continue;
                        }

                        setUltimateCharge(player, 0);
                    }
                    ultimateSessionInitialized = true;
                }

                passiveChargeSecondCounter++;
                boolean grantPassiveCharge = passiveChargeSecondCounter >= ULTIMATE_PASSIVE_CHARGE_INTERVAL_SECONDS;
                if (grantPassiveCharge) {
                    passiveChargeSecondCounter = 0;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL) {
                        continue;
                    }

                    if (grantPassiveCharge) {
                        addUltimateCharge(player, ULTIMATE_PASSIVE_CHARGE_AMOUNT);
                    } else {
                        updateUltimateHud(player, getUltimateCharge(player));
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 20L, 20L);
    }

    private static int getUltimateCharge(Player player) {
        return ultimateCharges.getOrDefault(player.getUniqueId(), 0);
    }

    private static void setUltimateCharge(Player player, int charge) {
        int normalizedCharge = Math.max(0, Math.min(ULTIMATE_MAX_CHARGE, charge));
        UUID playerId = player.getUniqueId();

        ultimateCharges.put(playerId, normalizedCharge);
        updateUltimateHud(player, normalizedCharge);

        if (normalizedCharge >= ULTIMATE_MAX_CHARGE) {
            boolean alreadyNotified = ultimateReadyNotified.getOrDefault(playerId, false);
            if (!alreadyNotified) {
                String ultimateName = getSelectedUltimateName(player);
                String activationHint = "Press F to activate.";
                if ("Gravitationskern".equals(ultimateName)) {
                    activationHint = "Press F to preview, F again to activate.";
                }
                player.sendActionBar(Component.text(ultimateName + " ready! " + activationHint, NamedTextColor.GOLD));
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.4f);
                ultimateReadyNotified.put(playerId, true);
            }
        } else {
            stopGravityCorePreview(playerId);
            ultimateReadyNotified.put(playerId, false);
        }
    }

    private static void addUltimateCharge(Player player, int amount) {
        if (!LobbySystem.gameStarted || player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        int currentCharge = getUltimateCharge(player);
        if (currentCharge >= ULTIMATE_MAX_CHARGE) {
            updateUltimateHud(player, currentCharge);
            return;
        }

        setUltimateCharge(player, currentCharge + amount);
    }

    private static void addCombatUltimateCharge(Player attacker, Player victim, int amount) {
        if (amount <= 0 || !LobbySystem.gameStarted) {
            return;
        }

        if (attacker == null || victim == null || attacker.equals(victim)) {
            return;
        }

        if (attacker.getGameMode() != GameMode.SURVIVAL || victim.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        if (!isEnemyPlayer(attacker, victim)) {
            return;
        }

        addUltimateCharge(attacker, amount);
    }

    private static void updateUltimateHud(Player player, int charge) {
        float expProgress = charge / (float) ULTIMATE_MAX_CHARGE;
        player.setExp(Math.max(0f, Math.min(1f, expProgress)));
        player.setLevel(charge >= ULTIMATE_MAX_CHARGE ? ULTIMATE_HUD_FULL_LEVEL : 0);
    }

    private static void loadUltimateSelectionsForOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshUltimateSelection(player);
        }
    }

    public static void refreshUltimateSelection(Player player) {
        Document document = PlayerDataCache.getPlayerPerks(player);

        String selectedUltimate = DEFAULT_ULTIMATE_NAME;
        if (document != null && document.get("ultimate") instanceof String) {
            String candidate = (String) document.get("ultimate");
            if (isUltimateName(candidate)) {
                selectedUltimate = candidate;
            }
        }

        selectedUltimateByPlayer.put(player.getUniqueId(), selectedUltimate);
    }

    private static String getSelectedUltimateName(Player player) {
        UUID playerId = player.getUniqueId();
        String cachedUltimate = selectedUltimateByPlayer.get(playerId);
        if (isUltimateName(cachedUltimate)) {
            return cachedUltimate;
        }

        refreshUltimateSelection(player);
        return selectedUltimateByPlayer.getOrDefault(playerId, DEFAULT_ULTIMATE_NAME);
    }

    private static boolean activateSelectedUltimate(Player player) {
        String ultimateName = getSelectedUltimateName(player);
        switch (ultimateName) {
            case "Zeitanker":
                return activateTimeAnchorUltimate(player);
            case "Gravitationskern":
                return activateGravityCoreUltimate(player);
            case "Perk-Hijack":
                return activatePerkHijackUltimate(player);
            case "Spiegelavatar":
                return activateMirrorAvatarUltimate(player);
            case "Kettenmarkierung":
                return activateChainMarkUltimate(player);
            case "Overclock":
                return activateOverclockUltimate(player);
            case "Minigun":
                return activateMinigunUltimate(player);
            default:
                return activateTimeAnchorUltimate(player);
        }
    }

    private static boolean activateTimeAnchorUltimate(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitTask existingTask = timeAnchorTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        Location anchorLocation = player.getLocation().clone();
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.0f);
        player.sendActionBar(Component.text("Zeitanker gesetzt. Ruecksprung in 4 Sekunden.", NamedTextColor.AQUA));

        BukkitTask task = new BukkitRunnable() {
            long elapsedTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !LobbySystem.gameStarted) {
                    cancel();
                    timeAnchorTasks.remove(playerId);
                    return;
                }

                player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1.0, 0), 4, 0.25, 0.3, 0.25, 0.0);
                player.getWorld().spawnParticle(Particle.ENCHANT, anchorLocation.clone().add(0, 1.0, 0), 4, 0.2, 0.2, 0.2, 0.03);

                elapsedTicks += 4;
                if (elapsedTicks < TIME_ANCHOR_DELAY_TICKS) {
                    return;
                }

                player.teleport(anchorLocation);
                player.setFallDistance(0f);
                executeImpulseWave(player);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendActionBar(Component.text("Zeitanker ausgeloest.", NamedTextColor.GOLD));

                cancel();
                timeAnchorTasks.remove(playerId);
            }
        }.runTaskTimer(Main.getInstance(), 0L, 4L);

        timeAnchorTasks.put(playerId, task);
        return true;
    }

    private static boolean activateGravityCoreUltimate(Player player) {
        return activateGravityCoreUltimate(player, null);
    }

    private static boolean activateGravityCoreUltimate(Player player, Location targetCenter) {
        Location center = targetCenter != null ? targetCenter.clone() : resolveGravityCoreCenter(player);
        center.setY(center.getY() + 0.1);

        Particle.DustOptions darkDust = new Particle.DustOptions(Color.fromRGB(18, 18, 24), 1.4f);
        Particle.DustOptions voidDust = new Particle.DustOptions(Color.fromRGB(42, 0, 58), 1.15f);

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 1.1f);
        player.sendActionBar(Component.text("Gravitationskern stabilisiert sich...", NamedTextColor.AQUA));

        new BukkitRunnable() {
            int ticks = 0;
            double swirlOffset = 0.0;

            @Override
            public void run() {
                if (!LobbySystem.gameStarted || !player.isOnline()) {
                    cancel();
                    return;
                }

                World world = center.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }

                List<LivingEntity> targets = getGravityCoreTargets(player, center, GRAVITY_CORE_RANGE);

                world.spawnParticle(Particle.SQUID_INK, center, 10, 0.28, 0.24, 0.28, 0.005);
                world.spawnParticle(Particle.PORTAL, center, 30, 1.2, 0.9, 1.2, 0.08);
                world.spawnParticle(Particle.SMOKE, center, 26, 0.95, 0.45, 0.95, 0.012);

                for (int i = 0; i < 4; i++) {
                    double angle = swirlOffset + (i * (Math.PI / 2));
                    double radius = Math.max(0.38, 2.35 - (ticks * 0.038) + (i * 0.12));
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = ((i % 2 == 0) ? 0.1 : -0.1);

                    Location swirlPoint = center.clone().add(x, y, z);
                    world.spawnParticle(Particle.DUST, swirlPoint, 2, 0.02, 0.02, 0.02, 0.0, darkDust);
                    world.spawnParticle(Particle.DUST, swirlPoint, 1, 0.0, 0.0, 0.0, 0.0, voidDust);
                }

                for (LivingEntity target : targets) {
                    Vector pull = center.toVector().subtract(target.getLocation().toVector());

                    if (pull.lengthSquared() < 0.0004) {
                        double orbitAngle = swirlOffset + (target.getEntityId() * 0.37);
                        Vector orbit = new Vector(Math.cos(orbitAngle), 0.0, Math.sin(orbitAngle));
                        target.setVelocity(orbit.multiply(0.22).setY(0.1));
                        continue;
                    }

                    double pullStrength = target instanceof Player ? 0.46 : 0.55;
                    double yPull = target.getLocation().getY() > center.getY() ? -0.08 : 0.1;
                    target.setVelocity(pull.normalize().multiply(pullStrength).setY(yPull));
                }

                if (ticks % 8 == 0) {
                    world.playSound(center, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, 0.65f, 0.55f + (ticks / 180f));
                }

                ticks += 2;
                swirlOffset += 0.45;

                if (ticks < 46) {
                    return;
                }

                for (LivingEntity target : targets) {
                    Vector push = target.getLocation().toVector().subtract(center.toVector());
                    push.setY(0);

                    if (push.lengthSquared() < 0.0001) {
                        double randomAngle = Math.random() * Math.PI * 2.0;
                        push = new Vector(Math.cos(randomAngle), 0.0, Math.sin(randomAngle));
                    }

                    double pushStrength = target instanceof Player ? 2.1 : 1.95;
                    target.setVelocity(push.normalize().multiply(pushStrength).setY(0.66));
                }

                world.spawnParticle(Particle.EXPLOSION, center, 4, 0.3, 0.3, 0.3, 0.0);
                world.spawnParticle(Particle.CLOUD, center, 90, 1.5, 0.9, 1.5, 0.02);
                world.spawnParticle(Particle.END_ROD, center, 60, 1.3, 0.8, 1.3, 0.01);
                world.playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.1f, 0.95f);
                world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.45f);
                cancel();
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        return true;
    }

    private static Location resolveGravityCoreCenter(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().clone().normalize();

        RayTraceResult trace = world.rayTrace(
                eye,
                direction,
                14.0,
                FluidCollisionMode.NEVER,
                true,
                0.2,
                entity -> entity != null && !entity.getUniqueId().equals(player.getUniqueId())
        );

        if (trace != null && trace.getHitPosition() != null) {
            return trace.getHitPosition().toLocation(world).add(0, 0.2, 0);
        }

        return eye.clone().add(direction.multiply(6.0));
    }

    private static boolean activatePerkHijackUltimate(Player player) {
        Player target = getNearestEnemyPlayer(player, 8.0);
        if (target == null) {
            player.sendActionBar(Component.text("Kein Ziel fuer Perk-Hijack in Reichweite.", NamedTextColor.RED));
            return false;
        }

        List<String> targetActivePerks = getSelectedActivePerkNames(target);
        if (targetActivePerks.isEmpty()) {
            player.sendActionBar(Component.text("Das Ziel hat keine aktiven Perks ausgeruestet.", NamedTextColor.RED));
            return false;
        }

        String blockedPerk = targetActivePerks.get(0);
        addPerkBlock(target, blockedPerk, HIJACK_DURATION_MS);

        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.3f);
        target.playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.4f);
        player.sendActionBar(Component.text("Hijacked: " + blockedPerk + " from " + target.getName(), NamedTextColor.GOLD));
        target.sendActionBar(Component.text("Your perk " + blockedPerk + " was hijacked!", NamedTextColor.RED));
        return true;
    }

    private static boolean activateMirrorAvatarUltimate(Player player) {
        World world = player.getWorld();
        Location spawnLocation = player.getLocation().clone();

        final PacketMirrorDecoy packetDecoy = trySpawnPacketMirrorDecoy(player, spawnLocation);
        if (packetDecoy == null) {
            player.sendActionBar(Component.text("Spiegelavatar konnte nicht als Fake Player erstellt werden.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.7f);
            return false;
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.0f);
        player.sendActionBar(Component.text("Spiegelavatar jagt Gegner.", NamedTextColor.AQUA));

        final HashMap<UUID, Long> mirrorAttackCooldowns = new HashMap<>();
        new BukkitRunnable() {
            int elapsedTicks = 0;
            double idleAngle = Math.random() * Math.PI * 2.0;

            @Override
            public void run() {
                if (!LobbySystem.gameStarted || !player.isOnline()) {
                    if (packetDecoy != null) {
                        removePacketMirrorDecoy(packetDecoy);
                    }
                    cancel();
                    return;
                }

                Location decoyLocation = packetDecoy.location.clone();

                LivingEntity chaseTarget = getNearestMirrorChaseTarget(player, decoyLocation, 14.0);
                Location nextLocation = decoyLocation.clone();

                if (chaseTarget != null && !chaseTarget.isDead()) {
                    Vector toTarget = chaseTarget.getLocation().toVector().subtract(decoyLocation.toVector());
                    toTarget.setY(0);

                    if (toTarget.lengthSquared() > 0.0001) {
                        double distance = Math.sqrt(toTarget.lengthSquared());
                        double speed = Math.min(1.2, 0.35 + (distance * 0.22));
                        Vector step = toTarget.normalize().multiply(speed);
                        nextLocation.add(step);
                        nextLocation.setY(chaseTarget.getLocation().getY());
                        nextLocation.setYaw((float) Math.toDegrees(Math.atan2(-step.getX(), step.getZ())));
                        nextLocation.setPitch(0f);
                    }
                }
                else {
                    idleAngle += 0.3;
                    nextLocation = spawnLocation.clone().add(Math.cos(idleAngle) * 1.4, 0.0, Math.sin(idleAngle) * 1.4);
                    nextLocation.setYaw((float) Math.toDegrees(idleAngle) + 90f);
                    nextLocation.setPitch(0f);
                }

                if (!movePacketMirrorDecoy(packetDecoy, nextLocation)) {
                    removePacketMirrorDecoy(packetDecoy);
                    cancel();
                    return;
                }

                decoyLocation = packetDecoy.location.clone();
                world.spawnParticle(Particle.ENCHANT, decoyLocation.clone().add(0, 1, 0), 4, 0.16, 0.18, 0.16, 0.02);
                world.spawnParticle(Particle.END_ROD, decoyLocation.clone().add(0, 1.0, 0), 2, 0.1, 0.14, 0.1, 0.0);

                long now = System.currentTimeMillis();
                mirrorAttackCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now - 3000L);

                for (LivingEntity target : getMirrorAttackTargets(player, decoyLocation, 2.2)) {
                    Long cooldownUntil = mirrorAttackCooldowns.get(target.getUniqueId());
                    if (cooldownUntil != null && cooldownUntil > now) {
                        continue;
                    }

                    mirrorAttackCooldowns.put(target.getUniqueId(), now + 700L);
                    target.damage(2.0, player);
                    Vector knock = target.getLocation().toVector().subtract(decoyLocation.toVector());
                    if (knock.lengthSquared() < 0.0001) {
                        knock = player.getLocation().getDirection().clone();
                    }
                    target.setVelocity(knock.normalize().multiply(0.58).setY(0.26));
                    if (target instanceof Player enemyPlayer) {
                        enemyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 24, 0, false, false, true));
                    }

                    world.spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.0, 0), 14, 0.25, 0.25, 0.25, 0.02);
                    world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.2f);
                }

                elapsedTicks += 2;
                if (elapsedTicks < MIRROR_DURATION_TICKS) {
                    return;
                }

                world.spawnParticle(Particle.CLOUD, decoyLocation.clone().add(0, 1, 0), 24, 0.3, 0.2, 0.3, 0.01);
                if (packetDecoy != null) {
                    removePacketMirrorDecoy(packetDecoy);
                }
                cancel();
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        return true;
    }

    private static boolean activateChainMarkUltimate(Player player) {
        Player firstTarget = getNearestEnemyPlayer(player, 8.0);
        if (firstTarget == null) {
            player.sendActionBar(Component.text("Kein Ziel fuer Kettenmarkierung in Reichweite.", NamedTextColor.RED));
            return false;
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.2f);
        player.sendActionBar(Component.text("Kettenmarkierung gestartet.", NamedTextColor.AQUA));

        ArrayList<UUID> alreadyHit = new ArrayList<>();
        new BukkitRunnable() {
            int chainStep = 0;
            Player currentTarget = firstTarget;

            @Override
            public void run() {
                if (!LobbySystem.gameStarted || !player.isOnline() || currentTarget == null || !currentTarget.isOnline()) {
                    cancel();
                    return;
                }

                alreadyHit.add(currentTarget.getUniqueId());
                currentTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 1, false, false, true));
                applyPerkSilence(currentTarget, 1500L);
                currentTarget.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, currentTarget.getLocation().add(0, 1, 0), 22, 0.4, 0.4, 0.4, 0.03);
                currentTarget.getWorld().playSound(currentTarget.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.9f, 1.2f);

                chainStep += 1;
                if (chainStep >= 3) {
                    cancel();
                    return;
                }

                currentTarget = getNearestEnemyPlayerFromLocation(player, currentTarget.getLocation(), 6.0, alreadyHit);
            }
        }.runTaskTimer(Main.getInstance(), 0L, CHAIN_MARK_STEP_DELAY_TICKS);

        return true;
    }

    private static boolean activateOverclockUltimate(Player player) {
        long now = System.currentTimeMillis();
        overclockActiveUntil.put(player.getUniqueId(), now + OVERCLOCK_DURATION_MS);
        overclockOverheatUntil.put(player.getUniqueId(), now + OVERCLOCK_DURATION_MS + OVERCLOCK_OVERHEAT_MS);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) (OVERCLOCK_DURATION_MS / 50L), 1, false, false, true));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.8f);
        player.sendActionBar(Component.text("Overclock online for 8 seconds.", NamedTextColor.GOLD));
        return true;
    }

    private static boolean activateMinigunUltimate(Player player) {
        UUID playerId = player.getUniqueId();

        BukkitTask existingTask = minigunTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }

        player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_QUICK_CHARGE_3, 1.0f, 1.15f);
        player.sendActionBar(Component.text("Minigun online!", NamedTextColor.GOLD));

        BukkitTask task = new BukkitRunnable() {
            int burstsFired = 0;

            @Override
            public void run() {
                if (!LobbySystem.gameStarted || !player.isOnline() || player.getGameMode() != GameMode.SURVIVAL) {
                    cancel();
                    minigunTasks.remove(playerId);
                    return;
                }

                Location eye = player.getEyeLocation();
                Vector baseDirection = eye.getDirection().normalize().multiply(2.9);

                for (int i = 0; i < MINIGUN_ARROWS_PER_BURST; i++) {
                    Arrow arrow = player.launchProjectile(Arrow.class);
                    Vector spread = new Vector(
                            (Math.random() - 0.5) * 0.22,
                            (Math.random() - 0.5) * 0.16,
                            (Math.random() - 0.5) * 0.22
                    );
                    arrow.setVelocity(baseDirection.clone().add(spread));
                    arrow.setCritical(false);
                    arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                    arrow.setMetadata(MINIGUN_ARROW_METADATA, new FixedMetadataValue(Main.getInstance(), true));
                    startArrowTrail(arrow, player);
                }

                player.playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 0.55f, 1.65f);
                burstsFired++;

                if (burstsFired < MINIGUN_BURSTS) {
                    return;
                }

                player.sendActionBar(Component.text("Minigun offline.", NamedTextColor.AQUA));
                minigunTasks.remove(playerId);
                cancel();
            }
        }.runTaskTimer(Main.getInstance(), 0L, MINIGUN_BURST_INTERVAL_TICKS);

        minigunTasks.put(playerId, task);
        return true;
    }

    private static PacketMirrorDecoy trySpawnPacketMirrorDecoy(Player owner, Location spawnLocation) {
        try {
            Object ownerHandle = getNmsHandle(owner);
            if (ownerHandle == null) {
                logMirrorDebug("owner handle not available", null);
                return null;
            }

            Object minecraftServer = resolveMinecraftServerHandle(ownerHandle);
            Object serverLevel = resolveServerLevelHandle(owner, spawnLocation, ownerHandle);

            if (minecraftServer == null || serverLevel == null) {
                logMirrorDebug("missing minecraftServer/serverLevel handle", null);
                return null;
            }

            UUID profileId = UUID.randomUUID();
            String mirrorName = owner.getName().substring(0, Math.min(16, owner.getName().length()));

            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object gameProfile = createMirrorGameProfile(ownerHandle, gameProfileClass, profileId, mirrorName);
            if (gameProfile == null) {
                logMirrorDebug("failed to build fake GameProfile", null);
                return null;
            }

            Object clientInformation = invokeNoArgs(ownerHandle, "clientInformation");
            if (clientInformation == null) {
                clientInformation = getFieldValueByType(ownerHandle, "ClientInformation");
            }

            Object fakePlayerHandle = constructServerPlayer(minecraftServer, serverLevel, gameProfile, clientInformation);
            if (fakePlayerHandle == null) {
                logMirrorDebug("could not construct ServerPlayer", null);
                return null;
            }

            syncMirrorSkinLayers(ownerHandle, fakePlayerHandle);
            moveNmsEntity(fakePlayerHandle, spawnLocation);

            Method getIdMethod = fakePlayerHandle.getClass().getMethod("getId");
            int entityId = (int) getIdMethod.invoke(fakePlayerHandle);

            Object addInfoPacket = createPlayerInfoAddPacket(fakePlayerHandle);
            Object addEntityPacket = createAddEntityPacket(fakePlayerHandle);
            Object entityDataPacket = createEntityDataPacket(fakePlayerHandle);
            if (addInfoPacket == null || addEntityPacket == null) {
                logMirrorDebug("failed to build spawn packets (info=" + (addInfoPacket != null) + ", entity=" + (addEntityPacket != null) + ")", null);
                return null;
            }

            sendPacketToWorld(owner.getWorld(), addInfoPacket);
            sendPacketToWorld(owner.getWorld(), addEntityPacket);
            if (entityDataPacket != null) {
                sendPacketToWorld(owner.getWorld(), entityDataPacket);
            }

            Object rotatePacket = createRotateHeadPacket(fakePlayerHandle, spawnLocation.getYaw());
            if (rotatePacket != null) {
                sendPacketToWorld(owner.getWorld(), rotatePacket);
            }

            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                Object removeTabPacket = createPlayerInfoRemovePacket(profileId);
                if (removeTabPacket != null) {
                    sendPacketToWorld(owner.getWorld(), removeTabPacket);
                }
            }, 20L);

            return new PacketMirrorDecoy(profileId, entityId, fakePlayerHandle, spawnLocation.clone());
        } catch (Throwable throwable) {
            logMirrorDebug("packet mirror creation threw exception", throwable);
            return null;
        }
    }

    private static Object createMirrorGameProfile(Object ownerHandle, Class<?> gameProfileClass, UUID profileId, String mirrorName) {
        Object ownerProfile = null;
        Object ownerProperties = null;
        Collection<?> textures = null;

        try {
            Method getGameProfileMethod = ownerHandle.getClass().getMethod("getGameProfile");
            ownerProfile = getGameProfileMethod.invoke(ownerHandle);
            ownerProperties = resolveGameProfileProperties(ownerProfile);
            textures = extractTextureProperties(ownerProperties);
        } catch (Throwable throwable) {
            logMirrorDebug("failed reading owner profile properties", throwable);
        }

        if (ownerProperties != null) {
            Object profileWithOwnerProperties = constructGameProfileWithProperties(gameProfileClass, profileId, mirrorName, ownerProperties);
            if (profileWithOwnerProperties != null) {
                return profileWithOwnerProperties;
            }

            logMirrorDebug("direct GameProfile construction with owner properties failed", null);
        }

        if (textures != null && !textures.isEmpty()) {
            Object mutablePropertyMap = createMutablePropertyMap();
            if (mutablePropertyMap != null) {
                boolean insertedAny = false;
                for (Object textureProperty : textures) {
                    insertedAny |= putTextureProperty(mutablePropertyMap, textureProperty);
                }

                if (!insertedAny) {
                    insertedAny = putTextureCollection(mutablePropertyMap, textures);
                }

                if (insertedAny) {
                    Object profileWithProperties = constructGameProfileWithProperties(gameProfileClass, profileId, mirrorName, mutablePropertyMap);
                    if (profileWithProperties != null) {
                        return profileWithProperties;
                    }

                    logMirrorDebug("constructing GameProfile with mutable texture map failed", null);
                }
                else {
                    logMirrorDebug("could not insert texture properties into mutable map", null);
                }
            }
            else {
                logMirrorDebug("could not create mutable PropertyMap", null);
            }
        }
        else {
            logMirrorDebug("owner profile has no textures to clone", null);
        }

        try {
            logMirrorDebug("using no-skin fallback GameProfile", null);
            return gameProfileClass.getConstructor(UUID.class, String.class).newInstance(profileId, mirrorName);
        } catch (Throwable throwable) {
            logMirrorDebug("fallback GameProfile creation failed", throwable);
            return null;
        }
    }

    private static Object constructGameProfileWithProperties(Class<?> gameProfileClass, UUID profileId, String mirrorName, Object propertyMap) {
        try {
            for (Constructor<?> constructor : gameProfileClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 3) {
                    continue;
                }

                if (parameterTypes[0] != UUID.class || parameterTypes[1] != String.class || !parameterTypes[2].isInstance(propertyMap)) {
                    continue;
                }

                return constructor.newInstance(profileId, mirrorName, propertyMap);
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object createMutablePropertyMap() {
        try {
            Class<?> hashMultimapClass = Class.forName("com.google.common.collect.HashMultimap");
            Method createMethod = hashMultimapClass.getMethod("create");
            Object multimap = createMethod.invoke(null);

            Class<?> propertyMapClass = Class.forName("com.mojang.authlib.properties.PropertyMap");
            for (Constructor<?> constructor : propertyMapClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0].isInstance(multimap)) {
                    return constructor.newInstance(multimap);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static void syncMirrorSkinLayers(Object ownerHandle, Object fakePlayerHandle) {
        byte desiredMask = FULL_SKIN_LAYER_MASK;
        Byte ownerMask = readPlayerModelCustomizationMask(ownerHandle);
        if (ownerMask != null) {
            desiredMask = ownerMask;
        }

        if (applyPlayerModelCustomizationMask(fakePlayerHandle, desiredMask)) {
            return;
        }

        if (desiredMask != FULL_SKIN_LAYER_MASK && applyPlayerModelCustomizationMask(fakePlayerHandle, FULL_SKIN_LAYER_MASK)) {
            return;
        }
    }

    private static Byte readPlayerModelCustomizationMask(Object playerHandle) {
        Object accessor = resolvePlayerModelCustomizationAccessor();
        if (accessor == null) {
            return null;
        }

        Object entityData = invokeNoArgs(playerHandle, "getEntityData");
        if (entityData == null) {
            return null;
        }

        for (Method method : entityData.getClass().getMethods()) {
            if (!method.getName().equals("get") || method.getParameterCount() != 1 || !method.getParameterTypes()[0].isInstance(accessor)) {
                continue;
            }

            try {
                Object value = method.invoke(entityData, accessor);
                if (value instanceof Byte mask) {
                    return mask;
                }

                if (value instanceof Number number) {
                    return number.byteValue();
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static boolean applyPlayerModelCustomizationMask(Object playerHandle, byte mask) {
        Object accessor = resolvePlayerModelCustomizationAccessor();
        if (accessor == null) {
            return false;
        }

        Object entityData = invokeNoArgs(playerHandle, "getEntityData");
        if (entityData == null) {
            return false;
        }

        for (Method method : entityData.getClass().getMethods()) {
            if (!method.getName().equals("set") || method.getParameterCount() != 2 || !method.getParameterTypes()[0].isInstance(accessor)) {
                continue;
            }

            try {
                method.invoke(entityData, accessor, mask);
                return true;
            } catch (Throwable ignored) {
                try {
                    method.invoke(entityData, accessor, Byte.valueOf(mask));
                    return true;
                } catch (Throwable ignoredAgain) {
                }
            }
        }

        return false;
    }

    private static Object resolvePlayerModelCustomizationAccessor() {
        try {
            Class<?> playerClass = Class.forName("net.minecraft.world.entity.player.Player");

            for (String fieldName : new String[] {"DATA_PLAYER_MODE_CUSTOMISATION", "DATA_PLAYER_MODE_CUSTOMIZATION"}) {
                try {
                    Field field = playerClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null) {
                        return value;
                    }
                } catch (Throwable ignored) {
                }
            }

            for (Field field : playerClass.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) || !field.getType().getName().contains("EntityDataAccessor")) {
                    continue;
                }

                if (!field.getName().toLowerCase().contains("custom")) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null) {
                        return value;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object resolveMinecraftServerHandle(Object ownerHandle) {
        Object minecraftServer = getFieldValueByType(ownerHandle, "MinecraftServer");
        if (minecraftServer != null) {
            return minecraftServer;
        }

        try {
            Object craftServer = Bukkit.getServer();
            if (craftServer == null) {
                return null;
            }

            Method getServerMethod = craftServer.getClass().getMethod("getServer");
            return getServerMethod.invoke(craftServer);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object resolveServerLevelHandle(Player owner, Location spawnLocation, Object ownerHandle) {
        Object serverLevel = invokeNoArgs(ownerHandle, "serverLevel");
        if (serverLevel == null) {
            serverLevel = getFieldValueByType(ownerHandle, "ServerLevel");
        }
        if (serverLevel != null) {
            return serverLevel;
        }

        World world = spawnLocation != null && spawnLocation.getWorld() != null ? spawnLocation.getWorld() : owner.getWorld();
        if (world == null) {
            return null;
        }

        try {
            Method getHandleMethod = world.getClass().getMethod("getHandle");
            return getHandleMethod.invoke(world);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean movePacketMirrorDecoy(PacketMirrorDecoy decoy, Location targetLocation) {
        if (decoy == null || targetLocation == null || targetLocation.getWorld() == null) {
            return false;
        }

        try {
            moveNmsEntity(decoy.handle, targetLocation);

            Object teleportPacket = createTeleportPacket(decoy.handle);
            if (teleportPacket == null) {
                return false;
            }

            sendPacketToWorld(targetLocation.getWorld(), teleportPacket);

            Object rotatePacket = createRotateHeadPacket(decoy.handle, targetLocation.getYaw());
            if (rotatePacket != null) {
                sendPacketToWorld(targetLocation.getWorld(), rotatePacket);
            }

            decoy.location = targetLocation.clone();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void removePacketMirrorDecoy(PacketMirrorDecoy decoy) {
        if (decoy == null || decoy.location == null || decoy.location.getWorld() == null) {
            return;
        }

        Object removeEntityPacket = createRemoveEntitiesPacket(decoy.entityId);
        if (removeEntityPacket != null) {
            sendPacketToWorld(decoy.location.getWorld(), removeEntityPacket);
        }

        Object removeTabPacket = createPlayerInfoRemovePacket(decoy.profileId);
        if (removeTabPacket != null) {
            sendPacketToWorld(decoy.location.getWorld(), removeTabPacket);
        }
    }

    private static Object constructServerPlayer(Object minecraftServer, Object serverLevel, Object gameProfile, Object clientInformation) {
        try {
            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");

            for (Constructor<?> constructor : serverPlayerClass.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length < 3) {
                    continue;
                }

                if (!parameterTypes[0].isInstance(minecraftServer)
                        || !parameterTypes[1].isInstance(serverLevel)
                        || !parameterTypes[2].isInstance(gameProfile)) {
                    continue;
                }

                constructor.setAccessible(true);

                if (parameterTypes.length == 3) {
                    return constructor.newInstance(minecraftServer, serverLevel, gameProfile);
                }

                if (parameterTypes.length == 4) {
                    Object resolvedClientInformation = resolveClientInformation(parameterTypes[3], clientInformation);
                    if (resolvedClientInformation != null) {
                        return constructor.newInstance(minecraftServer, serverLevel, gameProfile, resolvedClientInformation);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object resolveClientInformation(Class<?> expectedType, Object clientInformation) {
        if (clientInformation != null && expectedType.isInstance(clientInformation)) {
            return clientInformation;
        }

        for (Method method : expectedType.getMethods()) {
            if (method.getParameterCount() == 0
                    && method.getReturnType().equals(expectedType)
                    && java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                try {
                    return method.invoke(null);
                } catch (Throwable ignored) {
                }
            }
        }

        return null;
    }

    private static void copySkinProperties(Object ownerHandle, Object targetGameProfile) {
        try {
            Method getGameProfileMethod = ownerHandle.getClass().getMethod("getGameProfile");
            Object ownerProfile = getGameProfileMethod.invoke(ownerHandle);

            Object ownerProperties = resolveGameProfileProperties(ownerProfile);
            Object targetProperties = resolveGameProfileProperties(targetGameProfile);
            if (ownerProperties == null || targetProperties == null) {
                logMirrorDebug("could not resolve GameProfile properties holder", null);
                return;
            }

            Collection<?> textures = extractTextureProperties(ownerProperties);
            if (textures == null || textures.isEmpty()) {
                logMirrorDebug("owner profile has no texture properties", null);
                return;
            }

            boolean insertedAny = false;
            for (Object textureProperty : textures) {
                insertedAny |= putTextureProperty(targetProperties, textureProperty);
            }

            if (!insertedAny) {
                insertedAny = putTextureCollection(targetProperties, textures);
            }

            if (!insertedAny) {
                logMirrorDebug("failed to copy texture properties into fake profile", null);
            }
        } catch (Throwable throwable) {
            logMirrorDebug("copySkinProperties failed", throwable);
        }
    }

    private static Object resolveGameProfileProperties(Object gameProfile) {
        if (gameProfile == null) {
            return null;
        }

        for (String methodName : new String[] {"getProperties", "properties"}) {
            try {
                Method method = gameProfile.getClass().getMethod(methodName);
                Object value = method.invoke(gameProfile);
                if (value != null) {
                    return value;
                }
            } catch (Throwable ignored) {
            }
        }

        Object byFieldName = getFieldValueByName(gameProfile, "properties");
        if (byFieldName != null) {
            return byFieldName;
        }

        Object byType = getFieldValueByType(gameProfile, "PropertyMap");
        if (byType != null) {
            return byType;
        }

        return getFieldValueByType(gameProfile, "Multimap");
    }

    private static Collection<?> extractTextureProperties(Object propertyMap) {
        if (propertyMap == null) {
            return null;
        }

        Class<?>[] keyTypes = new Class<?>[] {String.class, Object.class};
        for (Class<?> keyType : keyTypes) {
            try {
                Method getMethod = propertyMap.getClass().getMethod("get", keyType);
                Object result = getMethod.invoke(propertyMap, "textures");
                if (result instanceof Collection<?> collection && !collection.isEmpty()) {
                    return collection;
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static boolean putTextureProperty(Object propertyMap, Object textureProperty) {
        try {
            Class<?> multimapClass = Class.forName("com.google.common.collect.Multimap");
            if (multimapClass.isInstance(propertyMap)) {
                Method putMethod = multimapClass.getMethod("put", Object.class, Object.class);
                Object result = putMethod.invoke(propertyMap, "textures", textureProperty);
                return !(result instanceof Boolean) || (Boolean) result;
            }
        } catch (Throwable ignored) {
        }

        Method[] methods = propertyMap.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().equals("put") || method.getParameterCount() != 2) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean keyAcceptsTextureKey = parameterTypes[0].isAssignableFrom(String.class)
                    || parameterTypes[0].isAssignableFrom(Object.class)
                    || parameterTypes[0] == CharSequence.class;
            if (!keyAcceptsTextureKey || !parameterTypes[1].isInstance(textureProperty)) {
                continue;
            }

            try {
                method.invoke(propertyMap, "textures", textureProperty);
                return true;
            } catch (Throwable ignored) {
            }
        }

        return false;
    }

    private static boolean putTextureCollection(Object propertyMap, Collection<?> textures) {
        try {
            Class<?> multimapClass = Class.forName("com.google.common.collect.Multimap");
            if (multimapClass.isInstance(propertyMap)) {
                Method putAllMethod = multimapClass.getMethod("putAll", Object.class, Iterable.class);
                Object result = putAllMethod.invoke(propertyMap, "textures", textures);
                return !(result instanceof Boolean) || (Boolean) result;
            }
        } catch (Throwable ignored) {
        }

        Method[] methods = propertyMap.getClass().getMethods();
        for (Method method : methods) {
            if (!method.getName().equals("putAll") || method.getParameterCount() != 2) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean keyAcceptsTextureKey = parameterTypes[0].isAssignableFrom(String.class)
                    || parameterTypes[0].isAssignableFrom(Object.class)
                    || parameterTypes[0] == CharSequence.class;
            if (!keyAcceptsTextureKey || !parameterTypes[1].isAssignableFrom(textures.getClass())) {
                continue;
            }

            try {
                method.invoke(propertyMap, "textures", textures);
                return true;
            } catch (Throwable ignored) {
            }
        }

        return false;
    }

    private static void moveNmsEntity(Object entityHandle, Location location) throws Exception {
        if (entityHandle == null || location == null) {
            return;
        }

        try {
            Method moveToMethod = entityHandle.getClass().getMethod(
                    "moveTo",
                    double.class,
                    double.class,
                    double.class,
                    float.class,
                    float.class
            );
            moveToMethod.invoke(entityHandle, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
            return;
        } catch (NoSuchMethodException ignored) {
        }

        Method setPosMethod = entityHandle.getClass().getMethod("setPos", double.class, double.class, double.class);
        setPosMethod.invoke(entityHandle, location.getX(), location.getY(), location.getZ());

        try {
            Method setYRotMethod = entityHandle.getClass().getMethod("setYRot", float.class);
            setYRotMethod.invoke(entityHandle, location.getYaw());
            Method setXRotMethod = entityHandle.getClass().getMethod("setXRot", float.class);
            setXRotMethod.invoke(entityHandle, location.getPitch());
        } catch (NoSuchMethodException ignored) {
        }
    }

    private static Object createPlayerInfoAddPacket(Object serverPlayerHandle) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Class<?> actionClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            Class<?> entryClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");

            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumClass = (Class<? extends Enum>) actionClass;
            Enum<?> addPlayerAction = Enum.valueOf(enumClass, "ADD_PLAYER");

            @SuppressWarnings({"rawtypes", "unchecked"})
            EnumSet actions = EnumSet.of((Enum) addPlayerAction);
            addActionIfPresent(actions, enumClass, "UPDATE_LISTED");
            addActionIfPresent(actions, enumClass, "UPDATE_GAME_MODE");
            addActionIfPresent(actions, enumClass, "UPDATE_LATENCY");
            addActionIfPresent(actions, enumClass, "UPDATE_HAT");
            addActionIfPresent(actions, enumClass, "UPDATE_LIST_ORDER");

            // 1) Static helper variants if available.
            for (Method method : packetClass.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers()) || !packetClass.equals(method.getReturnType())) {
                    continue;
                }

                if (!method.getName().equals("createSinglePlayerInitializing") && !method.getName().equals("createPlayerInitializing")) {
                    continue;
                }

                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 2
                        && parameterTypes[1] == boolean.class
                        && (parameterTypes[0].isInstance(serverPlayerHandle) || parameterTypes[0].getName().contains("ServerPlayer"))) {
                    try {
                        Object packet = method.invoke(null, serverPlayerHandle, true);
                        if (packet != null) {
                            return packet;
                        }
                    } catch (Throwable ignored) {
                    }

                    try {
                        Object packet = method.invoke(null, serverPlayerHandle, false);
                        if (packet != null) {
                            return packet;
                        }
                    } catch (Throwable ignored) {
                    }
                    continue;
                }

                if (parameterTypes.length == 1 && Collection.class.isAssignableFrom(parameterTypes[0])) {
                    try {
                        Object packet = method.invoke(null, Collections.singletonList(serverPlayerHandle));
                        if (packet != null) {
                            return packet;
                        }
                    } catch (Throwable ignored) {
                    }
                    continue;
                }
            }

            // 2) Build a manual Entry that does not require a live ServerPlayer.connection.
            Object manualEntry = createManualPlayerInfoEntry(serverPlayerHandle, entryClass);
            if (manualEntry != null) {
                Object packetFromEntry = createPlayerInfoPacketFromEntry(packetClass, actions, manualEntry);
                if (packetFromEntry != null) {
                    return packetFromEntry;
                }
            }

            // 3) Fallback constructor(Action, ServerPlayer) for compatible runtimes.
            for (Constructor<?> constructor : packetClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 2
                        && parameterTypes[0].equals(actionClass)
                        && (parameterTypes[1].isInstance(serverPlayerHandle) || parameterTypes[1].getName().contains("ServerPlayer"))) {
                    try {
                        Object packet = constructor.newInstance(addPlayerAction, serverPlayerHandle);
                        if (packet != null) {
                            return packet;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            // 4) Last fallback: constructor(EnumSet, Collection<ServerPlayer>)
            for (Constructor<?> constructor : packetClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 2
                        && java.util.Set.class.isAssignableFrom(parameterTypes[0])
                        && Collection.class.isAssignableFrom(parameterTypes[1])) {
                    try {
                        Object packet = constructor.newInstance(actions, Collections.singletonList(serverPlayerHandle));
                        if (packet != null) {
                            return packet;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable throwable) {
            logMirrorDebug("createPlayerInfoAddPacket failed", throwable);
        }

        logMirrorDebug("createPlayerInfoAddPacket exhausted all strategies", null);
        return null;
    }

    private static void addActionIfPresent(EnumSet actions, Class<? extends Enum> enumClass, String actionName) {
        try {
            actions.add(Enum.valueOf(enumClass, actionName));
        } catch (Throwable ignored) {
        }
    }

    private static Object createManualPlayerInfoEntry(Object serverPlayerHandle, Class<?> entryClass) {
        try {
            Object uuidObject = invokeNoArgs(serverPlayerHandle, "getUUID");
            Object profileObject = invokeNoArgs(serverPlayerHandle, "getGameProfile");
            if (!(uuidObject instanceof UUID uuid) || profileObject == null) {
                return null;
            }

            Object gameMode = invokeNoArgs(serverPlayerHandle, "gameMode");
            if (gameMode == null) {
                gameMode = getStaticFieldByName("net.minecraft.world.level.GameType", "DEFAULT_MODE");
            }
            if (gameMode == null) {
                return null;
            }

            for (Constructor<?> constructor : entryClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 9) {
                    continue;
                }

                if (!UUID.class.isAssignableFrom(parameterTypes[0])
                        || !parameterTypes[1].getName().contains("GameProfile")
                        || parameterTypes[2] != boolean.class
                        || parameterTypes[3] != int.class
                        || !parameterTypes[4].isInstance(gameMode)
                        || parameterTypes[6] != boolean.class
                        || parameterTypes[7] != int.class) {
                    continue;
                }

                return constructor.newInstance(uuid, profileObject, true, 0, gameMode, null, true, 0, null);
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object createPlayerInfoPacketFromEntry(Class<?> packetClass, EnumSet actions, Object entry) {
        try {
            for (Constructor<?> constructor : packetClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 2 || !java.util.Set.class.isAssignableFrom(parameterTypes[0])) {
                    continue;
                }

                if (parameterTypes[1].isInstance(entry)) {
                    return constructor.newInstance(actions, entry);
                }

                if (java.util.List.class.isAssignableFrom(parameterTypes[1])) {
                    return constructor.newInstance(actions, Collections.singletonList(entry));
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object getStaticFieldByName(String className, String fieldName) {
        try {
            Class<?> type = Class.forName(className);
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createPlayerInfoRemovePacket(UUID profileId) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
            Constructor<?> constructor = packetClass.getConstructor(List.class);
            return constructor.newInstance(Collections.singletonList(profileId));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object createAddEntityPacket(Object entityHandle) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");

            Object idObject = invokeNoArgs(entityHandle, "getId");
            Object uuidObject = invokeNoArgs(entityHandle, "getUUID");
            Object typeObject = invokeNoArgs(entityHandle, "getType");
            Object velocityObject = invokeNoArgs(entityHandle, "getDeltaMovement");
            Object xObject = invokeNoArgs(entityHandle, "getX");
            Object yObject = invokeNoArgs(entityHandle, "getY");
            Object zObject = invokeNoArgs(entityHandle, "getZ");

            if (idObject instanceof Integer id
                    && uuidObject instanceof UUID uuid
                    && typeObject != null
                    && velocityObject != null
                    && xObject instanceof Double x
                    && yObject instanceof Double y
                    && zObject instanceof Double z) {

                float xRot = extractRotation(entityHandle, "getXRot", 0f);
                float yRot = extractRotation(entityHandle, "getYRot", 0f);
                double yHeadRot = extractRotation(entityHandle, "getYHeadRot", yRot);

                for (Constructor<?> constructor : packetClass.getConstructors()) {
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    if (parameterTypes.length == 11
                            && parameterTypes[0] == int.class
                            && UUID.class.isAssignableFrom(parameterTypes[1])
                            && parameterTypes[2] == double.class
                            && parameterTypes[3] == double.class
                            && parameterTypes[4] == double.class
                            && parameterTypes[5] == float.class
                            && parameterTypes[6] == float.class
                            && parameterTypes[8] == int.class
                            && parameterTypes[10] == double.class) {
                        return constructor.newInstance(id, uuid, x, y, z, xRot, yRot, typeObject, 0, velocityObject, yHeadRot);
                    }
                }
            }

            for (Constructor<?> constructor : packetClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0].isInstance(entityHandle)) {
                    return constructor.newInstance(entityHandle);
                }

                if (parameterTypes.length == 1 && parameterTypes[0].getName().contains("Entity")) {
                    return constructor.newInstance(entityHandle);
                }

                if (parameterTypes.length == 3
                        && (parameterTypes[0].isInstance(entityHandle) || parameterTypes[0].getName().contains("Entity"))
                        && parameterTypes[1] == int.class
                        && parameterTypes[2].getName().endsWith("BlockPos")) {
                    Object blockPos = createBlockPosFromEntity(entityHandle);
                    if (blockPos != null) {
                        return constructor.newInstance(entityHandle, 0, blockPos);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object createEntityDataPacket(Object entityHandle) {
        try {
            Object entityIdObject = invokeNoArgs(entityHandle, "getId");
            Object entityData = invokeNoArgs(entityHandle, "getEntityData");
            if (!(entityIdObject instanceof Integer entityId) || entityData == null) {
                return null;
            }

            List<?> values = null;
            Object packedDirty = invokeNoArgs(entityData, "packDirty");
            if (packedDirty instanceof List<?> dirtyList && !dirtyList.isEmpty()) {
                values = dirtyList;
            }

            if (values == null) {
                Object nonDefaultValues = invokeNoArgs(entityData, "getNonDefaultValues");
                if (nonDefaultValues instanceof List<?> nonDefaultList && !nonDefaultList.isEmpty()) {
                    values = nonDefaultList;
                }
            }

            if (values == null || values.isEmpty()) {
                return null;
            }

            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
            for (Constructor<?> constructor : packetClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 2 || parameterTypes[0] != int.class) {
                    continue;
                }

                if (parameterTypes[1].isInstance(values) || List.class.isAssignableFrom(parameterTypes[1])) {
                    return constructor.newInstance(entityId, values);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object createBlockPosFromEntity(Object entityHandle) {
        try {
            double x = (double) invokeNoArgs(entityHandle, "getX");
            double y = (double) invokeNoArgs(entityHandle, "getY");
            double z = (double) invokeNoArgs(entityHandle, "getZ");

            Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
            for (Constructor<?> constructor : blockPosClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 3
                        && parameterTypes[0] == int.class
                        && parameterTypes[1] == int.class
                        && parameterTypes[2] == int.class) {
                    return constructor.newInstance((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object createTeleportPacket(Object entityHandle) {
        String[] packetClasses = new String[] {
                "net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket",
                "net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket"
        };

        for (String packetClassName : packetClasses) {
            try {
                Class<?> packetClass = Class.forName(packetClassName);

                for (Method method : packetClass.getMethods()) {
                    if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())
                            || method.getParameterCount() != 1
                            || !packetClass.isAssignableFrom(method.getReturnType())) {
                        continue;
                    }

                    if (method.getParameterTypes()[0].isInstance(entityHandle)
                            || method.getParameterTypes()[0].getName().contains("Entity")) {
                        return method.invoke(null, entityHandle);
                    }
                }

                for (Constructor<?> constructor : packetClass.getConstructors()) {
                    if (constructor.getParameterCount() == 1) {
                        Class<?> parameterType = constructor.getParameterTypes()[0];
                        if (parameterType.isInstance(entityHandle) || parameterType.getName().contains("Entity")) {
                            return constructor.newInstance(entityHandle);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static Object createRotateHeadPacket(Object entityHandle, float yaw) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket");
            byte yawByte = (byte) (yaw * 256.0f / 360.0f);

            for (Constructor<?> constructor : packetClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 2
                        && (parameterTypes[0].isInstance(entityHandle) || parameterTypes[0].getName().contains("Entity"))
                        && parameterTypes[1] == byte.class) {
                    return constructor.newInstance(entityHandle, yawByte);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Object createRemoveEntitiesPacket(int entityId) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
            for (Constructor<?> constructor : packetClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0] == int[].class) {
                    return constructor.newInstance((Object) new int[] {entityId});
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static void sendPacketToWorld(World world, Object packet) {
        if (world == null || packet == null) {
            return;
        }

        for (Player viewer : world.getPlayers()) {
            sendPacket(viewer, packet);
        }
    }

    private static void sendPacket(Player viewer, Object packet) {
        try {
            Object viewerHandle = getNmsHandle(viewer);
            if (viewerHandle == null) {
                return;
            }

            Object connection = getFieldValueByName(viewerHandle, "connection");
            if (connection == null) {
                connection = getFieldValueByType(viewerHandle, "ServerGamePacketListenerImpl");
            }

            if (connection == null) {
                return;
            }

            try {
                Class<?> packetType = Class.forName("net.minecraft.network.protocol.Packet");
                Method sendMethod = connection.getClass().getMethod("send", packetType);
                sendMethod.invoke(connection, packet);
                return;
            } catch (Throwable ignored) {
            }

            for (Method method : connection.getClass().getMethods()) {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0].getName().contains("Packet")) {
                    method.invoke(connection, packet);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static float extractRotation(Object entityHandle, String methodName, float fallback) {
        Object value = invokeNoArgs(entityHandle, methodName);
        if (value instanceof Float result) {
            return result;
        }

        if (value instanceof Double result) {
            return result.floatValue();
        }

        return fallback;
    }

    private static void logMirrorDebug(String message, Throwable throwable) {
        if (Main.getInstance() == null) {
            return;
        }

        if (throwable == null) {
            Main.getInstance().getLogger().warning("[MirrorAvatar] " + message);
            return;
        }

        Main.getInstance().getLogger().warning("[MirrorAvatar] " + message + ": " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
    }

    private static Object getNmsHandle(Player player) {
        try {
            Method getHandleMethod = player.getClass().getMethod("getHandle");
            return getHandleMethod.invoke(player);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getFieldValueByName(Object target, String fieldName) {
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static Object getFieldValueByType(Object target, String typeNameFragment) {
        for (Class<?> current = target.getClass(); current != null; current = current.getSuperclass()) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (!field.getType().getName().contains(typeNameFragment)) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    return field.get(target);
                } catch (Throwable ignored) {
                }
            }
        }

        return null;
    }

    private static List<String> getSelectedActivePerkNames(Player player) {
        ArrayList<String> perkNames = new ArrayList<>();

        Document document = PlayerDataCache.getPlayerPerks(player);
        if (document == null) {
            return perkNames;
        }

        Object first = document.get("first_active");
        if (first instanceof String && Cache.getActivePerks().containsKey(first)) {
            perkNames.add((String) first);
        }

        Object second = document.get("second_active");
        if (second instanceof String && Cache.getActivePerks().containsKey(second) && !perkNames.contains(second)) {
            perkNames.add((String) second);
        }

        return perkNames;
    }

    private static void addPerkBlock(Player player, String perkName, long durationMs) {
        long expiresAt = System.currentTimeMillis() + durationMs;
        HashMap<String, Long> playerBlocks = blockedPerksByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        playerBlocks.put(perkName, expiresAt);
    }

    private static void cleanupPerkBlocks() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, HashMap<String, Long>>> playerIterator = blockedPerksByPlayer.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, HashMap<String, Long>> entry = playerIterator.next();
            HashMap<String, Long> perkBlocks = entry.getValue();
            perkBlocks.entrySet().removeIf(blockedPerk -> blockedPerk.getValue() <= now);

            if (perkBlocks.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    private static boolean isPerkBlocked(Player player, String perkName) {
        cleanupPerkBlocks();
        HashMap<String, Long> perkBlocks = blockedPerksByPlayer.get(player.getUniqueId());
        return perkBlocks != null && perkBlocks.containsKey(perkName);
    }

    private static void cleanupOverclockStates() {
        long now = System.currentTimeMillis();
        overclockActiveUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
        overclockOverheatUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private static boolean isOverclockActive(Player player) {
        cleanupOverclockStates();
        Long activeUntil = overclockActiveUntil.get(player.getUniqueId());
        return activeUntil != null && activeUntil > System.currentTimeMillis();
    }

    private static boolean isOverheated(Player player) {
        cleanupOverclockStates();
        Long activeUntil = overclockActiveUntil.get(player.getUniqueId());
        if (activeUntil != null && activeUntil > System.currentTimeMillis()) {
            return false;
        }

        Long overheatUntil = overclockOverheatUntil.get(player.getUniqueId());
        return overheatUntil != null && overheatUntil > System.currentTimeMillis();
    }

    public static int applyUltimateWoolCostModifier(Player player, int baseCost) {
        if (baseCost <= 0) {
            return 0;
        }

        if (!isOverclockActive(player)) {
            return baseCost;
        }

        return Math.max(0, (int) Math.ceil(baseCost * 0.5));
    }

    public static int applyUltimateCooldownModifier(Player player, int baseCooldown) {
        if (baseCooldown <= 0) {
            return 0;
        }

        if (!isOverclockActive(player)) {
            return baseCooldown;
        }

        return Math.max(1, (int) Math.ceil(baseCooldown * 0.5));
    }

    public static boolean denyPerkUseByUltimateStates(Player player, String perkName) {
        if (isOverheated(player)) {
            player.sendActionBar(Component.text("Overheated: active perks disabled.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.6f);
            return true;
        }

        if (perkName != null && isPerkBlocked(player, perkName)) {
            player.sendActionBar(Component.text("This perk is hijacked right now.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.6f);
            return true;
        }

        return false;
    }

    private static Player getNearestEnemyPlayer(Player player, double radius) {
        return getNearestEnemyPlayerFromLocation(player, player.getLocation(), radius, new ArrayList<>());
    }

    private static Player getNearestEnemyPlayerFromLocation(Player source, Location center, double radius, List<UUID> ignoredPlayers) {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : source.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player target) || !isEnemyPlayer(source, target)) {
                continue;
            }

            if (ignoredPlayers.contains(target.getUniqueId())) {
                continue;
            }

            double distance = center.distanceSquared(target.getLocation());
            if (distance >= nearestDistance) {
                continue;
            }

            nearestDistance = distance;
            nearestPlayer = target;
        }

        return nearestPlayer;
    }

    private static List<Player> getEnemyPlayersAround(Player source, Location center, double radius) {
        ArrayList<Player> players = new ArrayList<>();
        for (Entity entity : source.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Player target && isEnemyPlayer(source, target)) {
                players.add(target);
            }
        }
        return players;
    }

    private static LivingEntity getNearestMirrorChaseTarget(Player source, Location center, double radius) {
        LivingEntity nearestTarget = null;
        double nearestDistance = Double.MAX_VALUE;

        for (LivingEntity target : getMirrorAttackTargets(source, center, radius)) {
            double distance = center.distanceSquared(target.getLocation());
            if (distance >= nearestDistance) {
                continue;
            }

            nearestDistance = distance;
            nearestTarget = target;
        }

        return nearestTarget;
    }

    private static List<LivingEntity> getMirrorAttackTargets(Player source, Location center, double radius) {
        ArrayList<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : source.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity livingEntity) || !isMirrorAttackTarget(source, livingEntity)) {
                continue;
            }

            targets.add(livingEntity);
        }

        return targets;
    }

    private static boolean isMirrorAttackTarget(Player source, LivingEntity target) {
        if (target.equals(source) || target instanceof ArmorStand || target.isDead()) {
            return false;
        }

        if (target instanceof Player targetPlayer) {
            return targetPlayer.getGameMode() == GameMode.SURVIVAL && isEnemyPlayer(source, targetPlayer);
        }

        // Test helper: allow villagers as mirror combat targets.
        return target instanceof Villager;
    }

    private static List<LivingEntity> getGravityCoreTargets(Player source, Location center, double radius) {
        ArrayList<LivingEntity> targets = new ArrayList<>();

        for (Entity entity : source.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            if (livingEntity.equals(source) || livingEntity instanceof ArmorStand) {
                continue;
            }

            if (livingEntity instanceof Player targetPlayer && !isEnemyPlayer(source, targetPlayer)) {
                continue;
            }

            targets.add(livingEntity);
        }

        return targets;
    }

    public static boolean isActivePerkSilenced(Player player) {
        cleanupExpiredSilences();
        Long silencedUntil = silencedActivePerksUntil.get(player.getUniqueId());
        return silencedUntil != null && silencedUntil > System.currentTimeMillis();
    }

    public static boolean denyPerkUseIfSilenced(Player player) {
        if (!isActivePerkSilenced(player)) {
            return false;
        }

        showSilencedFeedback(player);
        return true;
    }

    private static void cleanupExpiredSilences() {
        long now = System.currentTimeMillis();
        silencedActivePerksUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private static void showSilencedFeedback(Player player) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();
        Long lastFeedback = silenceFeedbackTimestamps.get(playerId);

        if (lastFeedback != null && now - lastFeedback < SILENCE_FEEDBACK_THROTTLE_MS) {
            return;
        }

        silenceFeedbackTimestamps.put(playerId, now);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
        player.sendActionBar(Component.text("Your active perks are disabled!", NamedTextColor.RED));
    }

    private static void applyPerkSilence(Player player, long durationInMs) {
        long newSilenceEnd = System.currentTimeMillis() + durationInMs;
        Long existingSilence = silencedActivePerksUntil.get(player.getUniqueId());
        if (existingSilence == null || existingSilence < newSilenceEnd) {
            silencedActivePerksUntil.put(player.getUniqueId(), newSilenceEnd);
        }
    }

    private static Block resolveStasisTrapTargetBlock(PlayerInteractEvent event, Player player) {
        if (event.getClickedBlock() != null) {
            return event.getClickedBlock();
        }

        return player.getTargetBlockExact(6, FluidCollisionMode.NEVER);
    }

    private static boolean canPlaceStasisTrapOn(Block targetBlock) {
        if (targetBlock == null || !targetBlock.getType().isSolid()) {
            return false;
        }

        Block blockAbove = targetBlock.getRelative(BlockFace.UP);
        return blockAbove.getType().isAir();
    }

    private static void handleStasisTrapUse(PlayerInteractEvent event, Player player, ActivePerk perk) {
        event.setCancelled(true);

        if (denyPerkUseIfSilenced(player)) {
            return;
        }

        Block targetBlock = resolveStasisTrapTargetBlock(event, player);
        if (!canPlaceStasisTrapOn(targetBlock)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
            player.sendActionBar(Component.text("Trap can only be placed on a block with free space above.", NamedTextColor.RED));
            return;
        }

        int woolCost = applyUltimateWoolCostModifier(player, perk.getWoolCost());
        if (!subtractWool(player, woolCost)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.4f);
            return;
        }

        int cooldown = applyUltimateCooldownModifier(player, perk.getCooldown());
        if (cooldown > 0) {
            ItemStack perkItem = perk.getItemStack().clone();
            perkItem.setAmount(1);
            setItemCooldown(player, perk.getSlotCache(player), perkItem, cooldown);
        }

        placeStasisTrap(player, targetBlock);
        StatsSystem.addActivePerkUsage(player);
    }

    private static void placeStasisTrap(Player player, Block targetBlock) {
        cleanupExpiredStasisTraps();

        Location trapLocation = targetBlock.getRelative(BlockFace.UP).getLocation().add(0.5, 0.05, 0.5);
        long expiresAt = System.currentTimeMillis() + STASIS_TRAP_DURATION_MS;
        String ownerTeam = getPlayerTeam(player, true);
        UUID ownerId = player.getUniqueId();
        StasisTrap trap = new StasisTrap(UUID.randomUUID(), ownerId, ownerTeam, trapLocation, expiresAt);

        stasisTraps.put(ownerId, trap);
        startStasisTrapOwnerMarker(trap);

        player.spawnParticle(Particle.END_ROD, trapLocation, 30, 0.25, 0.15, 0.25, 0.01);
        player.spawnParticle(Particle.ENCHANT, trapLocation.clone().add(0, 0.2, 0), 12, 0.25, 0.1, 0.25, 0.02);
        player.playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.2f);
        player.sendActionBar(Component.text("Stasis trap armed.", NamedTextColor.AQUA));
    }

    private static void startStasisTrapOwnerMarker(StasisTrap trap) {
        new BukkitRunnable() {
            @Override
            public void run() {
                StasisTrap currentTrap = stasisTraps.get(trap.ownerId);
                if (currentTrap == null || !currentTrap.trapId.equals(trap.trapId) || currentTrap.expiresAt <= System.currentTimeMillis()) {
                    cancel();
                    return;
                }

                Player owner = Bukkit.getPlayer(trap.ownerId);
                if (owner == null || !owner.isOnline()) {
                    return;
                }

                Location markerCenter = trap.location.clone().add(0, 0.15, 0);
                owner.spawnParticle(Particle.END_ROD, markerCenter, 18, 0.22, 0.1, 0.22, 0.01);
                owner.spawnParticle(Particle.ENCHANT, markerCenter.clone().add(0, 0.15, 0), 10, 0.2, 0.08, 0.2, 0.02);
            }
        }.runTaskTimer(Main.getInstance(), 0L, STASIS_OWNER_MARKER_INTERVAL_TICKS);
    }

    private static void cleanupExpiredStasisTraps() {
        long now = System.currentTimeMillis();
        stasisTraps.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }

    private static void handleStasisTrapTrigger(Player movingPlayer) {
        if (stasisTraps.isEmpty()) {
            return;
        }

        UUID movingPlayerId = movingPlayer.getUniqueId();
        String movingTeam = getPlayerTeam(movingPlayer, true);
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, StasisTrap>> iterator = stasisTraps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, StasisTrap> entry = iterator.next();
            StasisTrap trap = entry.getValue();

            if (trap.expiresAt <= now) {
                iterator.remove();
                continue;
            }

            if (trap.ownerId.equals(movingPlayerId)) {
                continue;
            }

            if (trap.ownerTeam != null && trap.ownerTeam.equals(movingTeam)) {
                continue;
            }

            World trapWorld = trap.location.getWorld();
            World playerWorld = movingPlayer.getWorld();
            if (trapWorld == null || playerWorld == null || !trapWorld.equals(playerWorld)) {
                continue;
            }

            if (trap.location.distanceSquared(movingPlayer.getLocation()) > STASIS_TRAP_TRIGGER_RADIUS_SQ) {
                continue;
            }

            iterator.remove();
            movingPlayer.setVelocity(new Vector(0, 0, 0));
            movingPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 26, 6, false, false, true));
            movingPlayer.playSound(movingPlayer.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 0.9f);
            movingPlayer.sendActionBar(Component.text("You stepped into a stasis trap!", NamedTextColor.RED));

            Player owner = Bukkit.getPlayer(trap.ownerId);
            if (owner != null && owner.isOnline()) {
                owner.sendActionBar(Component.text(movingPlayer.getName() + " triggered your stasis trap.", NamedTextColor.GOLD));
            }

            break;
        }
    }

    private static void cleanupExpiredRescueAnchors() {
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, Long>> anchorIterator = rescueAnchorExpiresAt.entrySet().iterator();
        while (anchorIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = anchorIterator.next();
            if (entry.getValue() > now) {
                continue;
            }

            UUID playerId = entry.getKey();
            anchorIterator.remove();
            rescueAnchorLocations.remove(playerId);
        }

        rescueAnchorCooldowns.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private static void clearRescueAnchor(UUID playerId) {
        rescueAnchorLocations.remove(playerId);
        rescueAnchorExpiresAt.remove(playerId);
    }

    private static void handleRescueAnchorUse(PlayerInteractEvent event, Player player, ActivePerk perk) {
        event.setCancelled(true);

        if (denyPerkUseIfSilenced(player)) {
            return;
        }

        cleanupExpiredRescueAnchors();

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long cooldownEnd = rescueAnchorCooldowns.get(playerId);
        if (cooldownEnd != null && cooldownEnd > now) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
            return;
        }

        Location existingAnchor = rescueAnchorLocations.get(playerId);
        Long expiresAt = rescueAnchorExpiresAt.get(playerId);

        if (existingAnchor == null || expiresAt == null || expiresAt <= now) {
            rescueAnchorLocations.put(playerId, player.getLocation().clone());
            rescueAnchorExpiresAt.put(playerId, now + RESCUE_ANCHOR_WINDOW_MS);

            player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.1f);
            player.sendActionBar(Component.text("Anchor saved. Click again within 6 seconds.", NamedTextColor.AQUA));
            return;
        }

        int woolCost = applyUltimateWoolCostModifier(player, perk.getWoolCost());
        if (!subtractWool(player, woolCost)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.4f);
            return;
        }

        int cooldown = applyUltimateCooldownModifier(player, perk.getCooldown());
        if (cooldown > 0) {
            ItemStack perkItem = perk.getItemStack().clone();
            perkItem.setAmount(1);
            setItemCooldown(player, perk.getSlotCache(player), perkItem, cooldown);
            rescueAnchorCooldowns.put(playerId, now + (cooldown * 1000L));
        }

        Location teleportLocation = existingAnchor.clone();
        if (teleportLocation.getWorld() == null) {
            teleportLocation.setWorld(player.getWorld());
        }

        player.teleport(teleportLocation);
        player.setFallDistance(0f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        clearRescueAnchor(playerId);
        StatsSystem.addActivePerkUsage(player);
    }

    private static boolean isEnemyPlayer(Player source, Player target) {
        if (source.equals(target)) {
            return false;
        }

        String sourceTeam = getPlayerTeam(source, true);
        String targetTeam = getPlayerTeam(target, true);
        return sourceTeam == null || !sourceTeam.equals(targetTeam);
    }

    private static void executeImpulseWave(Player player) {
        double radius = 4.0;
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof LivingEntity) || entity.equals(player)) {
                continue;
            }

            if (entity instanceof ArmorStand) {
                continue;
            }

            if (entity instanceof Player target && !isEnemyPlayer(player, target)) {
                continue;
            }

            Vector pushVector = entity.getLocation().toVector().subtract(player.getLocation().toVector());
            pushVector.setY(0.25);

            if (pushVector.lengthSquared() < 0.0001) {
                pushVector = player.getLocation().getDirection().clone().setY(0.25);
            }

            entity.setVelocity(pushVector.normalize().multiply(1.35).setY(0.38));
        }

        Location center = player.getLocation().clone().add(0, 1.0, 0);
        World world = player.getWorld();

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                double currentRadius = 0.6 + (step * 0.5);
                if (currentRadius > radius) {
                    cancel();
                    return;
                }

                int points = Math.max(22, (int) Math.round(currentRadius * 30));
                for (int i = 0; i < points; i++) {
                    double angle = (Math.PI * 2 * i) / points;
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;

                    Location ringPoint = center.clone().add(x, 0, z);
                    world.spawnParticle(Particle.CLOUD, ringPoint, 2, 0.03, 0.03, 0.03, 0.0);
                    world.spawnParticle(Particle.SWEEP_ATTACK, ringPoint, 1, 0.0, 0.0, 0.0, 0.0);
                }

                world.spawnParticle(Particle.EXPLOSION, center, 1, 0.05, 0.05, 0.05, 0.0);
                step++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 1L);

        world.spawnParticle(Particle.CLOUD, center, 120, 0.4, 0.2, 0.4, 0.06);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.8f, 1.6f);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
    }

    private static void executeSmokeGrenade(Player player) {
        Location center = player.getLocation().clone().add(player.getLocation().getDirection().clone().normalize().multiply(4));
        center.setY(player.getLocation().getY() + 1.0);

        World world = player.getWorld();

        new BukkitRunnable() {
            int elapsedTicks = 0;

            @Override
            public void run() {
                if (elapsedTicks >= 120) {
                    cancel();
                    return;
                }

                world.spawnParticle(Particle.CLOUD, center, 70, 2.6, 0.9, 2.6, 0.015);
                world.spawnParticle(Particle.LARGE_SMOKE, center, 50, 2.2, 0.8, 2.2, 0.01);
                world.spawnParticle(Particle.SMOKE, center, 100, 2.6, 0.9, 2.6, 0.015);
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 35, 2.1, 0.7, 2.1, 0.01);

                for (Entity entity : world.getNearbyEntities(center, 4.6, 2.6, 4.6)) {
                    if (!(entity instanceof Player target) || !isEnemyPlayer(player, target)) {
                        continue;
                    }

                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 1, false, false, true));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 50, 0, false, false, true));
                }

                elapsedTicks += 2;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);

        player.playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 0.7f, 1.7f);
    }

    private static void executeDisarmPulse(Player player) {
        double radius = 5.0;
        int affectedPlayers = 0;

        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), radius, radius, radius)) {
            if (!(entity instanceof Player target) || !isEnemyPlayer(player, target)) {
                continue;
            }

            applyPerkSilence(target, DISARM_PULSE_DURATION_MS);
            target.sendActionBar(Component.text("Your active perks are disabled for 4 seconds!", NamedTextColor.RED));
            target.playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.7f);
            affectedPlayers++;
        }

        if (affectedPlayers > 0) {
            player.sendActionBar(Component.text("Disarmed " + affectedPlayers + " player(s).", NamedTextColor.GOLD));
        }

        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 24, 1.2, 0.5, 1.2, 0.03);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);
    }

    private static void executeBridgePush(Player player) {
        World world = player.getWorld();
        DyeColor teamColor = findTeamDyeColor(player);
        Vector direction = player.getLocation().getDirection().clone().setY(0);

        if (direction.lengthSquared() < 0.0001) {
            return;
        }

        direction.normalize();
        Location origin = player.getLocation();
        int bridgeY = origin.getBlockY() - 1;

        for (int i = 1; i <= 10; i++) {
            Location target = origin.clone().add(direction.clone().multiply(i));
            Block bridgeBlock = world.getBlockAt(target.getBlockX(), bridgeY, target.getBlockZ());

            if (bridgeBlock.getType() != Material.AIR) {
                continue;
            }

            bridgeBlock.setType(WoolHelper.getWoolMaterial(teamColor));
            BlockBreakingSystem.trackPlacedBlock(bridgeBlock.getLocation());
        }

        player.playSound(player.getLocation(), Sound.BLOCK_WOOL_PLACE, 1.0f, 1.0f);
    }

    /**
     * The PlayerMoveEvent Event is duplicated here for the jump Platform perk.
     * @param event the PlayerMoveEvent event
     * @author SimsumMC
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        cleanupExpiredStasisTraps();
        cleanupExpiredRescueAnchors();

        if (event.getTo() != null
                && (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ())) {
            handleStasisTrapTrigger(player);
        }

        Location playerLocation = player.getLocation();

        Location location = playerLocation.clone().subtract(0, 1, 0);

        Block block = location.getBlock();
        ArrayList<Block> nearbyWoolBlocks = new ArrayList<>();

        nearbyWoolBlocks.add(block);

        if(!WoolHelper.isWool(block.getType())){

            World world = playerLocation.getWorld();
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();

            nearbyWoolBlocks.add(new Location(world,x-1, y, z).getBlock());
            nearbyWoolBlocks.add(new Location(world,x-1, y, z).getBlock());
            nearbyWoolBlocks.add(new Location(world,x-1, y, z-1).getBlock());
            nearbyWoolBlocks.add(new Location(world, x, y, z+1).getBlock());
            nearbyWoolBlocks.add(new Location(world, x, y, z).getBlock());
            nearbyWoolBlocks.add(new Location(world, x,y, z-1).getBlock());
            nearbyWoolBlocks.add(new Location(world,x+1, y, z+1).getBlock());
            nearbyWoolBlocks.add(new Location(world,x+1, y, z).getBlock());
            nearbyWoolBlocks.add(new Location(world,x+1, y, z-1).getBlock());
            nearbyWoolBlocks.removeIf(nearbyBlock -> !WoolHelper.isWool(nearbyBlock.getType()));
        }

        HashMap<Player, ArrayList<ArrayList<Block>>> jumpPlatformBlocks = Cache.getJumpPlatformBlocks();

        if(!jumpPlatformBlocks.containsKey(player)){
            return;
        }

        ArrayList<ArrayList<Block>> playerBlocks = jumpPlatformBlocks.get(player);

        if(playerBlocks.isEmpty()){
            return;
        }

        for(ArrayList<Block> array : playerBlocks){
            for(Block nearbyBlock : nearbyWoolBlocks) {
                if (array.contains(nearbyBlock)) {
                    for (Block existingJumpBlock : array) {
                        existingJumpBlock.setType(Material.AIR);
                    }

                    player.setVelocity(location.getDirection().multiply(1).setY(3));

                }
            }

        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        lastGrapplingUse.remove(playerId);
        anchoredHookLocations.remove(playerId);
        silencedActivePerksUntil.remove(playerId);
        silenceFeedbackTimestamps.remove(playerId);

        clearRescueAnchor(playerId);
        rescueAnchorCooldowns.remove(playerId);

        stasisTraps.remove(playerId);
        stasisTraps.entrySet().removeIf(entry -> entry.getValue().ownerId.equals(playerId));

        stopGravityCorePreview(playerId);

        removeSyntheticArrow(event.getPlayer());

        BukkitTask minigunTask = minigunTasks.remove(playerId);
        if (minigunTask != null) {
            minigunTask.cancel();
        }

        ultimateCharges.remove(playerId);
        ultimateReadyNotified.remove(playerId);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if(event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.EGG){
            event.setCancelled(true);
        }
    }

    private static void applyConfiguredPerkTexture(String perkName, ActivePerk perk) {
        Integer customModelData = Config.getPerkCustomModelData(perkName);
        if (customModelData != null && customModelData > 0) {
            perk.setCustomModelData(customModelData);
        }
    }

    public static void load(){

        ActivePerk shears = new ActivePerk(new ItemStack(Material.SHEARS), 0, 0, false, false)
                .setItemName(Component.text("Shears", NamedTextColor.AQUA))
                .addEnchantment(Enchantment.EFFICIENCY, 5, false)
                .addEnchantment(Enchantment.UNBREAKING, 10, false)
                .addEnchantment(Enchantment.KNOCKBACK, 5, false);

        ItemStack shearsItemStack = shears.getItemStack();
        ItemMeta shearsItemMeta = shearsItemStack.getItemMeta();
        shearsItemMeta.setUnbreakable(true);
        shearsItemStack.setItemMeta(shearsItemMeta);
        shears.setItemStack(shearsItemStack);

        applyConfiguredPerkTexture("Shears", shears);

        shears.register();

        ActivePerk bow = new ActivePerk(new ItemStack(Material.BOW), 0, 1, false, false)
                .setItemName(Component.text("Bow", NamedTextColor.AQUA))
                .addEnchantment(Enchantment.UNBREAKING, 10, false)
                .addEnchantment(Enchantment.KNOCKBACK, 5, false)
                .addEnchantment(Enchantment.PUNCH, 5, false)
                .addEnchantment(Enchantment.INFINITY, 1, false);

        ItemStack bowItemStack = bow.getItemStack();
        ItemMeta bowItemMeta = bowItemStack.getItemMeta();
        bowItemMeta.setUnbreakable(true);
        bowItemStack.setItemMeta(bowItemMeta);
        bow.setItemStack(bowItemStack);

        applyConfiguredPerkTexture("Bow", bow);

        bow.register();

        ActivePerk enderPearl = new ActivePerk(new ItemStack(Material.ENDER_PEARL), 5, 5, false, false)
                .setItemName(Component.text("Ender Pearl", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true);

        applyConfiguredPerkTexture("Ender Pearl", enderPearl);

        enderPearl.register();

        ActivePerk rescuePlatform = new ActivePerk(new ItemStack(Material.BLAZE_ROD), 15, 25, true){
            @Override
            public void onExecute(PlayerInteractEvent event, Player player){
                Location playerLocation = player.getLocation();
                DyeColor teamColor = findTeamDyeColor(player);

                World world = playerLocation.getWorld();
                double x = playerLocation.getX();
                double y = playerLocation.getY();
                double z = playerLocation.getZ();

                ArrayList<Location> locations = new ArrayList<Location>(){{
                    add(new Location(world, x, y -5, z));
                    add(new Location(world, x, y -5, z+1));
                    add(new Location(world, x, y -5, z+2));
                    add(new Location(world, x, y -5, z-1));
                    add(new Location(world, x, y -5, z-2));
                    add(new Location(world, x+1, y -5, z));
                    add(new Location(world, x+1, y -5, z+1));
                    add(new Location(world, x+1, y -5, z+2));
                    add(new Location(world, x+1, y -5, z-1));
                    add(new Location(world, x+1, y -5, z-2));
                    add(new Location(world, x+2, y -5, z));
                    add(new Location(world, x+2, y -5, z+1));
                    add(new Location(world, x+2, y -5, z-1));
                    add(new Location(world, x-1, y -5, z));
                    add(new Location(world, x-1, y -5, z+1));
                    add(new Location(world, x-1, y -5, z+2));
                    add(new Location(world, x-1, y -5, z-1));
                    add(new Location(world, x-1, y -5, z-2));
                    add(new Location(world, x-2, y -5, z));
                    add(new Location(world, x-2, y -5, z+1));
                    add(new Location(world, x-2, y -5, z-1));
                }};

                for(Location location : locations){
                    Block block = location.getBlock();
                    Material material = block.getType();
                    if(material != Material.AIR){
                        continue;
                    }
                    block.setType(WoolHelper.getWoolMaterial(teamColor));
                    BlockBreakingSystem.trackPlacedBlock(location);
                }
            }
        }.setItemName(Component.text("Rescue Platform", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
         .setDescription("Places blocks under you.");

        applyConfiguredPerkTexture("Rescue Platform", rescuePlatform);

        rescuePlatform.register();

        ActivePerk exchanger = new ActivePerk(new ItemStack(Material.SNOWBALL), 15, 10, false)
                .setItemName(Component.text("Exchanger", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
                .setDescription("Swap your Location with another player.");
        //no onExecute method here, see onProjectileLaunch event

        applyConfiguredPerkTexture("Exchanger", exchanger);

        exchanger.register();

        ActivePerk jumpPlatform = new ActivePerk(new ItemStack(Material.SLIME_BALL), 15, 25, true){
            @Override
            public void onExecute(PlayerInteractEvent event, Player player){
                Location playerLocation = player.getLocation();
                DyeColor teamColor = findTeamDyeColor(player);

                World world = playerLocation.getWorld();
                double x = playerLocation.getX();
                double y = playerLocation.getY();
                double z = playerLocation.getZ();

                ArrayList<Location> locations = new ArrayList<Location>(){{
                    add(new Location(world, x, y -5, z));
                    add(new Location(world, x, y -5, z+1));
                    add(new Location(world, x, y -5, z-1));
                    add(new Location(world, x+1, y -5, z));
                    add(new Location(world, x-1, y -5, z));
                }};

                HashMap<Player, ArrayList<ArrayList<Block>>> jumpPlatformBlocks = Cache.getJumpPlatformBlocks();

                jumpPlatformBlocks.put(player, null);

                ArrayList<ArrayList<Block>> playerBlocks = new ArrayList<>();

                ArrayList<Block> newPlayerBlocks = new ArrayList<>();

                for(Location location : locations){
                    Block block = location.getBlock();
                    Material material = block.getType();

                    if(material != Material.AIR){
                        continue;
                    }

                    newPlayerBlocks.add(block);

                    block.setType(WoolHelper.getWoolMaterial(teamColor));
                    BlockBreakingSystem.trackPlacedBlock(location);
                }

                playerBlocks.add(newPlayerBlocks);

                jumpPlatformBlocks.put(player, playerBlocks);

                Cache.setJumpPlatformBlocks(jumpPlatformBlocks);

            }
        }.setItemName(Component.text("Jump Platform", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
         .setDescription("Boosts yourself up.");

        applyConfiguredPerkTexture("Jump Platform", jumpPlatform);

        jumpPlatform.register();

        ActivePerk grapplingHook = new ActivePerk(new ItemStack(Material.FISHING_ROD), 5, 5, false)
                .setItemName(Component.text("Grappling Hook", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
                .setDescription("Helps you go fast from one point to another.");

        applyConfiguredPerkTexture("Grappling Hook", grapplingHook);

        grapplingHook.register();

        ActivePerk homeTeleport = new ActivePerk(new ItemStack(Material.CLOCK), 30, 25, true){
            @Override
            public void onExecute(PlayerInteractEvent event, Player player) {
                teleportPlayerTeamSpawn(player);
            }
        }.setItemName(Component.text("Home Teleport", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
         .setDescription("Teleports you home.");

        applyConfiguredPerkTexture("Home Teleport", homeTeleport);

        homeTeleport.register();

        ActivePerk rescuePod = new ActivePerk(new ItemStack(Material.FEATHER), 15, 30, true){
            @Override
            public void onExecute(PlayerInteractEvent event, Player player){
                Location playerLocation = player.getLocation();
                DyeColor teamColor = findTeamDyeColor(player);

                World world = playerLocation.getWorld();
                double x = playerLocation.getX();
                double y = playerLocation.getY();
                double z = playerLocation.getZ();

                ArrayList<Location> locations = new ArrayList<Location>(){{
                    add(new Location(world, x, y -1, z));
                    add(new Location(world, x, y+2 , z));
                    add(new Location(world, x, y , z+1));
                    add(new Location(world, x, y , z-1));
                    add(new Location(world, x+1, y , z));
                    add(new Location(world, x-1, y , z));
                    add(new Location(world, x, y+1, z+1));
                    add(new Location(world, x, y+1, z-1));
                    add(new Location(world, x+1, y+1, z));
                    add(new Location(world, x-1, y+1, z));
                }};

                player.teleport(new Location(world, x, y, z));

                for(Location location : locations){
                    Block block = location.getBlock();
                    Material material = block.getType();
                    if(material != Material.AIR){
                        continue;
                    }
                    block.setType(WoolHelper.getWoolMaterial(teamColor));
                    BlockBreakingSystem.trackPlacedBlock(location);
                }
            }
        }.setItemName(Component.text("Rescue Pod", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
         .setDescription("Places blocks around you.");

        applyConfiguredPerkTexture("Rescue Pod", rescuePod);

        rescuePod.register();

        ActivePerk duel = new ActivePerk(new ItemStack(Material.WOODEN_SWORD), 30, 10, false)
                .setItemName(Component.text("Duel", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
                .setDescription("Puts you in a 1V1 with the hit player.");

        applyConfiguredPerkTexture("Duel", duel);

        duel.register();

        ActivePerk impulswelle = new ActivePerk(new ItemStack(Material.HEART_OF_THE_SEA), 26, 10, true) {
            @Override
            public void onExecute(PlayerInteractEvent event, Player player) {
                executeImpulseWave(player);
            }
        }.setItemName(Component.text("Impulswelle", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
         .setDescription("Stoesst nahe Gegner zurueck.");

        applyConfiguredPerkTexture("Impulswelle", impulswelle);

        impulswelle.register();

        ActivePerk stasisfalle = new ActivePerk(new ItemStack(Material.TRIPWIRE_HOOK), 28, 12, false)
        .setItemName(Component.text("Stasisfalle", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
         .setDescription("Legt eine unsichtbare Falle fuer Gegner.");

        applyConfiguredPerkTexture("Stasisfalle", stasisfalle);

        stasisfalle.register();

        ActivePerk rettungsanker = new ActivePerk(new ItemStack(Material.RESPAWN_ANCHOR), 40, 12, false)
                .setItemName(Component.text("Rettungsanker", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
                .setDescription("Erster Klick speichert Position, zweiter teleportiert zurueck.");

        applyConfiguredPerkTexture("Rettungsanker", rettungsanker);

        rettungsanker.register();

        ActivePerk nebelgranate = new ActivePerk(new ItemStack(Material.GUNPOWDER), 32, 15, true) {
            @Override
            public void onExecute(PlayerInteractEvent event, Player player) {
                executeSmokeGrenade(player);
            }
        }.setItemName(Component.text("Nebelgranate", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
         .setDescription("Erzeugt Nebel und blendet Gegner im Bereich.");

        applyConfiguredPerkTexture("Nebelgranate", nebelgranate);

        nebelgranate.register();

        ActivePerk entwaffnerPuls = new ActivePerk(new ItemStack(Material.AMETHYST_SHARD), 55, 20, true) {
            @Override
            public void onExecute(PlayerInteractEvent event, Player player) {
                executeDisarmPulse(player);
            }
        }.setItemName(Component.text("Entwaffner-Puls", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
         .setDescription("Blockiert aktive Perks von Gegnern fuer 4 Sekunden.");

        applyConfiguredPerkTexture("Entwaffner-Puls", entwaffnerPuls);

        entwaffnerPuls.register();

        ActivePerk brueckenstoss = new ActivePerk(new ItemStack(Material.SCAFFOLDING), 35, 10, true) {
            @Override
            public void onExecute(PlayerInteractEvent event, Player player) {
                executeBridgePush(player);
            }
        }.setItemName(Component.text("Brueckenstoss", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
         .setDescription("Baut eine kurze Wollbruecke nach vorne.");

        applyConfiguredPerkTexture("Brueckenstoss", brueckenstoss);

        brueckenstoss.register();

        ActivePerk Egg = new ActivePerk(new ItemStack(Material.EGG), 0, 1, false)
                .setItemName(Component.text("Egg", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
                .setDescription("Basically a Bow for Eggs.");

        applyConfiguredPerkTexture("Egg", Egg);

        Egg.register();

    }

}
