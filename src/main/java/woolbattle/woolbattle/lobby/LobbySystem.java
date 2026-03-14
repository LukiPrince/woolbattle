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

package woolbattle.woolbattle.lobby;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import woolbattle.woolbattle.Cache;
import woolbattle.woolbattle.Config;
import woolbattle.woolbattle.MapConfig;
import woolbattle.woolbattle.Enums.PerkType;
import woolbattle.woolbattle.Main;
import woolbattle.woolbattle.achievements.AchievementUI;
import woolbattle.woolbattle.itemsystem.ItemSystem;
import woolbattle.woolbattle.perks.ActivePerk;
import woolbattle.woolbattle.perks.AllPassivePerks;
import woolbattle.woolbattle.perks.PassivePerk;
import woolbattle.woolbattle.stats.StatsSystem;
import woolbattle.woolbattle.team.TeamSystem;
import woolbattle.woolbattle.woolsystem.BlockBreakingSystem;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static woolbattle.woolbattle.lives.LivesSystem.setPlayerSpawnProtection;
import static woolbattle.woolbattle.stats.StatsSystem.addDefaultStats;

public class LobbySystem implements Listener {

    public static boolean gameStarted = false;
    public static boolean runCooldownTask = false;
    public static boolean runScoreBoardTask = false;

    private static String plainName(ItemMeta meta) {
        Component display = meta.displayName();
        return display != null ? PlainTextComponentSerializer.plainText().serialize(display) : "";
    }
    private static int cooldown = Config.startCooldown;
    public static int teamLimit = Config.teamSize;

