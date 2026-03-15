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
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import woolbattle.woolbattle.WoolHelper;
import woolbattle.woolbattle.woolsystem.BlockBreakingSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import woolbattle.woolbattle.Cache;
import woolbattle.woolbattle.Config;
import woolbattle.woolbattle.Main;
import woolbattle.woolbattle.stats.StatsSystem;

import java.util.ArrayList;
import java.util.HashMap;

import static woolbattle.woolbattle.base.Base.addEnderPearl;
import static woolbattle.woolbattle.itemsystem.ItemSystem.setItemCooldown;
import static woolbattle.woolbattle.itemsystem.ItemSystem.subtractWool;
import static woolbattle.woolbattle.lives.LivesSystem.teleportPlayerTeamSpawn;
import static woolbattle.woolbattle.team.TeamSystem.*;

public class AllActivePerks implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if(!(event.getEntity().getShooter() instanceof Player)){
            return;
        }

        Player player = (Player) projectile.getShooter();

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

            ActivePerk perk = Cache.getActivePerks().get(perkName);
            ItemStack itemStack = perk.getItemStack();
            itemStack.setAmount(1);

            int woolCost = perk.getWoolCost();
            int cooldown = perk.getCooldown();
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
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof Player)){
            return;
        }
        if(event.getDamager() instanceof Player){
            Player player = (Player) event.getDamager();
            Player damagedPlayer = (Player) event.getEntity();
            Component itemDisplayName = player.getInventory().getItemInMainHand().hasItemMeta() ? player.getInventory().getItemInMainHand().getItemMeta().displayName() : null;
            String itemPlainName = itemDisplayName != null ? PlainTextComponentSerializer.plainText().serialize(itemDisplayName) : "";
            if(itemPlainName.equals("Duel")){
                event.setCancelled(true);
                ActivePerk perk = Cache.getActivePerks().get("Duel");

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

                int woolCost = perk.getWoolCost();
                int cooldown = perk.getCooldown();

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
        if (event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT || event.getState() == PlayerFishEvent.State.IN_GROUND) {
            FishHook hook = event.getHook();
            ActivePerk perk = Cache.getActivePerks().get("Grappling Hook");
            ItemStack itemStack = perk.getItemStack();

            int woolCost = perk.getWoolCost();
            int cooldown = perk.getCooldown();
            int perkSlot;

            perkSlot = perk.getSlotCache(player);

            if(!subtractWool(player, woolCost)){
                event.setCancelled(true);
                hook.remove();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.4f);
                return;
            }
            else{
                if(cooldown != 0) {
                    setItemCooldown(player, perkSlot, itemStack, cooldown);
                }
            }

            Location playerLocation = player.getLocation();
            Location hookLocation = hook.getLocation();
            Vector vector = new Vector(hookLocation.getX() - playerLocation.getX(), 1.0, hookLocation.getZ() - playerLocation.getZ());
            player.setVelocity(vector);

            StatsSystem.addActivePerkUsage(player);
        }
    }

    /**
     * The PlayerMoveEvent Event is duplicated here for the jump Platform perk.
     * @param event the PlayerMoveEvent event
     * @author SimsumMC
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();

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

        ActivePerk knockbackStick = new ActivePerk(new ItemStack(Material.STICK), 0, 0, false)
                .setItemName(Component.text("Knockback Stick", NamedTextColor.AQUA))
                .addEnchantment(Enchantment.KNOCKBACK,100, false)
                .setDescription("Best weapon in the game.");

        applyConfiguredPerkTexture("Knockback Stick", knockbackStick);

        knockbackStick.register();

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

        ActivePerk Egg = new ActivePerk(new ItemStack(Material.EGG), 0, 1, false)
                .setItemName(Component.text("Egg", NamedTextColor.AQUA)).addEnchantment(Enchantment.UNBREAKING, true)
                .setDescription("Basically a Bow for Eggs.");

        applyConfiguredPerkTexture("Egg", Egg);

        Egg.register();

    }

}
