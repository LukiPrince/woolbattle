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

package woolbattle.woolbattle.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import woolbattle.woolbattle.Cache;
import woolbattle.woolbattle.lobby.LobbySystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import static woolbattle.woolbattle.Cache.getTeamMembers;


public class TeamSystem implements Listener {

    /**
     * A method that handles the team division.
     * @author Beelzebub
     */
    public static void teamsOnStart() {
        int numActiveTeams = 0;
        String teamWithMembers = null;
        ArrayList<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        ArrayList<Player> teamLessPlayers = new ArrayList<>();

        for (int i = onlinePlayers.size() - 1; i>=0; i--) {
            Player player = onlinePlayers.get(i);
            if (TeamSystem.getPlayerTeam(player, true).equals("Not selected")) {
                teamLessPlayers.add(player);
            }
        }

        for (int i = teamLessPlayers.size() - 1; i>=0; i--) {
            Player teamLessPlayer = teamLessPlayers.get(i);
            int[] sizes = {
                    getTeamMembers().get("Red").size(),
                    getTeamMembers().get("Blue").size(),
                    getTeamMembers().get("Green").size(),
                    getTeamMembers().get("Yellow").size()
            };
            int smallestSize = Integer.MAX_VALUE;
            ArrayList<Integer> smallestTeams = new ArrayList<>();
            for(int a=0;a<sizes.length;a++) {
                if(sizes[a] < smallestSize) {
                    smallestSize = sizes[a];
                    smallestTeams.clear();
                    smallestTeams.add(a);
                }
                else if (sizes[a] == smallestSize) {
                    smallestTeams.add(a);
                }
            }
            int smallestNumber = smallestTeams.get(ThreadLocalRandom.current().nextInt(smallestTeams.size()));
            switch (smallestNumber){
                case 0:
                    (getTeamMembers().get("Red")).add(teamLessPlayer);
                    teamLessPlayer.sendMessage(Component.text("You didn't enter a team so you were put into team ", NamedTextColor.GRAY).append(Component.text("red", NamedTextColor.RED)).append(Component.text("!", NamedTextColor.GRAY)));
                    break;
                case 1: (getTeamMembers().get("Blue")).add(teamLessPlayer);
                    teamLessPlayer.sendMessage(Component.text("You didn't enter a team so you were put into team ", NamedTextColor.GRAY).append(Component.text("blue", NamedTextColor.DARK_BLUE)).append(Component.text("!", NamedTextColor.GRAY)));
                    break;
                case 2: (getTeamMembers().get("Green")).add(teamLessPlayer);
                    teamLessPlayer.sendMessage(Component.text("You didn't enter a team so you were put into team ", NamedTextColor.GRAY).append(Component.text("green", NamedTextColor.DARK_GREEN)).append(Component.text("!", NamedTextColor.GRAY)));
                    break;
                case 3: (getTeamMembers().get("Yellow")).add(teamLessPlayer);
                    teamLessPlayer.sendMessage(Component.text("You didn't enter a team so you were put into team ", NamedTextColor.GRAY).append(Component.text("yellow", NamedTextColor.YELLOW)).append(Component.text("!", NamedTextColor.GRAY)));
                    break;
            }
        }
        int[] sizes = {
                getTeamMembers().get("Red").size(),
                getTeamMembers().get("Blue").size(),
                getTeamMembers().get("Green").size(),
                getTeamMembers().get("Yellow").size()
        };
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i] > 0) {
                switch (i) {
                    case 0: teamWithMembers = "Red"; break;
                    case 1: teamWithMembers = "Blue"; break;
                    case 2: teamWithMembers = "Green"; break;
                    case 3: teamWithMembers = "Yellow"; break;
                }
                numActiveTeams += 1;
            }
        }

        if (numActiveTeams < 2) {
            int size = getTeamMembers().get(teamWithMembers).size();
            if (size >= 2) {

                ArrayList<Player> member = getTeamMembers().get(teamWithMembers);

                if (!teamWithMembers.equals("Blue")) {
                    HashMap<String, ArrayList<Player>> members = getTeamMembers();

                    ArrayList<Player> newMem = new ArrayList<Player>() {{
                        add(member.get(0));
                    }};
                    member.remove(0);
                    members.put("Blue", newMem);

                    members.put(teamWithMembers, member);

                    Cache.setTeamMembers(members);
                }
                else {
                    HashMap<String, ArrayList<Player>> members = getTeamMembers();

                    ArrayList<Player> newMem = new ArrayList<Player>() {{
                        add(member.get(0));
                    }};
                    member.remove(0);
                    members.put("Red", newMem);

                    members.put(teamWithMembers, member);

                    Cache.setTeamMembers(members);
                }

            }
        }
    }

    /**
     * A method that opens the Inventory for the team selection for the given player.
     * @param player - the player that gets the inventory "shown" (opened)
     * @author Beelzebub
     */
    public static void showTeamSelectionInventory(Player player) {
        Inventory voting = Bukkit.createInventory(null, 27, Component.text("Team Selecting", NamedTextColor.YELLOW));

        //adding glass
        ItemStack Glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta GlassMeta = Glass.getItemMeta();
        GlassMeta.displayName(Component.text(" "));
        Glass.setItemMeta(GlassMeta);

        for (int i = 0; i <= 26; i++) {
            voting.setItem(i, Glass);
        }

        // Adding the "Vote for red" item
        ItemStack voteRed = new ItemStack(Material.RED_WOOL);
        ArrayList<Component> voteRedLore = new ArrayList<>();
        ItemMeta voteRedMeta = voteRed.getItemMeta();
        for (int i = getTeamMembers().get("Red").size() - 1; i >= 0; i--) {
            voteRedLore.add(Component.text("» " + getTeamMembers().get("Red").get(i).getName(), NamedTextColor.GRAY));
        }

        voteRedMeta.displayName(Component.text("Team Red", NamedTextColor.RED));
        voteRedMeta.lore(voteRedLore);
        voteRed.setItemMeta(voteRedMeta);
        voting.setItem(11, voteRed);

        // Adding the "Vote for blue" item
        ItemStack voteBlue = new ItemStack(Material.BLUE_WOOL);
        ArrayList<Component> voteBlueLore = new ArrayList<>();
        ItemMeta voteBlueMeta = voteBlue.getItemMeta();
        for (int i = getTeamMembers().get("Blue").size() - 1; i >= 0; i--) {
            voteBlueLore.add(Component.text("» " + getTeamMembers().get("Blue").get(i).getName(), NamedTextColor.GRAY));
        }

        voteBlueMeta.displayName(Component.text("Team Blue", NamedTextColor.BLUE));
        voteBlueMeta.lore(voteBlueLore);
        voteBlue.setItemMeta(voteBlueMeta);
        voting.setItem(12, voteBlue);

        // Adding the "Vote for Green" item
        ItemStack voteGreen = new ItemStack(Material.LIME_WOOL);
        ArrayList<Component> voteGreenLore = new ArrayList<>();
        ItemMeta voteGreenMeta = voteGreen.getItemMeta();
        for (int i = getTeamMembers().get("Green").size() - 1; i >= 0; i--) {
            voteGreenLore.add(Component.text("» " + getTeamMembers().get("Green").get(i).getName(), NamedTextColor.GRAY));
        }

        voteGreenMeta.displayName(Component.text("Team Green", NamedTextColor.GREEN));
        voteGreenMeta.lore(voteGreenLore);
        voteGreen.setItemMeta(voteGreenMeta);
        voting.setItem(14, voteGreen);

        // Adding the "Vote for Yellow" item
        ItemStack voteYellow = new ItemStack(Material.YELLOW_WOOL);
        ArrayList<Component> voteYellowLore = new ArrayList<>();
        ItemMeta voteYellowMeta = voteYellow.getItemMeta();
        for (int i = getTeamMembers().get("Yellow").size() - 1; i >= 0; i--) {
            voteYellowLore.add(Component.text("» " + getTeamMembers().get("Yellow").get(i).getName(), NamedTextColor.GRAY));
        }

        voteYellowMeta.displayName(Component.text("Team Yellow", NamedTextColor.YELLOW));
        voteYellowMeta.lore(voteYellowLore);
        voteYellow.setItemMeta(voteYellowMeta);
        voting.setItem(15, voteYellow);

        player.openInventory(voting);
    }

    /**
     * An event that gets executed whenever an entity damages another entity to prevent hitting team members.
     * @param event - the EntityDamageByEntityEvent
     * @author Beelzebub & SimsumMC
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event){
        if(!(event.getEntity() instanceof Player)){
            return;
        }

        if(event.getDamager() instanceof Player){
            HashMap<Player, Player> playerDuels = Cache.getPlayerDuels();
            Player player = (Player) event.getEntity();
            if(playerDuels.containsKey(player) && (playerDuels.get(player) != event.getDamager())){
                event.setCancelled(true);
                Component duelPlayerName = Component.text(((Player) event.getEntity()).getName(),
                        getTeamColour(getPlayerTeam(playerDuels.get(player), true)));
                event.getDamager().sendMessage(Component.text("This player is in a duel with ", NamedTextColor.RED)
                        .append(duelPlayerName).append(Component.text("!", NamedTextColor.RED)));
                return;
            }
            player = (Player) event.getDamager();
            if(playerDuels.containsKey(player) && (playerDuels.get(player) != event.getEntity())){
                event.setCancelled(true);
                Component duelPlayerName = Component.text(((Player) event.getEntity()).getName(),
                        getTeamColour(getPlayerTeam(playerDuels.get(player), true)));
                event.getDamager().sendMessage(Component.text("This player is in a duel with ", NamedTextColor.RED)
                        .append(duelPlayerName).append(Component.text("!", NamedTextColor.RED)));
                return;
            }
        }

        HashMap<Player, Long> lastDamage = Cache.getLastDamage();
        long unixTime = System.currentTimeMillis() / 1000L;
        lastDamage.put((Player) event.getEntity(), unixTime);
        Cache.setLastDamage(lastDamage);

        Player damager;
        Player damaged = (Player) event.getEntity();

        if(event.getDamager() instanceof Arrow){
            Arrow arrow = (Arrow) event.getDamager();
            damager = (Player) arrow.getShooter();

        }
        else{
            if(!(event.getDamager() instanceof Player)){
                return;
            }
            damager = (Player) event.getDamager();
        }

        if (damaged != null && damager != null) {
            if (TeamSystem.getPlayerTeam(damager, true).equals(TeamSystem.getPlayerTeam(damaged, true)) || !LobbySystem.gameStarted) {
                Vector velocity;
                velocity = event.getEntity().getVelocity();
                event.getEntity().setVelocity(velocity);
                event.setCancelled(true);
                return;
            }
            HashMap<Player, Long> spawnProtection = Cache.getSpawnProtection();
            if(spawnProtection.containsKey(damaged) && (unixTime < spawnProtection.get(damaged))){
                if(damager.getUniqueId() != damaged.getUniqueId()){
                    damager.sendMessage(Component.text("The player has spawn protection!", NamedTextColor.RED));
                }
                event.setCancelled(true);
            }
        }
    }

    /**
     * A Method that returns the team of the player with the colour as a string.
     * @param player the player which team gets returned
     * @param raw a boolean whether the method should return a raw string or a colored one
     * @return the team name as a string if any, else "Not selected"
     * @author SimsumMC
     */
    public static String getPlayerTeam(Player player, boolean raw) {

        String teamName = "Not selected";
        HashMap<String, ArrayList<Player>> data = getTeamMembers();

        for(String key : data.keySet()){
            ArrayList<Player> players = data.get(key);
            if(players.contains(player)){
                teamName = key;
                break;
            }
        }
        return teamName;

    }

    /**
     * A Method that removes the player from the current team
     * @param player which gets removed from his team
     * @author SimsumMC
     */
    public static void removePlayerTeam(Player player) {
        HashMap<String, ArrayList<Player>> teamMembers = getTeamMembers();

        for(String key : teamMembers.keySet()){
            ArrayList<Player> players = teamMembers.get(key);
            if(players.contains(player)){
                players.remove(player);
                teamMembers.put(key, players);
                Cache.setTeamMembers(teamMembers);
                break;
            }
        }
    }

    /**
     * A Method that returns the team of the player with the colour as a TextColor.
     * @param team a String representing the team name
     * @return the colour of the team
     * @author SimsumMC
     */
    public static TextColor getTeamColour(String team) {
        return switch (team) {
            case "Blue" -> NamedTextColor.DARK_BLUE;
            case "Green" -> NamedTextColor.GREEN;
            case "Yellow" -> NamedTextColor.YELLOW;
            case "Red" -> NamedTextColor.DARK_RED;
            default -> NamedTextColor.WHITE;
        };
    }

    /**
     * Returns a legacy-colored team label for sidebar scoreboards.
     * @param rawTeamName a raw team value like Red/Blue/Green/Yellow
     * @return the team name including legacy color code, or gray None if no team is selected
     */
    public static String getScoreboardTeamName(String rawTeamName) {
        return switch (rawTeamName) {
            case "Red" -> "§cRed";
            case "Blue" -> "§9Blue";
            case "Green" -> "§aGreen";
            case "Yellow" -> "§eYellow";
            default -> "§8None";
        };
    }

    /**
     * Method that returns the team-color of the specified player as a DyeColor.
     * @param p The player to get the team-color of
     * @author Servaturus
     */
    public static DyeColor findTeamDyeColor(Player p){
        String team = getPlayerTeam(p, true);
        switch(team){
            case "Blue":
                return DyeColor.BLUE;
            case "Red":
                return DyeColor.RED;
            case "Green":
                return DyeColor.LIME;
            case "Yellow":
                return DyeColor.YELLOW;
            default:
                return DyeColor.WHITE;
        }
    }

    /**
     * Method that returns the team-color of the specified player as a Color.
     * @param p The player to get the team-color of
     * @author Servaturus
     */
    public static Color findTeamColor(Player p){
        String team = getPlayerTeam(p, true);
        switch(team){
            case "Blue":
                return Color.BLUE;
            case "Red":
                return Color.RED;
            case "Green":
                return Color.LIME;
            case "Yellow":
                return Color.YELLOW;
            default:
                return Color.WHITE;
        }
    }
}