    /**
     * An Event that gets executed whenever a player dies to send a custom death message.
     *
     * @param event the PlayerDeathEvent event
     * @author SimsumMC
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        // this is basically an unnecessary event -> only gets called when something goes wrong
        Player player = event.getEntity();
        event.deathMessage(Component.text("The player ", NamedTextColor.GRAY)
                .append(Component.text(player.getName(), NamedTextColor.GREEN))
                .append(Component.text(" died.", NamedTextColor.GRAY)));
        if(!gameStarted){
            giveLobbyItems(player);
        }

    }

    /**
     * An Event that gets executed whenever a player joins the server to send a custom join message, set the
     * right GameMode, update the ScoreBoard, teleport to the right position or maybe start the cooldown.
     *
     * @param event the PlayerJoinEvent event
     * @author SimsumMC
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        event.joinMessage(Component.text("The player ", NamedTextColor.GRAY)
                .append(Component.text(player.getName(), NamedTextColor.GREEN))
                .append(Component.text(" joined the game.", NamedTextColor.GRAY)));

        if (gameStarted) {
            setGameScoreBoard(player);
            setPlayerSpectator(player);
            player.sendMessage(Component.text("There is already a running game!", NamedTextColor.RED));
        } else {
            setLobbyScoreBoard(player);
            giveLobbyItems(player);
            player.teleport(MapConfig.lobbyLocation);
        }
        if (!runCooldownTask) {
            updatePlayerCooldown();
        }

        if (!runScoreBoardTask) {
            updateScoreBoard();
        }

    }

    /**
     * An Event that gets executed whenever a player leaves the server to send a custom death message and
     * eventually end the current game.
     *
     * @param event the PlayerQuitEvent event
     * @author SimsumMC
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        event.quitMessage(Component.text("The player ", NamedTextColor.GRAY)
                .append(Component.text(player.getName(), NamedTextColor.GREEN))
                .append(Component.text(" left the game.", NamedTextColor.GRAY)));

        // remove voting if any
        HashMap<Integer, ArrayList<Player>> lifeVoting = Cache.getLifeVoting();
        for (Integer key : lifeVoting.keySet()) {
            ArrayList<Player> players = lifeVoting.get(key);
            if (players.contains(player)) {
                players.remove(player);
                lifeVoting.put(key, players);
                Cache.setLifeVoting(lifeVoting);
                break;
            }
        }

        TeamSystem.removePlayerTeam(player);

        determinateWinnerTeam();
    }

    /**
     * An Event that gets executed whenever a player tries to move an item in the inventory to prevent the moving of
     * lobby items.
     *
     * @param event the InventoryClickEvent event
     * @author SimsumMC & Beelzebub
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();

        if(event.getCurrentItem() == null){
            return;
        }

        if(event.getCurrentItem().getType() == Material.GUNPOWDER){
            player.sendMessage(Component.text("You can't move items that are on cooldown!", NamedTextColor.RED));
            event.setCancelled(true);
        }
        if (gameStarted) {
            return;
        }

        if (event.getWhoClicked() instanceof Player && event.getClickedInventory() != null && event.getCurrentItem().getItemMeta() != null) {
            if (!event.getView().title().equals(Component.text("Edit Inventory", NamedTextColor.AQUA)) || plainName(event.getCurrentItem().getItemMeta()).equals(" ")) {
                List<ItemStack> items = new ArrayList<>();
                items.add(event.getCurrentItem());
                items.add(event.getCursor());
                items.add((event.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) ?
                        event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) : event.getCurrentItem());
                for (ItemStack item : items) {
                    if (item != null && item.hasItemMeta()) {
                        event.setCancelled(true);
                    }
                }
            }
        }

        if(event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory() || !event.getCurrentItem().hasItemMeta() || plainName(event.getCurrentItem().getItemMeta()).equals(" ")) {
            return;
        }

        String rawInventoryName = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String rawItemName = plainName(event.getCurrentItem().getItemMeta());

        if(rawItemName == null){
            return;
        }
        switch(rawInventoryName) {
            case "Amount of Lives Voting":
                HashMap<Integer, ArrayList<Player>> votingData = Cache.getLifeVoting();
                ItemMeta clickedItemMeta = event.getCurrentItem().getItemMeta();
                int lifeAmount = Integer.parseInt(plainName(clickedItemMeta).split(" ")[0]);

                //doesn't change anything if the player already voted for the given value
                if (votingData.get(lifeAmount).contains(player)) {
                    return;
                }

                //check if the player voted for another value before, if true remove the vote there
                for (Integer key : votingData.keySet()) {
                    ArrayList<Player> players = votingData.get(key);
                    if (players.contains(player)) {
                        players.remove(player);
                        votingData.put(key, players);
                    }
                }

                //update the cache
                ArrayList<Player> players = votingData.get(lifeAmount);
                players.add(player);
                votingData.put(lifeAmount, players);
                Cache.setLifeVoting(votingData);

                //update the inventory
                showLifeAmountVoting(player);
                break;
            case "Team Selecting":
                String teamName = plainName(event.getCurrentItem().getItemMeta()).substring(5);
                TextColor teamColor = TeamSystem.getTeamColour(teamName);

                if ((Cache.getTeamMembers().get(teamName)).contains(player)) {
                    return;

                } else if ((Cache.getTeamMembers().get(teamName)).size() >= teamLimit) {
                    player.sendMessage(Component.text("The team already has " + teamLimit + " Members!", NamedTextColor.RED));

                } else {
                    TeamSystem.removePlayerTeam(player);
                    (Cache.getTeamMembers().get(teamName)).add(player);
                    player.sendMessage(Component.text("You have entered team ", NamedTextColor.GRAY)
                            .append(Component.text(teamName, teamColor))
                            .append(Component.text(".", NamedTextColor.GRAY)));
                }

                TeamSystem.showTeamSelectionInventory((Player) event.getWhoClicked());

                break;
            case "Choose Perks":
                switch(rawItemName){
                    case "Active Perk #1":
                        showActivePerkMenu(player, PerkType.FIRST_ACTIVE);
                        break;
                    case "Active Perk #2":
                        showActivePerkMenu(player, PerkType.SECOND_ACTIVE);
                        break;
                    case "Passive Perk":
                        showPassivePerkMenu(player);
                        break;
                }

                break;
            case "Active Perk #1":
                if(rawItemName.equals("Go Back")){
                    showPerkMenu(player);
                }
                else{
                    savePerkSelection(player, rawItemName, PerkType.FIRST_ACTIVE);
                    showActivePerkMenu(player, PerkType.FIRST_ACTIVE);
                }
                break;
            case "Active Perk #2":
                if(rawItemName.equals("Go Back")){
                    showPerkMenu(player);
                }
                else {
                    savePerkSelection(player, rawItemName, PerkType.SECOND_ACTIVE);
                    showActivePerkMenu(player, PerkType.SECOND_ACTIVE);
                }
                break;
            case "Passive Perk":
                if(rawItemName.equals("Go Back")){
                    showPerkMenu(player);
                }
                else {
                    savePerkSelection(player, rawItemName, PerkType.PASSIVE);
                    showPassivePerkMenu(player);
                }
                break;
        }
    }


    /**
     * An Event that gets executed whenever a player interacts with an item to make the lobby items functional.
     * @param event the PlayerInteractEvent event
     * @author SimsumMC
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getItemMeta() == null || event.getItem().getItemMeta().displayName() == null) return;

        Player player = event.getPlayer();
        String displayName = plainName(event.getItem().getItemMeta());

        switch (displayName) {
            case "Achievements":
                AchievementUI.showAchievementGUI(player);
                break;
            case "Leave":
                player.kick(Component.text("You left the game.", NamedTextColor.RED, TextDecoration.BOLD));
                break;
            case "Amount of Lives":
                showLifeAmountVoting(player);
                break;
            case "Edit Inventory":
                showEditInventoryMenu(player);
                break;
            case "Perks":
                showPerkMenu(player);
                break;
            case "Team Selecting":
                TeamSystem.showTeamSelectionInventory(player);
                break;
        }

        ActivePerk activePerk = Cache.getActivePerks().get(displayName);
        if(activePerk != null){
            activePerk.execute(event, player);
        }

    }

    /**
     * An Event that gets executed whenever a player closes the current inventory which is required to save the new
     * inventory of the "Edit Inventory" Item
     * @param event the InventoryCloseEvent event
     * @author SimsumMC
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if(!event.getView().title().equals(Component.text("Edit Inventory", NamedTextColor.AQUA))){
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory eventInventory = event.getInventory();

        int shearsPosition = 0;
        int bowPosition = 0;
        int enderPearlPosition = 0;
        int activePerk1Position = 0;
        int activePerk2Position = 0;
        int position = 0;

        for (ItemStack itemStack : eventInventory.getContents()) {
            if(itemStack != null && itemStack.hasItemMeta() && !itemStack.getType().equals(Material.BLACK_STAINED_GLASS_PANE)){
                switch(plainName(itemStack.getItemMeta())){
                    case "Shears":
                        shearsPosition = position - 9;
                        break;
                    case "Bow":
                        bowPosition = position - 9;
                        break;
                    case "Ender Pearl":
                        enderPearlPosition = position - 9;
                        break;
                    case "Active Perk 1":
                        activePerk1Position = position - 9;
                        break;
                    case "Active Perk 2":
                        activePerk2Position = position - 9;
                        break;
                }
            }
            position += 1;
        }

        int finalShearsPosition = shearsPosition;
        int finalBowPosition = bowPosition;
        int finalEnderPearlPosition = enderPearlPosition;
        int finalActivePerk1Position = activePerk1Position;
        int finalActivePerk2Position = activePerk2Position;

        //check for mistakes
        ArrayList<Integer> values= new ArrayList<Integer>(){{
            add(finalShearsPosition);
            add(finalBowPosition);
            add(finalEnderPearlPosition);
        }};

        int notDefined = 0;

        for(Integer value: values){
            if(value==0){
                notDefined +=1;
            }
        }

        if(notDefined >= 2){
            new BukkitRunnable(){

                @Override
                public void run() {
                    giveLobbyItems(player);
                }

            }.runTaskLaterAsynchronously(Main.getInstance(), 10);
            player.sendMessage(Component.text("Something went wrong!", NamedTextColor.RED));
            return;
        }

        //put the new configuration in the database
        MongoDatabase db = Main.getMongoDatabase();
        MongoCollection<Document> collection = db.getCollection("playerInventories");

        Document foundDocument = collection.find(eq("_id", player.getUniqueId().toString())).first();
        if(foundDocument == null){
            HashMap<String, Object> playerData = new HashMap<String, Object>(){{
                put("_id", player.getUniqueId().toString());
                put("shears", finalShearsPosition);
                put("bow", finalBowPosition);
                put("ender_pearl", finalEnderPearlPosition);
                put("active_perk1", finalActivePerk1Position);
                put("active_perk2", finalActivePerk2Position);
            }};

            Document document = new Document(playerData);
            collection.insertOne(document);
        }
        else{
            Document query = new Document().append("_id",  player.getUniqueId().toString());

            Bson updates = Updates.combine(
                    Updates.set("shears", finalShearsPosition),
                    Updates.set("bow", finalBowPosition),
                    Updates.set("ender_pearl", finalEnderPearlPosition),
                    Updates.set("active_perk1", finalActivePerk1Position),
                    Updates.set("active_perk2", finalActivePerk2Position)
            );

            collection.updateOne(query, updates);
        }

        player.sendMessage(Component.text("Your new inventory was successfully saved.", NamedTextColor.GREEN));

        new BukkitRunnable(){

            @Override
            public void run() {
                giveLobbyItems(player);
            }

        }.runTaskLaterAsynchronously(Main.getInstance(), 10);
    }

    /**
     * A Method that starts the game, gives every player the right items, changes the scoreboard, resets the cooldown and
     * teleports the players to the right position.
     * @return A boolean whether the game could successfully be started
     * @author SimsumMC
     */
    public static boolean startGame() {
        if(gameStarted){
            return false;
        }

        ActivePerk.loadActivePerkSlots();

        AllPassivePerks.assignPlayersToPerks();

        TeamSystem.teamsOnStart();

        int topVotedLifeAmount = getTopVotedLifeAmount();
        HashMap<String, Integer> teamLives = Cache.getTeamLives();

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        for(Player player: players){
            String team = TeamSystem.getPlayerTeam(player, true);
            teamLives.put(team, topVotedLifeAmount);
            Location location;
            switch (team){
                case "Red":
                    location = MapConfig.redLocation;
                    break;
                case "Green":
                    location = MapConfig.greenLocation;
                    break;
                case "Yellow":
                    location = MapConfig.yellowLocation;
                    break;
                default:
                    location = MapConfig.blueLocation;
                    break;
            }
            setPlayerCooldown(player, 0);
            setGameScoreBoard(player);

            gameStarted = true;

            ItemSystem.giveItems(player);
            player.teleport(location);
            player.setAllowFlight(true);

            setPlayerSpawnProtection(player, Config.spawnProtectionLengthAtGameStart);

            if(player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE){
                player.setGameMode(GameMode.SURVIVAL);
            }

        }

        addDefaultStats();

        Cache.setTeamLives(teamLives);

        return true;
    }

