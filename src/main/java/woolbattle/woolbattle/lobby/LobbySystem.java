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

import org.bson.Document;
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
import org.bukkit.event.block.Action;
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
import woolbattle.woolbattle.PlayerDataCache;
import woolbattle.woolbattle.achievements.AchievementUI;
import woolbattle.woolbattle.itemsystem.ItemSystem;
import woolbattle.woolbattle.perks.ActivePerk;
import woolbattle.woolbattle.perks.AllActivePerks;
import woolbattle.woolbattle.perks.AllPassivePerks;
import woolbattle.woolbattle.perks.PassivePerk;
import woolbattle.woolbattle.stats.StatsSystem;
import woolbattle.woolbattle.team.TeamSystem;
import woolbattle.woolbattle.woolsystem.BlockBreakingSystem;

import java.util.*;

import static woolbattle.woolbattle.lives.LivesSystem.setPlayerSpawnProtection;
import static woolbattle.woolbattle.stats.StatsSystem.addDefaultStats;

public class LobbySystem implements Listener {

    public static boolean gameStarted = false;
    public static boolean runCooldownTask = false;
    public static boolean runScoreBoardTask = false;
    private static final String SCOREBOARD_TITLE_TEXT = "WOOLBATTLE";
    private static final String[] SCOREBOARD_TITLE_COLORS = new String[]{"§c", "§6", "§e", "§a", "§b", "§9", "§d"};
    private static int scoreboardTitleOffset = 0;
    private static final int PERK_LORE_WRAP_LENGTH = 32;
    private static final String PASSIVE_PERK_MENU_TITLE = "Passive Perk";
    private static final String ULTIMATE_MENU_TITLE = "Ultimate";
    private static final String PASSIVE_CATEGORY_ECONOMY = "Economy";
    private static final String PASSIVE_CATEGORY_DEFENSE = "Defense";
    private static final String PASSIVE_CATEGORY_UTILITY = "Utility";
        private static final Map<String, String> PERK_NAME_TRANSLATIONS = Map.ofEntries(
            Map.entry("Sparfuchs", "Bargain Hunter"),
            Map.entry("Ankerstiefel", "Anchor Boots"),
            Map.entry("Heimvorteil", "Homefield"),
            Map.entry("Nachschub", "Resupply"),
            Map.entry("Baumeister", "Builder"),
            Map.entry("Rettungsinstinkt", "Survival Instinct"),
            Map.entry("Standhaft", "Steadfast"),
            Map.entry("Rueckprall", "Rebound"),
            Map.entry("Zeitanker", "Time Anchor"),
            Map.entry("Gravitationskern", "Gravity Core"),
            Map.entry("Perk-Hijack", "Perk Hijack"),
            Map.entry("Kettenmarkierung", "Chain Mark")
        );
        private static final Map<String, String> PERK_DESCRIPTION_TRANSLATIONS = Map.ofEntries(
            Map.entry("Beim Abbau von Wolle erhaeltst du zusaetzliche Team-Wolle", "When mining wool, you gain extra team wool."),
            Map.entry("20% Chance: Aktive Perks kosten keine Wolle", "20% chance: active perks cost no wool."),
            Map.entry("12% weniger eingehender Knockback", "12% less incoming knockback."),
            Map.entry("Auf eigener Team-Wolle bekommst du 10% Laufgeschwindigkeit", "Gain 10% movement speed while standing on your own team wool."),
            Map.entry("Alle 5 Sekunden +1 Team-Wolle, wenn dein Inventar Platz hat", "Every 5 seconds: +1 team wool if your inventory has space."),
            Map.entry("Jede 6. platzierte Wolle wird direkt erstattet", "Every 6th placed wool is refunded instantly."),
            Map.entry("Einmal pro Leben rettet dich ein Void-Fall zur letzten sicheren Position (1 Herz Schaden)", "Once per life, a void fall teleports you to your last safe position (1 heart damage)."),
            Map.entry("5% Chance, eingehenden Knockback komplett zu negieren", "5% chance to completely negate incoming knockback."),
            Map.entry("5% Chance, Knockback abzuwehren und auf den Angreifer zurueckzugeben", "5% chance to block knockback and reflect it to the attacker."),
            Map.entry("Nach kurzer Zeit springst du zurueck und loest eine Impulswelle aus.", "After a short delay, you jump back and release an impulse wave."),
            Map.entry("Zieht Gegner an und schleudert sie danach auseinander.", "Pulls enemies in, then throws them apart."),
            Map.entry("Sperrt einen gegnerischen aktiven Perk fuer kurze Zeit.", "Disables one enemy active perk for a short time."),
            Map.entry("Stoesst nahe Gegner zurueck.", "Knocks back nearby enemies."),
            Map.entry("Legt eine unsichtbare Falle fuer Gegner.", "Places an invisible trap for enemies."),
            Map.entry("Erster Klick speichert Position, zweiter teleportiert zurueck.", "First click stores your position, second click teleports you back."),
            Map.entry("Erzeugt Nebel und blendet Gegner im Bereich.", "Creates fog and blinds enemies in the area."),
            Map.entry("Blockiert aktive Perks von Gegnern fuer 4 Sekunden.", "Blocks enemy active perks for 4 seconds.")
        );

