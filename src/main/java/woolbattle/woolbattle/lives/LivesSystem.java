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

package woolbattle.woolbattle.lives;

import org.bukkit.Bukkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import woolbattle.woolbattle.Cache;
import woolbattle.woolbattle.Config;
import woolbattle.woolbattle.MapConfig;
import woolbattle.woolbattle.achievements.AchievementSystem;
import woolbattle.woolbattle.team.TeamSystem;
import woolbattle.woolbattle.lobby.LobbySystem;
import woolbattle.woolbattle.perks.AllPassivePerks;

import java.util.HashMap;

import static woolbattle.woolbattle.base.Base.resetEnderPearls;
import static woolbattle.woolbattle.team.TeamSystem.getPlayerTeam;
import static woolbattle.woolbattle.team.TeamSystem.getTeamColour;

public class LivesSystem implements Listener {

    /**
     * A Method that teleports the player to the team spawn.
     * @param player the player that gets teleported
     * @author SimsumMC
     */
    public static void teleportPlayerTeamSpawn(Player player){
        String team = TeamSystem.getPlayerTeam(player, true);

        switch(team){
            case "Blue":
                player.teleport(MapConfig.blueLocation);
                break;
            case "Red":
                player.teleport(MapConfig.redLocation);
                break;
            case "Green":
                player.teleport(MapConfig.greenLocation);
                break;
            case "Yellow":
                player.teleport(MapConfig.yellowLocation);
                break;
            default:
                player.teleport(MapConfig.midLocation);
                break;
        }
    }

    /**
     * A Method that updates the spawnProtection HashMap in the Cache with the current unix timestamp.
     * @param player the player that gets teleported
     * @param length the length of the spawn protection in seconds
     * @author SimsumMC
     */
    public static void setPlayerSpawnProtection(Player player, int length){
        long unixTime = (System.currentTimeMillis() / 1000L) + length;

        HashMap<Player, Long> spawnProtection = Cache.getSpawnProtection();

        spawnProtection.put(player, unixTime);

        Cache.setSpawnProtection(spawnProtection);
    }