    /**
     * A Method that ends the game, gives every player the lobby items, changes the scoreboard, resets the Cache,
     * teleports the players to the lobby and announces the winner team.
     * @param winnerTeam the winner team as a **COLORED** String
     * @return A boolean whether the game could be successfully ended
     * @author SimsumMC
     */
    public static boolean endGame(String winnerTeam) {
        runScoreBoardTask = false;

        if(!gameStarted){
            return false;
        }

        gameStarted = false;

        cooldown = 60;

        StatsSystem.saveAllPlayerStats(winnerTeam);

        Cache.clear();

        Bukkit.broadcast(Component.text("The team ", NamedTextColor.GRAY)
                .append(Component.text(winnerTeam, NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(" won!", NamedTextColor.GRAY)));

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        for(Player player: players){
            setLobbyScoreBoard(player);
            giveLobbyItems(player);
            player.teleport(MapConfig.lobbyLocation);
            player.setAllowFlight(true);
            if(player.getGameMode() == GameMode.SPECTATOR || player.getGameMode() == GameMode.CREATIVE){
                player.setGameMode(GameMode.SURVIVAL);
            }
            player.showTitle(net.kyori.adventure.title.Title.title(
                    Component.text(winnerTeam + " won!", NamedTextColor.GRAY),
                    Component.empty(),
                    net.kyori.adventure.title.Title.Times.times(
                            java.time.Duration.ofMillis(500),
                            java.time.Duration.ofMillis(3500),
                            java.time.Duration.ofMillis(1000))));
        }

        updateScoreBoard();
        BlockBreakingSystem.clearPlacedBlocks();
        BlockBreakingSystem.resetMap();
        return true;
    }

    /**
     * A Method that trys to find a winner team and ends the game if a team is found.
     * @author SimsumMC
     */
    public static void determinateWinnerTeam() {
        //check for teams -> if only one is left over
        HashMap<String, ArrayList<Player>> teamMembers = Cache.getTeamMembers();

        int emptyTeams = 0;
        String existingTeam = null;

        for(String key : teamMembers.keySet()){
            ArrayList<Player> players = teamMembers.get(key);
            if(players.size() == 0){
                emptyTeams += 1;
            }
            else{
                existingTeam = key;
            }

        }
        if(emptyTeams >= 3){
            if(existingTeam != null){
                endGame(existingTeam);
            }
            else{
                endGame("Unknown");
            }
            return;
        }

        //check for lives
        HashMap<String, Integer> teamLives = Cache.getTeamLives();

        int deathTeams = 0;
        existingTeam = null;

        for(String key : teamLives.keySet()){
            int lives = teamLives.get(key);
            if(lives == 0){
                deathTeams += 1;
            }
            else{
                existingTeam = key;
            }

        }
        if(deathTeams >= 3){
            if(existingTeam != null){
                endGame(existingTeam);
            }
            else{
                endGame("Unknown");
            }
        }
    }


    /**
     * A Method that saves the Perk that was selected by a player.
     * @param player the Player that selected the perk
     * @param perkName the Name of the Perk without colour code
     * @param perkType the Type of the perk as an enum
     * @author SimsumMC
     */
    public void savePerkSelection(Player player, String perkName, PerkType perkType){
        String perkTypeString = perkType.toString().toLowerCase();

        MongoDatabase db = Main.getMongoDatabase();
        MongoCollection<Document> collection = db.getCollection("playerPerks");

        Document foundDocument = collection.find(eq("_id", player.getUniqueId().toString())).first();
        if(foundDocument == null){

            HashMap<String, Object> playerData = new HashMap<String, Object>(){{
                put("_id", player.getUniqueId().toString());
                put("first_active", null);
                put("second_active", null);
                put("passive", null);
            }};

            playerData.put(perkTypeString, perkName);

            Document document = new Document(playerData);
            collection.insertOne(document);
        }
        else{
            if(perkType == PerkType.FIRST_ACTIVE){
                Object get = foundDocument.get(PerkType.SECOND_ACTIVE.toString().toLowerCase());
                if(get != null && get.equals(perkName)){
                    player.sendMessage(Component.text("You can't have the same active perks!", NamedTextColor.RED));
                    return;
                }
            }
            else if (perkType == PerkType.SECOND_ACTIVE){
                Object get = foundDocument.get(PerkType.FIRST_ACTIVE.toString().toLowerCase());
                if(get != null && get.equals(perkName)){
                    player.sendMessage(Component.text("You can't have the same active perks!", NamedTextColor.RED));
                    return;
                }
            }

            Document query = new Document().append("_id",  player.getUniqueId().toString());

            Bson updates = Updates.set(perkTypeString, perkName);

            collection.updateOne(query, updates);
        }
    }


    /**
     * A Method that gives the lobby items to the given player.
     * @param player the player that becomes the items in the inventory
     * @author SimsumMC
     */
    public static void giveLobbyItems(Player player) {

        PlayerInventory inv = player.getInventory();

        // clear players inventory
        inv.clear();

        inv.setBoots(null);
        inv.setLeggings(null);
        inv.setChestplate(null);
        inv.setHelmet(null);

        // Team Selecting Item
        ItemStack teamStack = new ItemStack(Material.RED_BED);
        ItemMeta teamMeta = teamStack.getItemMeta();
        teamMeta.displayName(Component.text("Team Selecting", NamedTextColor.YELLOW, TextDecoration.BOLD));
        teamStack.setItemMeta(teamMeta);
        inv.setItem(0, teamStack);

        // Achievement Item
        ItemStack achievementStack = new ItemStack(Material.DIAMOND);
        ItemMeta achievementMeta = achievementStack.getItemMeta();
        achievementMeta.displayName(Component.text("Achievements", NamedTextColor.GOLD, TextDecoration.BOLD));
        achievementStack.setItemMeta(achievementMeta);
        inv.setItem(1, achievementStack);

        // Vote Life Count Item
        ItemStack livesStack = new ItemStack(Material.FEATHER);
        ItemMeta livesMeta = livesStack.getItemMeta();
        livesMeta.displayName(Component.text("Amount of Lives", NamedTextColor.GREEN, TextDecoration.BOLD));
        livesStack.setItemMeta(livesMeta);
        inv.setItem(2, livesStack);

        // Edit Inventory Item
        ItemStack inventoryStack = new ItemStack(Material.CHEST);
        ItemMeta inventoryMeta = inventoryStack.getItemMeta();
        inventoryMeta.displayName(Component.text("Edit Inventory", NamedTextColor.AQUA, TextDecoration.BOLD));
        inventoryStack.setItemMeta(inventoryMeta);
        inv.setItem(4, inventoryStack);

        // Choose Perks Item TODO: add interaction -> LATER
        ItemStack perksStack = new ItemStack(Material.ENDER_CHEST);
        ItemMeta perksMeta = perksStack.getItemMeta();
        perksMeta.displayName(Component.text("Perks", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        perksStack.setItemMeta(perksMeta);
        inv.setItem(6, perksStack);

        // Leave Server Item
        ItemStack leaveStack = new ItemStack(Material.SLIME_BALL);
        ItemMeta leaveMeta = leaveStack.getItemMeta();
        leaveMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        leaveMeta.displayName(Component.text("Leave", NamedTextColor.RED, TextDecoration.BOLD));
        leaveMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        leaveStack.setItemMeta(leaveMeta);
        inv.setItem(8, leaveStack);

    }

    /**
     * A Method that shows / updates the inventar to vote for the life count.
     * @param player the player that gets the life amount voting inventory opened
     * @author SimsumMC
     */
    private static void showLifeAmountVoting(Player player) {
        Inventory inv = Bukkit.createInventory(null, 3*9, Component.text("Amount of Lives Voting", NamedTextColor.GREEN));

        // Glass Background
        ItemStack glassStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassStack.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glassStack.setItemMeta(glassMeta);

        for (int i = 0; i<= 26; i++) {
            inv.setItem(i, glassStack);
        }

        // 5 Votes Item
        int fiveVoteCount = Cache.getLifeVoting().get(5).size();
        ItemStack fiveLivesStack = new ItemStack(Material.LIME_DYE, 5);
        ItemMeta fiveLivesMeta = fiveLivesStack.getItemMeta();
        fiveLivesMeta.displayName(Component.text("5 Lives", NamedTextColor.GREEN));
        fiveLivesMeta.lore(List.of(Component.text("»Votes: ", NamedTextColor.GRAY).append(Component.text(fiveVoteCount, NamedTextColor.GREEN))));
        fiveLivesStack.setItemMeta(fiveLivesMeta);
        inv.setItem(11, fiveLivesStack);

        // 10 Votes Item
        int tenVoteCount = Cache.getLifeVoting().get(10).size();
        ItemStack tenLivesStack = new ItemStack(Material.LIME_DYE, 10);
        ItemMeta tenLivesMeta = tenLivesStack.getItemMeta();
        tenLivesMeta.displayName(Component.text("10 Lives", NamedTextColor.GREEN));
        tenLivesMeta.lore(List.of(Component.text("»Votes: ", NamedTextColor.GRAY).append(Component.text(tenVoteCount, NamedTextColor.GREEN))));
        tenLivesStack.setItemMeta(tenLivesMeta);
        inv.setItem(13, tenLivesStack);

        // 15 Votes Item
        int fifteenVoteCount = Cache.getLifeVoting().get(15).size();
        ItemStack fifteenLivesStack = new ItemStack(Material.LIME_DYE, 15);
        ItemMeta fifteenLivesMeta = fifteenLivesStack.getItemMeta();
        fifteenLivesMeta.displayName(Component.text("15 Lives", NamedTextColor.GREEN));
        fifteenLivesMeta.lore(List.of(Component.text("»Votes: ", NamedTextColor.GRAY).append(Component.text(fifteenVoteCount, NamedTextColor.GREEN))));
        fifteenLivesStack.setItemMeta(fifteenLivesMeta);
        inv.setItem(15, fifteenLivesStack);

        player.openInventory(inv);
    }

    /**
     * A Method that shows / updates the inventar to change the inventory sort.
     * @param player - the Player that gets the inventory opened
     * @author SimsumMC
     */
    private static void showEditInventoryMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 3*9, Component.text("Edit Inventory", NamedTextColor.AQUA));

        // Glass Background
        ItemStack glassStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassStack.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glassStack.setItemMeta(glassMeta);

        for (int i = 0; i<= 26; i++) {
            if(i >= 9 && i <=17){
                continue;
            }
            inv.setItem(i, glassStack);
        }

        int shearsSlot;
        int bowSlot;
        int enderPearlSlot;
        int activePerk1Slot;
        int activePerk2Slot;

        MongoDatabase db = Main.getMongoDatabase();
        MongoCollection<Document> collection = db.getCollection("playerInventories");

        Document foundDocument = collection.find(eq("_id", player.getUniqueId().toString())).first();
        if(foundDocument == null){
            shearsSlot = 9;
            bowSlot = 10;
            enderPearlSlot = 17;
            activePerk1Slot = 16;
            activePerk2Slot = 11;
        }
        else{
            shearsSlot = (int) foundDocument.get("shears") + 9;
            bowSlot = (int) foundDocument.get("bow") + 9;
            enderPearlSlot = (int) foundDocument.get("ender_pearl") + 9;
            activePerk1Slot = (int) foundDocument.get("active_perk1") + 9;
            activePerk2Slot = (int) foundDocument.get("active_perk2") + 9;

        }

        // shears
        ItemStack shearsStack = new ItemStack(Material.SHEARS);
        ItemMeta shearsMeta = shearsStack.getItemMeta();
        shearsMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        shearsMeta.displayName(Component.text("Shears", NamedTextColor.AQUA));
        shearsStack.setItemMeta(shearsMeta);
        inv.setItem(shearsSlot, shearsStack);

        // bow
        ItemStack bowStack = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bowStack.getItemMeta();
        bowMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
        bowMeta.displayName(Component.text("Bow", NamedTextColor.AQUA));
        bowStack.setItemMeta(bowMeta);
        inv.setItem(bowSlot, bowStack);

        // ender pearl
        ItemStack enderPearlStack = new ItemStack(Material.ENDER_PEARL);
        ItemMeta enderPearlMeta = enderPearlStack.getItemMeta();
        enderPearlMeta.displayName(Component.text("Ender Pearl", NamedTextColor.AQUA));
        enderPearlStack.setItemMeta(enderPearlMeta);
        inv.setItem(enderPearlSlot, enderPearlStack);

        // active perk 1
        ItemStack activePerk1Stack = new ItemStack(Material.BARRIER);
        ItemMeta activePerk1Meta = activePerk1Stack.getItemMeta();
        activePerk1Meta.displayName(Component.text("Active Perk 1", NamedTextColor.AQUA));
        activePerk1Stack.setItemMeta(activePerk1Meta);
        inv.setItem(activePerk1Slot, activePerk1Stack);

        // active perk 1
        ItemStack activePerk2Stack = new ItemStack(Material.BARRIER);
        ItemMeta activePerk2Meta = activePerk2Stack.getItemMeta();
        activePerk2Meta.displayName(Component.text("Active Perk 2", NamedTextColor.AQUA));
        activePerk2Stack.setItemMeta(activePerk2Meta);
        inv.setItem(activePerk2Slot, activePerk2Stack);

        player.openInventory(inv);
    }

    /**
     * A Method that shows the inventar to choose between the different perk types to change them.
     * @param player - The player gets an inventory opened to choose the perks.
     * @author SimsumMC
     */
    private static void showPerkMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 3*9, Component.text("Choose Perks", NamedTextColor.LIGHT_PURPLE));