    private static String plainName(ItemMeta meta) {
        Component display = meta.displayName();
        return display != null ? PlainTextComponentSerializer.plainText().serialize(display) : "";
    }

    private static List<String> wrapLoreText(String text, int maxLineLength) {
        ArrayList<String> lines = new ArrayList<>();
        if(text == null || text.isBlank()) {
            return lines;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for(String word : words) {
            if(currentLine.length() == 0) {
                currentLine.append(word);
                continue;
            }

            if(currentLine.length() + 1 + word.length() <= maxLineLength) {
                currentLine.append(" ").append(word);
            }
            else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if(currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    public static List<Component> buildPerkDescriptionLore(String description) {
        ArrayList<Component> lore = new ArrayList<>();
        for(String line : wrapLoreText(translatePerkDescription(description), PERK_LORE_WRAP_LENGTH)) {
            lore.add(Component.text(line, NamedTextColor.WHITE));
        }
        return lore;
    }

    public static String translatePerkName(String internalName) {
        return PERK_NAME_TRANSLATIONS.getOrDefault(internalName, internalName);
    }

    public static String toInternalPerkName(String displayName) {
        for(Map.Entry<String, String> entry : PERK_NAME_TRANSLATIONS.entrySet()) {
            if(entry.getValue().equals(displayName)) {
                return entry.getKey();
            }
        }
        return displayName;
    }

    private static String translatePerkDescription(String description) {
        return PERK_DESCRIPTION_TRANSLATIONS.getOrDefault(description, description);
    }

    public static LinkedHashMap<String, List<String>> getPassivePerkCategories() {
        LinkedHashMap<String, List<String>> categories = new LinkedHashMap<>();
        categories.put(PASSIVE_CATEGORY_ECONOMY, Arrays.asList("Wool Duplication", "Sparfuchs", "Nachschub", "Baumeister"));
        categories.put(PASSIVE_CATEGORY_DEFENSE, Arrays.asList("Ankerstiefel", "Standhaft", "Rueckprall", "Rettungsinstinkt"));
        categories.put(PASSIVE_CATEGORY_UTILITY, Arrays.asList("Heimvorteil", "Wool Archer"));
        return categories;
    }

    private static String getPassiveCategoryInventoryTitle(String categoryName) {
        return PASSIVE_PERK_MENU_TITLE + " - " + categoryName;
    }

    private static boolean isPerkSelectedInCategory(String selectedPerk, String categoryName) {
        if(selectedPerk == null) {
            return false;
        }

        List<String> perks = getPassivePerkCategories().get(categoryName);
        return perks != null && perks.contains(selectedPerk);
    }


    public static String getSelectedPassivePerk(Player player) {
        Document foundDocument = PlayerDataCache.getPlayerPerks(player);
        if(foundDocument == null) {
            return null;
        }

        Object passive = foundDocument.get("passive");
        return passive instanceof String ? (String) passive : null;
    }

    public static String getSelectedUltimate(Player player) {
        Document foundDocument = PlayerDataCache.getPlayerPerks(player);
        if (foundDocument != null && foundDocument.get("ultimate") instanceof String) {
            String selected = (String) foundDocument.get("ultimate");
            if (AllActivePerks.isUltimateName(selected)) {
                return selected;
            }
        }

        return AllActivePerks.getDefaultUltimateName();
    }

    private static ItemStack createGlassPane() {
        ItemStack glassStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassStack.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glassStack.setItemMeta(glassMeta);
        return glassStack;
    }

    private static void fillWithGlass(Inventory inventory) {
        ItemStack glass = createGlassPane();
        for(int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }
    }

    private static ItemStack createPassiveCategoryItem(Material material, String title, NamedTextColor color, String subtitle, boolean selected) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.displayName(Component.text(title, color, TextDecoration.BOLD));
        ArrayList<Component> lore = new ArrayList<>();
        lore.add(Component.text(subtitle, NamedTextColor.GRAY));
        lore.add(Component.text(" "));
        lore.add(Component.text("Click to open", NamedTextColor.DARK_GRAY));
        itemMeta.lore(lore);

        if(selected) {
            itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private static ItemStack createLobbyHotbarItem(Material material, String title, NamedTextColor color, List<Component> lore, boolean glowing) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.displayName(Component.text(title, color, TextDecoration.BOLD));
        itemMeta.lore(lore);
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        if(glowing) {
            itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        itemStack.setItemMeta(itemMeta);
        return itemStack;
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
        PlayerDataCache.invalidatePlayer(player);

        // Clear advancements and recipe book
        java.util.Iterator<org.bukkit.advancement.Advancement> advIter = Bukkit.advancementIterator();
        while (advIter.hasNext()) {
            org.bukkit.advancement.AdvancementProgress progress = player.getAdvancementProgress(advIter.next());
            for (String criteria : progress.getAwardedCriteria()) {
                progress.revokeCriteria(criteria);
            }
        }
        java.util.List<org.bukkit.NamespacedKey> recipeKeys = new java.util.ArrayList<>();
        Bukkit.recipeIterator().forEachRemaining(recipe -> {
            if (recipe instanceof org.bukkit.Keyed keyed) {
                recipeKeys.add(keyed.getKey());
            }
        });
        player.undiscoverRecipes(recipeKeys);

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
        PlayerDataCache.invalidatePlayer(player);

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

        rawItemName = toInternalPerkName(rawItemName);

        String passiveCategoryPrefix = PASSIVE_PERK_MENU_TITLE + " - ";
        if(rawInventoryName.startsWith(passiveCategoryPrefix)) {
            String categoryName = rawInventoryName.substring(passiveCategoryPrefix.length());

            if(rawItemName.equals("Go Back")) {
                showPassivePerkMenu(player);
                return;
            }

            if(!Cache.getPassivePerks().containsKey(rawItemName)) {
                return;
            }

            savePerkSelection(player, rawItemName, PerkType.PASSIVE);
            showPassivePerkCategoryMenu(player, categoryName);
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
                    case ULTIMATE_MENU_TITLE:
                        showUltimatePerkMenu(player);
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
                    if(getPassivePerkCategories().containsKey(rawItemName)) {
                        showPassivePerkCategoryMenu(player, rawItemName);
                    }
                }
                break;
            case ULTIMATE_MENU_TITLE:
                if(rawItemName.equals("Go Back")){
                    showPerkMenu(player);
                }
                else if(AllActivePerks.isUltimateName(rawItemName)){
                    savePerkSelection(player, rawItemName, PerkType.ULTIMATE);
                    showUltimatePerkMenu(player);
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
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null
                && event.getClickedBlock().getState() instanceof org.bukkit.block.Sign) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }

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
                new AnimatedPerkGUI(player).open();
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

            }.runTaskLater(Main.getInstance(), 10);
            player.sendMessage(Component.text("Something went wrong!", NamedTextColor.RED));
            return;
        }

        //put the new configuration in the database
        Document foundDocument = PlayerDataCache.getPlayerInventories(player);
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
            Main.getStore().insert("playerInventories", document);
            PlayerDataCache.putPlayerInventories(player, document);
        }
        else{
            java.util.Map<String, Object> updates = new java.util.HashMap<>();
            updates.put("shears", finalShearsPosition);
            updates.put("bow", finalBowPosition);
            updates.put("ender_pearl", finalEnderPearlPosition);
            updates.put("active_perk1", finalActivePerk1Position);
            updates.put("active_perk2", finalActivePerk2Position);
            Main.getStore().set("playerInventories", player.getUniqueId().toString(), updates);

            Document updatedDocument = new Document(foundDocument);
            updatedDocument.put("shears", finalShearsPosition);
            updatedDocument.put("bow", finalBowPosition);
            updatedDocument.put("ender_pearl", finalEnderPearlPosition);
            updatedDocument.put("active_perk1", finalActivePerk1Position);
            updatedDocument.put("active_perk2", finalActivePerk2Position);
            PlayerDataCache.putPlayerInventories(player, updatedDocument);
        }

        player.sendMessage(Component.text("Your new inventory was successfully saved.", NamedTextColor.GREEN));

        new BukkitRunnable(){

            @Override
            public void run() {
                giveLobbyItems(player);
            }

        }.runTaskLater(Main.getInstance(), 10);
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

        // Clean up any leftover blocks from a previous game (e.g. server crashed)
        BlockBreakingSystem.clearPlacedBlocks();
        BlockBreakingSystem.resetMap();

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

        // Remove all projectiles (arrows, eggs, etc.) from the game world
        org.bukkit.World gameWorld = (MapConfig.gameWorldName != null && Bukkit.getWorld(MapConfig.gameWorldName) != null)
                ? Bukkit.getWorld(MapConfig.gameWorldName) : Bukkit.getWorlds().get(0);
        for (org.bukkit.entity.Entity entity : gameWorld.getEntities()) {
            if (entity instanceof org.bukkit.entity.Projectile) {
                entity.remove();
            }
        }
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
    public static void savePerkSelection(Player player, String perkName, PerkType perkType){
        String perkTypeString = perkType.toString().toLowerCase();

        if(perkType == PerkType.ULTIMATE && !AllActivePerks.isUltimateName(perkName)){
            player.sendMessage(Component.text("Unknown ultimate selected.", NamedTextColor.RED));
            return;
        }

        Document foundDocument = PlayerDataCache.getPlayerPerks(player);
        if(foundDocument == null){

            HashMap<String, Object> playerData = new HashMap<String, Object>(){{
                put("_id", player.getUniqueId().toString());
                put("first_active", null);
                put("second_active", null);
                put("passive", null);
                put("ultimate", AllActivePerks.getDefaultUltimateName());
            }};

            playerData.put(perkTypeString, perkName);

            Document document = new Document(playerData);
            Main.getStore().insert("playerPerks", document);
            PlayerDataCache.putPlayerPerks(player, document);

            if(perkType == PerkType.ULTIMATE){
                AllActivePerks.refreshUltimateSelection(player);
            }
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

            Main.getStore().set("playerPerks", player.getUniqueId().toString(),
                    java.util.Collections.singletonMap(perkTypeString, perkName));

            Document updatedDocument = new Document(foundDocument);
            updatedDocument.put(perkTypeString, perkName);
            PlayerDataCache.putPlayerPerks(player, updatedDocument);

            if(perkType == PerkType.ULTIMATE){
                AllActivePerks.refreshUltimateSelection(player);
            }
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
        ItemStack teamStack = createLobbyHotbarItem(
            Material.RED_BED,
            "Team Selecting",
            NamedTextColor.YELLOW,
            Arrays.asList(
                Component.text("Choose your team for the next round", NamedTextColor.GRAY),
                Component.text(" "),
                Component.text("Click to open", NamedTextColor.DARK_GRAY)
            ),
            false
        );
        inv.setItem(0, teamStack);

        // Perk Item (left side for quicker access)
        ItemStack perksStack = createLobbyHotbarItem(
            Material.ENDER_CHEST,
            "Perks",
            NamedTextColor.LIGHT_PURPLE,
            Arrays.asList(
                Component.text("Active, passive and ultimate perks", NamedTextColor.GRAY),
                Component.text(" "),
                Component.text("Click to open", NamedTextColor.DARK_GRAY)
            ),
            true
        );
        inv.setItem(1, perksStack);

        // Achievement Item
        ItemStack achievementStack = createLobbyHotbarItem(
            Material.DIAMOND,
            "Achievements",
            NamedTextColor.GOLD,
            Arrays.asList(
                Component.text("Shows all unlocked achievements", NamedTextColor.GRAY),
                Component.text(" "),
                Component.text("Click to open", NamedTextColor.DARK_GRAY)
            ),
            false
        );
        inv.setItem(2, achievementStack);

        // Vote Life Count Item
        ItemStack livesStack = createLobbyHotbarItem(
            Material.FEATHER,
            "Amount of Lives",
            NamedTextColor.GREEN,
            Arrays.asList(
                Component.text("Vote for 5, 10 or 15 lives", NamedTextColor.GRAY),
                Component.text(" "),
                Component.text("Click to open", NamedTextColor.DARK_GRAY)
            ),
            false
        );
        inv.setItem(4, livesStack);

        // Edit Inventory Item
        ItemStack inventoryStack = createLobbyHotbarItem(
            Material.CHEST,
            "Edit Inventory",
            NamedTextColor.AQUA,
            Arrays.asList(
                Component.text("Sort your in-game hotbar layout", NamedTextColor.GRAY),
                Component.text(" "),
                Component.text("Click to open", NamedTextColor.DARK_GRAY)
            ),
            false
        );
        inv.setItem(6, inventoryStack);

        // Leave Server Item
        ItemStack leaveStack = createLobbyHotbarItem(
            Material.SLIME_BALL,
            "Leave",
            NamedTextColor.RED,
            Arrays.asList(
                Component.text("Leaves the server", NamedTextColor.GRAY),
                Component.text(" "),
                Component.text("Click to execute", NamedTextColor.DARK_GRAY)
            ),
            true
        );
        inv.setItem(8, leaveStack);

    }

    public static void openPerkMenu(Player player) {
        new AnimatedPerkGUI(player).open();
    }

    public static void openLifeVotingMenu(Player player) {
        showLifeAmountVoting(player);
    }

    public static void openEditInventoryMenu(Player player) {
        showEditInventoryMenu(player);
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

        Document foundDocument = PlayerDataCache.getPlayerInventories(player);
        HashMap<String, Integer> resolvedSlots = ItemSystem.resolveBaseInventorySlots(foundDocument);
        shearsSlot = resolvedSlots.get("shears") + 9;
        bowSlot = resolvedSlots.get("bow") + 9;
        enderPearlSlot = resolvedSlots.get("enderpearl") + 9;
        activePerk1Slot = resolvedSlots.get("perk1") + 9;
        activePerk2Slot = resolvedSlots.get("perk2") + 9;

        // shears
        ActivePerk shearsPerk = Cache.getActivePerks().get("Shears");
        ItemStack shearsStack = shearsPerk != null ? shearsPerk.getItemStack().clone() : new ItemStack(Material.SHEARS);
        ItemMeta shearsMeta = shearsStack.getItemMeta();
        shearsMeta.displayName(Component.text("Shears", NamedTextColor.AQUA));
        shearsStack.setItemMeta(shearsMeta);
        inv.setItem(shearsSlot, shearsStack);

        // bow
        ActivePerk bowPerk = Cache.getActivePerks().get("Bow");
        ItemStack bowStack = bowPerk != null ? bowPerk.getItemStack().clone() : new ItemStack(Material.BOW);
        ItemMeta bowMeta = bowStack.getItemMeta();
        bowMeta.displayName(Component.text("Bow", NamedTextColor.AQUA));
        bowStack.setItemMeta(bowMeta);
        inv.setItem(bowSlot, bowStack);

        // ender pearl
        ActivePerk enderPearlPerk = Cache.getActivePerks().get("Ender Pearl");
        ItemStack enderPearlStack = enderPearlPerk != null ? enderPearlPerk.getItemStack().clone() : new ItemStack(Material.ENDER_PEARL);
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

        String firstActiveSelection = null;
        String secondActiveSelection = null;
        String passiveSelection = getSelectedPassivePerk(player);
        String ultimateSelection = getSelectedUltimate(player);

        Document foundDocument = PlayerDataCache.getPlayerPerks(player);
        if(foundDocument != null){
            Object first = foundDocument.get("first_active");
            if(first instanceof String && Cache.getActivePerks().containsKey(first)){
                firstActiveSelection = (String) first;
            }

            Object second = foundDocument.get("second_active");
            if(second instanceof String && Cache.getActivePerks().containsKey(second)){
                secondActiveSelection = (String) second;
            }
        }

        // Glass Background
        ItemStack glassStack = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassStack.getItemMeta();
        glassMeta.displayName(Component.text(" "));
        glassStack.setItemMeta(glassMeta);

        for (int i = 0; i<= 26; i++) {
            inv.setItem(i, glassStack);
        }

        // Active Perk #1
        ItemStack activeOneStack = new ItemStack(Material.IRON_SWORD);
        ItemMeta activeOneMeta = activeOneStack.getItemMeta();
        activeOneMeta.displayName(Component.text("Active Perk #1", NamedTextColor.LIGHT_PURPLE));
        activeOneMeta.lore(Arrays.asList(
            Component.text("Current: " + (firstActiveSelection != null ? translatePerkName(firstActiveSelection) : "No perk selected"), NamedTextColor.GRAY),
            Component.text("Your first active slot", NamedTextColor.DARK_GRAY),
            Component.text(" "),
            Component.text("Click to open", NamedTextColor.DARK_GRAY)
        ));
        if(firstActiveSelection != null) {
            activeOneMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
            activeOneMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        activeOneMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        activeOneStack.setItemMeta(activeOneMeta);
        inv.setItem(10, activeOneStack);

        // Active Perk #2
        ItemStack activeTwoStack = new ItemStack(Material.CROSSBOW);
        ItemMeta activeTwoMeta = activeTwoStack.getItemMeta();
        activeTwoMeta.displayName(Component.text("Active Perk #2", NamedTextColor.LIGHT_PURPLE));
        activeTwoMeta.lore(Arrays.asList(
            Component.text("Current: " + (secondActiveSelection != null ? translatePerkName(secondActiveSelection) : "No perk selected"), NamedTextColor.GRAY),
            Component.text("Your second active slot", NamedTextColor.DARK_GRAY),
            Component.text(" "),
            Component.text("Click to open", NamedTextColor.DARK_GRAY)
        ));
        if(secondActiveSelection != null) {
            activeTwoMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
            activeTwoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        activeTwoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        activeTwoStack.setItemMeta(activeTwoMeta);
        inv.setItem(12, activeTwoStack);

        // Passive Perk
        ItemStack passiveStack = new ItemStack(Material.ENDER_CHEST);
        ItemMeta passiveMeta = passiveStack.getItemMeta();
        passiveMeta.displayName(Component.text("Passive Perk", NamedTextColor.LIGHT_PURPLE));
        passiveMeta.lore(Arrays.asList(
            Component.text("Current: " + (passiveSelection != null ? translatePerkName(passiveSelection) : "No perk selected"), NamedTextColor.GRAY),
            Component.text(" "),
            Component.text("Click to open", NamedTextColor.DARK_GRAY)
        ));
        passiveMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        passiveStack.setItemMeta(passiveMeta);
        inv.setItem(14, passiveStack);

        // Ultimate
        ItemStack ultimateStack = new ItemStack(Material.NETHER_STAR);
        ItemMeta ultimateMeta = ultimateStack.getItemMeta();
        ultimateMeta.displayName(Component.text(ULTIMATE_MENU_TITLE, NamedTextColor.LIGHT_PURPLE));
        ultimateMeta.lore(Arrays.asList(
            Component.text("Current: " + (ultimateSelection != null ? translatePerkName(ultimateSelection) : translatePerkName(AllActivePerks.getDefaultUltimateName())), NamedTextColor.GRAY),
            Component.text(" "),
            Component.text("Click to open", NamedTextColor.DARK_GRAY)
        ));
        ultimateMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        ultimateStack.setItemMeta(ultimateMeta);
        inv.setItem(16, ultimateStack);

        player.openInventory(inv);
    }

    /**
     * A Method that shows the inventar to choose between the different perk types to change them.
     * @author SimsumMC
     */
    private static void showActivePerkMenu(Player player, PerkType perkType) {
        String perkTypeString = perkType.toString().toLowerCase();
        String selectedPerk = null;

        Document foundDocument = PlayerDataCache.getPlayerPerks(player);
        if(foundDocument != null && foundDocument.get(perkTypeString) instanceof String) {
            selectedPerk = (String) foundDocument.get(perkTypeString);
        }

        Inventory inv = Bukkit.createInventory(null, 6*9, Component.text("Active Perk #" + perkType.value, NamedTextColor.LIGHT_PURPLE));
        fillWithGlass(inv);

        int slot = 0;
        for(ActivePerk perk : Cache.getActivePerks().values()) {
            if(!perk.getSelectableStatus()) {
                continue;
            }

            ItemStack itemStack = perk.getItemStack().clone();
            ItemMeta itemMeta = itemStack.getItemMeta();
            String internalName = plainName(itemMeta);

            itemMeta.displayName(Component.text(translatePerkName(internalName), NamedTextColor.AQUA));

            if(selectedPerk != null && selectedPerk.equals(internalName)) {
                itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            else if(itemMeta.hasEnchants()) {
                for(Enchantment enchantment : itemMeta.getEnchants().keySet()) {
                    itemMeta.removeEnchant(enchantment);
                }
            }

            ArrayList<Component> newLore = new ArrayList<>(buildPerkDescriptionLore(perk.getDescription()));
            newLore.add(Component.text(" "));
            newLore.add(Component.text("Wool: ", NamedTextColor.GOLD).append(Component.text(perk.getWoolCost(), NamedTextColor.DARK_PURPLE)));
            newLore.add(Component.text("Cooldown: ", NamedTextColor.GOLD).append(Component.text(perk.getCooldown() + "s", NamedTextColor.DARK_PURPLE)));
            newLore.add(Component.text("Click to select", NamedTextColor.GRAY));

            itemMeta.lore(newLore);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            itemStack.setItemMeta(itemMeta);

            while(slot < 53 && slot % 9 == 8) {
                slot += 1;
            }
            if(slot >= 53) {
                break;
            }

            inv.setItem(slot, itemStack);
            slot += 1;
        }

        // Back Item
        ItemStack backStack = new ItemStack(Material.OAK_DOOR);
        ItemMeta backMeta = backStack.getItemMeta();
        backMeta.displayName(Component.text("Go Back", NamedTextColor.RED));
        backStack.setItemMeta(backMeta);
        inv.setItem(53, backStack);

        player.openInventory(inv);
    }

    private static void showUltimatePerkMenu(Player player) {
        String selectedUltimate = getSelectedUltimate(player);

        Inventory inv = Bukkit.createInventory(null, 3*9, Component.text(ULTIMATE_MENU_TITLE, NamedTextColor.LIGHT_PURPLE));
        fillWithGlass(inv);

        int[] ultimateSlots = {10, 11, 12, 13, 14, 15, 16};
        int index = 0;

        for(AllActivePerks.UltimateDefinition definition : AllActivePerks.getUltimateDefinitions().values()) {
            if(index >= ultimateSlots.length) {
                break;
            }

            ItemStack itemStack = new ItemStack(definition.getIconMaterial());
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.displayName(Component.text(translatePerkName(definition.getDisplayName()), NamedTextColor.LIGHT_PURPLE));

            if(definition.getDisplayName().equals(selectedUltimate)) {
                itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            ArrayList<Component> lore = new ArrayList<>(buildPerkDescriptionLore(definition.getDescription()));
            lore.add(Component.text(" "));
            lore.add(Component.text("Click to select", NamedTextColor.GRAY));
            itemMeta.lore(lore);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

            itemStack.setItemMeta(itemMeta);
            inv.setItem(ultimateSlots[index], itemStack);
            index += 1;
        }

        ItemStack backStack = new ItemStack(Material.OAK_DOOR);
        ItemMeta backMeta = backStack.getItemMeta();
        backMeta.displayName(Component.text("Go Back", NamedTextColor.RED));
        backStack.setItemMeta(backMeta);
        inv.setItem(26, backStack);

        player.openInventory(inv);
    }

    private static void showPassivePerkMenu(Player player) {
        String selectedPerk = getSelectedPassivePerk(player);

        Inventory inv = Bukkit.createInventory(null, 3*9, Component.text(PASSIVE_PERK_MENU_TITLE, NamedTextColor.LIGHT_PURPLE));
        fillWithGlass(inv);

        ItemStack selectedPerkInfo = new ItemStack(Material.NETHER_STAR);
        ItemMeta selectedPerkInfoMeta = selectedPerkInfo.getItemMeta();
        selectedPerkInfoMeta.displayName(Component.text("Current Passive", NamedTextColor.AQUA, TextDecoration.BOLD));
        ArrayList<Component> selectedPerkLore = new ArrayList<>();
        selectedPerkLore.add(Component.text(selectedPerk != null ? translatePerkName(selectedPerk) : "No perk selected", NamedTextColor.WHITE));
        selectedPerkLore.add(Component.text(" "));
        selectedPerkLore.add(Component.text("Choose a category below", NamedTextColor.DARK_GRAY));
        selectedPerkInfoMeta.lore(selectedPerkLore);
        selectedPerkInfoMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        selectedPerkInfo.setItemMeta(selectedPerkInfoMeta);
        inv.setItem(4, selectedPerkInfo);

        inv.setItem(11, createPassiveCategoryItem(
                Material.GOLD_INGOT,
            PASSIVE_CATEGORY_ECONOMY,
                NamedTextColor.GOLD,
            "Wool, costs and build speed",
            isPerkSelectedInCategory(selectedPerk, PASSIVE_CATEGORY_ECONOMY)
            ));
        inv.setItem(13, createPassiveCategoryItem(
                Material.SHIELD,
            PASSIVE_CATEGORY_DEFENSE,
                NamedTextColor.RED,
            "Knockback and survivability",
            isPerkSelectedInCategory(selectedPerk, PASSIVE_CATEGORY_DEFENSE)
            ));
        inv.setItem(15, createPassiveCategoryItem(
                Material.FEATHER,
                PASSIVE_CATEGORY_UTILITY,
                NamedTextColor.AQUA,
            "Movement and positioning",
                isPerkSelectedInCategory(selectedPerk, PASSIVE_CATEGORY_UTILITY)
            ));

        ItemStack backStack = new ItemStack(Material.OAK_DOOR);
        ItemMeta backMeta = backStack.getItemMeta();
        backMeta.displayName(Component.text("Go Back", NamedTextColor.RED));
        backStack.setItemMeta(backMeta);
        inv.setItem(26, backStack);

        player.openInventory(inv);
    }

    private static void showPassivePerkCategoryMenu(Player player, String categoryName) {
        LinkedHashMap<String, List<String>> categories = getPassivePerkCategories();
        List<String> categoryPerks = categories.get(categoryName);

        if(categoryPerks == null) {
            showPassivePerkMenu(player);
            return;
        }

        String selectedPerk = getSelectedPassivePerk(player);

        Inventory inv = Bukkit.createInventory(null, 3*9, Component.text(getPassiveCategoryInventoryTitle(categoryName), NamedTextColor.LIGHT_PURPLE));
        fillWithGlass(inv);

        Material categoryMaterial;
        NamedTextColor categoryColor;
        String categorySubtitle;

        switch(categoryName) {
            case PASSIVE_CATEGORY_ECONOMY:
                categoryMaterial = Material.GOLD_INGOT;
                categoryColor = NamedTextColor.GOLD;
                categorySubtitle = "Wool, costs and build speed";
                break;
            case PASSIVE_CATEGORY_DEFENSE:
                categoryMaterial = Material.SHIELD;
                categoryColor = NamedTextColor.RED;
                categorySubtitle = "Knockback and survivability";
                break;
            default:
                categoryMaterial = Material.FEATHER;
                categoryColor = NamedTextColor.AQUA;
                categorySubtitle = "Movement and positioning";
                break;
        }

        ItemStack categoryInfo = createPassiveCategoryItem(categoryMaterial, categoryName, categoryColor, categorySubtitle, selectedPerk != null && categoryPerks.contains(selectedPerk));
        inv.setItem(4, categoryInfo);

        int[] perkSlots = {10, 11, 12, 13, 14, 15, 16};
        HashMap<String, PassivePerk<? extends Event, ?>> passivePerks = Cache.getPassivePerks();

        for(int i = 0; i < categoryPerks.size() && i < perkSlots.length; i++) {
            String perkName = categoryPerks.get(i);
            PassivePerk<? extends Event, ?> perk = passivePerks.get(perkName);

            if(perk == null) {
                continue;
            }

            ItemStack itemStack = perk.getItem().clone();
            ItemMeta itemMeta = itemStack.getItemMeta();

            itemMeta.displayName(Component.text(translatePerkName(perkName), NamedTextColor.AQUA));

            if(selectedPerk != null && perkName.equals(selectedPerk)){
                itemMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            else {
                if(itemMeta.hasEnchants()) {
                    for(Enchantment enchantment : itemMeta.getEnchants().keySet()) {
                        itemMeta.removeEnchant(enchantment);
                    }
                }
            }

            ArrayList<Component> newLore = new ArrayList<>(buildPerkDescriptionLore(perk.getDescription()));
            newLore.add(Component.text(" "));
            newLore.add(Component.text("Click to select", NamedTextColor.GRAY));

            itemMeta.lore(newLore);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            itemStack.setItemMeta(itemMeta);

            inv.setItem(perkSlots[i], itemStack);
        }

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
        }.runTaskTimer(Main.getInstance(), 0, 5);

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
                }

                scoreboardTitleOffset = (scoreboardTitleOffset + 1) % SCOREBOARD_TITLE_COLORS.length;
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);

    }

    private static String getAnimatedScoreboardTitle() {
        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 0; i < SCOREBOARD_TITLE_TEXT.length(); i++) {
            String color = SCOREBOARD_TITLE_COLORS[(i + scoreboardTitleOffset) % SCOREBOARD_TITLE_COLORS.length];
            titleBuilder.append(color).append(SCOREBOARD_TITLE_TEXT.charAt(i));
        }
        return titleBuilder.toString();
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

        Objective obj = board.registerNewObjective("Lobby", "dummy", getAnimatedScoreboardTitle());
        obj.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team team = board.registerNewTeam("team");
        Team map = board.registerNewTeam("map");
        Team lives = board.registerNewTeam("lives");
        Team players = board.registerNewTeam("players");

        obj.getScore("§8§m                         ").setScore(9);
        obj.getScore("§0").setScore(8);

        obj.getScore("§c").setScore(7);

        obj.getScore("§1").setScore(6);

        obj.getScore("§d").setScore(5);

        obj.getScore("§3").setScore(4);

        obj.getScore("§a").setScore(3);

        obj.getScore("§5").setScore(2);

        obj.getScore("§b").setScore(1);

        obj.getScore("§7§m                         ").setScore(0);

        String teamName = TeamSystem.getPlayerTeam(player, true);
        team.addEntry("§c");
        team.setPrefix("§7 Team §8» " + TeamSystem.getScoreboardTeamName(teamName));

        map.addEntry("§d");
        map.setPrefix("§7 Map §8» §e" + MapConfig.mapName);

        lives.addEntry("§a");
        lives.setPrefix("§7 Lives §8» §a" + Config.defaultLives);

        players.addEntry("§b");
        players.setPrefix("§7 Players §8» §b" + actualPlayers + "§8/§b" + maxPlayers);

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

        Objective objective = board.getObjective("Lobby");
        if (objective != null) {
            objective.setDisplayName(getAnimatedScoreboardTitle());
        }

        Team team = board.getTeam("team");
        Team map = board.getTeam("map");
        Team lives = board.getTeam("lives");
        Team players = board.getTeam("players");

        String teamName = TeamSystem.getPlayerTeam(player, true);
        team.setPrefix("§7 Team §8» " + TeamSystem.getScoreboardTeamName(teamName));
        map.setPrefix("§7 Map §8» §e" + MapConfig.mapName);
        lives.setPrefix("§7 Lives §8» §a" + getTopVotedLifeAmount());
        players.setPrefix("§7 Players §8» §b" + actualPlayers + "§8/§b" + maxPlayers);

    }

    /**
     * A Method that changes the scoreboard for the given player to the game scoreboard.
     * @param player the player that gets the scoreboard modified
     * @author SimsumMC
     */
    public static void setGameScoreBoard(Player player) {

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();

        Objective obj = board.registerNewObjective("Game", "dummy", getAnimatedScoreboardTitle());
        obj.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());

        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team team = board.registerNewTeam("team");
        Team map = board.registerNewTeam("map");

        Team redTeam = board.registerNewTeam("redTeam");
        Team blueTeam = board.registerNewTeam("blueTeam");
        Team greenTeam = board.registerNewTeam("greenTeam");
        Team yellowTeam = board.registerNewTeam("yellowTeam");

        obj.getScore("§8§m                         ").setScore(11);
        obj.getScore("§0").setScore(10);

        obj.getScore("§c").setScore(9);

        obj.getScore("§1").setScore(8);

        obj.getScore("§d").setScore(7);

        obj.getScore("§3").setScore(6);

        obj.getScore("§4").setScore(5);
        obj.getScore("§9").setScore(4);
        obj.getScore("§2").setScore(3);
        obj.getScore("§6").setScore(2);

        obj.getScore("§5").setScore(1);
        obj.getScore("§7§m                         ").setScore(0);

        String teamName = TeamSystem.getPlayerTeam(player, true);
        team.addEntry("§c");
        team.setPrefix("§7 Team §8» " + TeamSystem.getScoreboardTeamName(teamName));

        map.addEntry("§d");
        map.setPrefix("§7 Map §8» §e" + MapConfig.mapName);

        HashMap<String, Integer> teamLives = Cache.getTeamLives();
        int redLives = teamLives.get("Red");
        int blueLives = teamLives.get("Blue");
        int greenLives = teamLives.get("Green");
        int yellowLives = teamLives.get("Yellow");

        redTeam.addEntry("§4");
        redTeam.setPrefix("§c ■ §fRed §8» §c" + redLives);

        blueTeam.addEntry("§9");
        blueTeam.setPrefix("§9 ■ §fBlue §8» §9" + blueLives);

        greenTeam.addEntry("§2");
        greenTeam.setPrefix("§a ■ §fGreen §8» §a" + greenLives);

        yellowTeam.addEntry("§6");
        yellowTeam.setPrefix("§e ■ §fYellow §8» §e" + yellowLives);

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

        Objective objective = board.getObjective("Game");
        if (objective != null) {
            objective.setDisplayName(getAnimatedScoreboardTitle());
        }

        Team team = board.getTeam("team");
        Team map = board.getTeam("map");

        Team redTeam = board.getTeam("redTeam");
        Team blueTeam = board.getTeam("blueTeam");
        Team greenTeam = board.getTeam("greenTeam");
        Team yellowTeam = board.getTeam("yellowTeam");

        String teamName = TeamSystem.getPlayerTeam(player, true);
        team.setPrefix("§7 Team §8» " + TeamSystem.getScoreboardTeamName(teamName));
        map.setPrefix("§7 Map §8» §e" + MapConfig.mapName);

        HashMap<String, Integer> teamLives = Cache.getTeamLives();
        int redLives = teamLives.get("Red");
        int blueLives = teamLives.get("Blue");
        int greenLives = teamLives.get("Green");
        int yellowLives = teamLives.get("Yellow");

        redTeam.setPrefix("§c ■ §fRed §8» §c" + redLives);
        blueTeam.setPrefix("§9 ■ §fBlue §8» §9" + blueLives);
        greenTeam.setPrefix("§a ■ §fGreen §8» §a" + greenLives);
        yellowTeam.setPrefix("§e ■ §fYellow §8» §e" + yellowLives);
    }
}