    /**
     * An Event that gets executed whenever a Player moves to use it as a kill event when a player gets under a
     * specific y coordinate.
     *
     * @param event the PlayerMoveEvent
     * @author SimsumMC & Beelzebub
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if(!LobbySystem.gameStarted){
            return;
        }

        Player player = event.getPlayer();

        HashMap<Player, Long> lastDamage = Cache.getLastDamage();

        long unixTime = System.currentTimeMillis() / 1000L;

        if (player.getLocation().getY() <= MapConfig.minHeight) {
            resetEnderPearls(player);
            AllPassivePerks.resetPerLifeState(player);
            if (lastDamage.containsKey(player)) {
                long realLastDamage = lastDamage.get(player);
                if (unixTime - realLastDamage >= Config.deathCooldown) {
                    teleportPlayerTeamSpawn(player);
                    setPlayerSpawnProtection(player, Config.spawnProtectionLengthAfterDeath);
                    return;
                }
            }

            HashMap<Player, Player> playerDuels = Cache.getPlayerDuels();
            Player otherPlayer = playerDuels.get(player);
            if(otherPlayer != null){
                playerDuels.remove(player);
                playerDuels.remove(otherPlayer);
                Cache.setPlayerDuels(playerDuels);

                TextColor otherPlayerColor = getTeamColour(getPlayerTeam(otherPlayer, true));
                TextColor playerColor = getTeamColour(getPlayerTeam(player, true));

                player.sendMessage(Component.text("You are now in a duel with ", NamedTextColor.GOLD)
                        .append(Component.text(otherPlayer.getName(), otherPlayerColor))
                        .append(Component.text("!", NamedTextColor.GOLD)));
                otherPlayer.sendMessage(Component.text("You are now in a duel with ", NamedTextColor.GOLD)
                        .append(Component.text(player.getName(), playerColor))
                        .append(Component.text("!", NamedTextColor.GOLD)));
            }

            String team = TeamSystem.getPlayerTeam(player, true);

            HashMap<String, Integer> teamLives = Cache.getTeamLives();

            int lives = teamLives.get(team);

            EntityDamageEvent lastDamageEvent = event.getPlayer().getLastDamageCause();

            Entity damager;

            if(lastDamageEvent instanceof EntityDamageByEntityEvent){
                damager = ((EntityDamageByEntityEvent) lastDamageEvent).getDamager();
            }
            else{
                damager = null;
            }

            if(damager == null){
                teleportPlayerTeamSpawn(player);
                setPlayerSpawnProtection(player, Config.spawnProtectionLengthAfterDeath);
            }

            if(damager instanceof Arrow){
                Arrow arrow = (Arrow) damager;
                damager = (Entity) arrow.getShooter();

            }

            if (damager instanceof Player) {
                if(lives != 0){
                    lives -= 1;
                }
                teamLives.put(team, lives);
                Cache.setTeamLives(teamLives);

                lastDamage.remove(damager);
                Cache.setLastDamage(lastDamage);

                String damagerTeam = TeamSystem.getPlayerTeam((Player) damager, true);
                TextColor damagerTeamColour = TeamSystem.getTeamColour(damagerTeam);
                Component killMessage = Component.text("The player ", NamedTextColor.GRAY)
                        .append(Component.text(player.getName(), TeamSystem.getTeamColour(team)))
                        .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                        .append(Component.text(((Player) damager).getName(), damagerTeamColour))
                        .append(Component.text(".", NamedTextColor.GRAY));

                Bukkit.broadcast(killMessage);
                HashMap<String, HashMap<Player, Integer>> killStreaks = Cache.getKillStreaks();

                HashMap<Player, Integer> kills = killStreaks.get(damagerTeam);

                killStreaks.put(team, new HashMap<Player, Integer>(){{put(player, 0);}});

                int amKills;
                if(kills.containsKey(damager)){
                    amKills = kills.get(damager) + 1;
                }
                else{
                    amKills = 1;
                }

                kills.put((Player) damager, amKills);

                HashMap<Player, HashMap<String, Integer>> playerStats = Cache.getPlayerStats();

                if(amKills == 5){

                    AchievementSystem.giveKillstreak5((Player) damager);

                    Component streakMessage = Component.text("The player ", NamedTextColor.GRAY)
                            .append(Component.text(((Player) damager).getName(), damagerTeamColour))
                            .append(Component.text(" has a 5er kill streak!", NamedTextColor.GRAY));
                    Bukkit.broadcast(streakMessage);

                    kills.put((Player) damager, 0);


                    teamLives = Cache.getTeamLives();
                    teamLives.put(damagerTeam, (teamLives.get(damagerTeam) + 1));
                    Cache.setTeamLives(teamLives);

                    HashMap<String, Integer> damagerStatsNew = playerStats.get(damager);
                    damagerStatsNew.put("streaks", (damagerStatsNew.get("streaks") + 1));
                    playerStats.put((Player) damager, damagerStatsNew);
                }

                killStreaks.put(damagerTeam, kills);
                Cache.setKillStreaks(killStreaks);

                if (lives == 0) {
                    TeamSystem.removePlayerTeam(player);
                    LobbySystem.setPlayerSpectator(player);
                }
                else{
                    teleportPlayerTeamSpawn(player);
                    setPlayerSpawnProtection(player, Config.spawnProtectionLengthAfterDeath);
                }

                HashMap<String, Integer> damagerStats = playerStats.get(damager);
                damagerStats.put("kills", (damagerStats.get("kills") + 1));
                playerStats.put((Player) damager, damagerStats);

                HashMap<String, Integer> damagedStats = playerStats.get(player);
                damagedStats.put("deaths", (damagedStats.get("deaths") + 1));
                playerStats.put(player, damagedStats);

                Cache.setPlayerStats(playerStats);

            }
            LobbySystem.determinateWinnerTeam();
        }
    }
}