        // Glass Background
        ItemStack glassStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassStack.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glassStack.setItemMeta(glassMeta);

        for (int i = 0; i<= 26; i++) {
            inv.setItem(i, glassStack);
        }

        // Active Perk #1
        ItemStack activeOneStack = new ItemStack(Material.CHEST);
        ItemMeta activeOneMeta = activeOneStack.getItemMeta();
        activeOneMeta.displayName(Component.text("Active Perk #1", NamedTextColor.LIGHT_PURPLE));
        activeOneStack.setItemMeta(activeOneMeta);
        inv.setItem(11, activeOneStack);

        // Active Perk #2
        ItemStack activeTwoStack = new ItemStack(Material.CHEST);
        ItemMeta activeTwoMeta = activeTwoStack.getItemMeta();
        activeTwoMeta.displayName(Component.text("Active Perk #2", NamedTextColor.LIGHT_PURPLE));
        activeTwoStack.setItemMeta(activeTwoMeta);
        inv.setItem(13, activeTwoStack);

        // Passive Perk
        ItemStack passiveStack = new ItemStack(Material.ENDER_CHEST);
        ItemMeta passiveMeta = passiveStack.getItemMeta();
        passiveMeta.displayName(Component.text("Passive Perk", NamedTextColor.LIGHT_PURPLE));
        passiveStack.setItemMeta(passiveMeta);
        inv.setItem(15, passiveStack);

        player.openInventory(inv);
    }

    /**
     * A Method that shows the inventar to choose between the different perk types to change them.
     * @author SimsumMC
     */
    private static void showActivePerkMenu(Player player, PerkType perkType) {

        String perkTypeString = perkType.toString().toLowerCase();
        String selectedPerk = null;

        MongoDatabase db = Main.getMongoDatabase();
        MongoCollection<Document> collection = db.getCollection("playerPerks");

        Document foundDocument = collection.find(eq("_id", player.getUniqueId().toString())).first();
        if(foundDocument != null){
            if(foundDocument.get(perkTypeString) != null){
                selectedPerk = (String) foundDocument.get(perkTypeString);
            }
        }

        Inventory inv = Bukkit.createInventory(null, 3*9, Component.text("Active Perk #" + perkType.value, NamedTextColor.LIGHT_PURPLE));

        List<Component> newLore;

        HashMap<String, ActivePerk> activePerks = Cache.getActivePerks();
        for(ActivePerk perk : activePerks.values()){
            if(!perk.getSelectableStatus()){
                continue;
            }

            ItemStack itemStack = perk.getItemStack().clone();
            ItemMeta itemMeta = itemStack.getItemMeta();

            if(selectedPerk != null && plainName(itemMeta).equals(selectedPerk)){
                itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            else{
                if (itemMeta.hasEnchants()){
                    for(Enchantment enchantment : itemMeta.getEnchants().keySet()){
                        itemMeta.removeEnchant(enchantment);
                    }
                }
            }


            newLore = new ArrayList<>();

            newLore.add(Component.text(perk.getDescription(), NamedTextColor.WHITE));
            newLore.add(Component.text("\u1CBC"));
            newLore.add(Component.text("WoolCost: ", NamedTextColor.GOLD).append(Component.text(perk.getWoolCost(), NamedTextColor.DARK_PURPLE)));
            newLore.add(Component.text("Cooldown: ", NamedTextColor.GOLD).append(Component.text(perk.getCooldown(), NamedTextColor.DARK_PURPLE)));

            itemMeta.lore(newLore);

            itemStack.setItemMeta(itemMeta);

            inv.addItem(itemStack);
        }

        // Back Item
        ItemStack backStack = new ItemStack(Material.OAK_DOOR);
        ItemMeta backMeta = backStack.getItemMeta();
        backMeta.displayName(Component.text("Go Back", NamedTextColor.RED));
        backStack.setItemMeta(backMeta);
        inv.setItem(26, backStack);

        player.openInventory(inv);
    }

    private static void showPassivePerkMenu(Player player) {

        String selectedPerk = null;

        MongoDatabase db = Main.getMongoDatabase();
        MongoCollection<Document> collection = db.getCollection("playerPerks");

        Document foundDocument = collection.find(eq("_id", player.getUniqueId().toString())).first();
        if(foundDocument != null){
            selectedPerk = (String) foundDocument.get("passive");
        }

        Inventory inv = Bukkit.createInventory(null, 3*9, Component.text("Passive Perk", NamedTextColor.LIGHT_PURPLE));

        List<Component> newLore;

        HashMap<String, PassivePerk<? extends Event, ?>> passivePerks = Cache.getPassivePerks();
        for(PassivePerk<? extends Event, ?> perk : passivePerks.values()){

            ItemStack itemStack = perk.getItem().clone();
            ItemMeta itemMeta = itemStack.getItemMeta();

            if(selectedPerk != null && plainName(itemMeta).equals(selectedPerk)){
                itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            else{
                if (itemMeta.hasEnchants()){
                    for(Enchantment enchantment : itemMeta.getEnchants().keySet()){
                        itemMeta.removeEnchant(enchantment);
                    }
                }
            }


            newLore = new ArrayList<>();

            newLore.add(Component.text(perk.getDescription(), NamedTextColor.WHITE));

            itemMeta.lore(newLore);

            itemStack.setItemMeta(itemMeta);

            inv.addItem(itemStack);
        }

        // Back Item
        ItemStack backStack = new ItemStack(Material.OAK_DOOR);
        ItemMeta backMeta = backStack.getItemMeta();
        backMeta.displayName(Component.text("Go Back", NamedTextColor.RED));
        backStack.setItemMeta(backMeta);
        inv.setItem(26, backStack);

        player.openInventory(inv);
    }



    /**
     * A Method that updates the player cooldown (the number in the XP bar) every 20 ticks depended on the player
     * amount.
     * @author SimsumMC
     */
    public static void updatePlayerCooldown() {
        runCooldownTask = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(runScoreBoardTask){
                    if(!gameStarted) {
                        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                        int playerAmount = players.size();
                        if (playerAmount >= (Config.teamSize * 2)) {
                            if (cooldown == 0) {
                                startGame();
                            } else if (cooldown > Config.skipCooldown) {
                                cooldown = Config.skipCooldown;
                            }
                            cooldown -= 1;
                        } else {
                            cooldown = 60;
                        }
                        for (Player player : players) {

                            if (player.getGameMode() == GameMode.SURVIVAL) {
                                setPlayerCooldown(player, cooldown);
                            }

                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);

    }

    /**
     * A Method that sets the cooldown for the given player.
     * @param player the player that gets the cooldown modified
     * @author SimsumMC
     */
    public static void setPlayerCooldown(Player player, int level) {
        float exp = (float) level/60;
        player.setExp(exp);
        player.setLevel(level);

    }

    /**
     * A Method that returns the current top-voted amount of lives.
     * @return The top voted life amount as an Integer
     * @author SimsumMC
     */
    public static int getTopVotedLifeAmount(){
        int topKey = Config.defaultLives;
        int topVoters = 0;
        HashMap<Integer, ArrayList<Player>> data = Cache.getLifeVoting();
        for(Integer key: data.keySet()){
            ArrayList<Player> players = data.get(key);
            if(players.size() > topVoters){
                topVoters = players.size();
                topKey = key;
            }
        }
        return topKey;
    }

    /**
     * A Method that sets a player to a spectator -> game mode & position changes
     * @param player the player that gets into spectator mode
     * @author SimsumMC
     */
    public static void setPlayerSpectator(Player player) {
        player.teleport(MapConfig.midLocation);
        player.setGameMode(GameMode.SPECTATOR);
    }

    /**
     * A Method that updates the scoreboard for every player, depending on the game status.
     * @author SimsumMC
     */
    public static void updateScoreBoard() {
        runScoreBoardTask = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();

                for(Player player : players) {
                    if (!gameStarted) {
                        updateLobbyScoreBoard(player);
                    } else {
                        updateGameScoreBoard(player);
                    }
                }            }
        }.runTaskTimer(Main.getInstance(), 0, 20);

    }

    /**
     * A Method that changes the scoreboard for the given player to the lobby scoreboard.
     * @param player the player that gets the scoreboard modified
     * @author SimsumMC
     */
    public static void setLobbyScoreBoard(Player player) {

        int maxPlayers = Bukkit.getServer().getMaxPlayers();
        int actualPlayers = Bukkit.getServer().getOnlinePlayers().size();

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective obj = board.registerNewObjective("Lobby", "dummy", "§a§lWoolbattle");

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team team = board.registerNewTeam("team");
        Team map = board.registerNewTeam("map");
        Team lives = board.registerNewTeam("lives");
        Team players = board.registerNewTeam("players");

        obj.getScore("\u1CBC\u1CBC\u1CBC\u1CBC").setScore(11);

        obj.getScore("§7»Team").setScore(10);
        obj.getScore("§c").setScore(9);

        obj.getScore("\u1CBC\u1CBC\u1CBC").setScore(8);

        obj.getScore("§7»Map").setScore(7);
        obj.getScore("§d").setScore(6);

        obj.getScore("\u1CBC\u1CBC").setScore(5);

        obj.getScore("§7»Amount of Lives").setScore(4);
        obj.getScore("§e").setScore(3);

        obj.getScore("\u1CBC").setScore(2);

        obj.getScore("§7»Players").setScore(1);
        obj.getScore("§b").setScore(0);

        team.addEntry("§c");
        team.setPrefix(TeamSystem.getPlayerTeam(player, false));

        map.addEntry("§d");
        map.setPrefix("§d" + MapConfig.mapName);

        lives.addEntry("§e");
        lives.setPrefix("§e" + Config.defaultLives);

        players.addEntry("§b");
        players.setPrefix("§b" + actualPlayers + "/" + maxPlayers);

        player.setScoreboard(board);

    }

    /**
     * A Method that updates the scoreboard for the given player with the current values.
     * @param player the player that gets the scoreboard modified
     * @author SimsumMC
     */
    public static void updateLobbyScoreBoard(Player player) {
        if (!runScoreBoardTask){return;}

        int maxPlayers = Bukkit.getServer().getMaxPlayers();
        int actualPlayers = Bukkit.getServer().getOnlinePlayers().size();

        Scoreboard board = player.getScoreboard();

        Team team = board.getTeam("team");
        Team map = board.getTeam("map");
        Team lives = board.getTeam("lives");
        Team players = board.getTeam("players");

        team.setPrefix(TeamSystem.getPlayerTeam(player, false));
        map.setPrefix("§d" + MapConfig.mapName);
        lives.setPrefix("§e" + getTopVotedLifeAmount());
        players.setPrefix("§b" + actualPlayers + "/" + maxPlayers);

    }

    /**
     * A Method that changes the scoreboard for the given player to the game scoreboard.
     * @param player the player that gets the scoreboard modified
     * @author SimsumMC
     */
    public static void setGameScoreBoard(Player player) {

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective obj = board.registerNewObjective("Game", "dummy", "§a§lWoolbattle");

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team team = board.registerNewTeam("team");
        Team map = board.registerNewTeam("map");

        Team redTeam = board.registerNewTeam("redTeam");
        Team blueTeam = board.registerNewTeam("blueTeam");
        Team greenTeam = board.registerNewTeam("greenTeam");
        Team yellowTeam = board.registerNewTeam("yellowTeam");

        obj.getScore("\u1CBC\u1CBC\u1CBC").setScore(11);

        obj.getScore("§7»Team").setScore(10);
        obj.getScore("§c").setScore(9);

        obj.getScore("\u1CBC\u1CBC").setScore(8);

        obj.getScore("§7»Map").setScore(7);
        obj.getScore("§d").setScore(6);

        obj.getScore("\u1CBC").setScore(5);

        obj.getScore("§7»Lives").setScore(4);

        obj.getScore("§4").setScore(3);
        obj.getScore("§9").setScore(2);
        obj.getScore("§2").setScore(1);
        obj.getScore("§e").setScore(0);

        team.addEntry("§c");
        team.setPrefix(TeamSystem.getPlayerTeam(player,false));

        map.addEntry("§d");
        map.setPrefix("§d" + MapConfig.mapName);

        HashMap<String, Integer> teamLives = Cache.getTeamLives();
        int redLives = teamLives.get("Red");
        int blueLives = teamLives.get("Blue");
        int greenLives = teamLives.get("Green");
        int yellowLives = teamLives.get("Yellow");

        redTeam.addEntry("§4");
        redTeam.setPrefix("§4❤ " + redLives);

        blueTeam.addEntry("§9");
        blueTeam.setPrefix("§9❤ " + blueLives);

        greenTeam.addEntry("§2");
        greenTeam.setPrefix("§2❤ " + greenLives);

        yellowTeam.addEntry("§e");
        yellowTeam.setPrefix("§e❤ " + yellowLives);

        player.setScoreboard(board);

    }

    /**
     * A Method that updates the scoreboard for the given player with the current values.
     * @param player the player that gets the scoreboard modified
     * @author SimsumMC
     */
    public static void updateGameScoreBoard(Player player) {
        if (!runScoreBoardTask){return;}

        Scoreboard board = player.getScoreboard();

        Team team = board.getTeam("team");
        Team map = board.getTeam("map");

        Team redTeam = board.getTeam("redTeam");
        Team blueTeam = board.getTeam("blueTeam");
        Team greenTeam = board.getTeam("greenTeam");
        Team yellowTeam = board.getTeam("yellowTeam");

        team.setPrefix(TeamSystem.getPlayerTeam(player,false));
        map.setPrefix("§d" + MapConfig.mapName);

        HashMap<String, Integer> teamLives = Cache.getTeamLives();
        int redLives = teamLives.get("Red");
        int blueLives = teamLives.get("Blue");
        int greenLives = teamLives.get("Green");
        int yellowLives = teamLives.get("Yellow");

        redTeam.setPrefix("§4❤ " + redLives);
        blueTeam.setPrefix("§9❤ " + blueLives);
        greenTeam.setPrefix("§2❤ " + greenLives);
        yellowTeam.setPrefix("§e❤ " + yellowLives);
    }
}