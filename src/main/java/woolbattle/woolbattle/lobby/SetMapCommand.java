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
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package woolbattle.woolbattle.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import woolbattle.woolbattle.MapConfig;
import woolbattle.woolbattle.woolsystem.BlockBreakingSystem;

import java.io.File;

public class SetMapCommand implements CommandExecutor {

    /**
     * Command: /setmap <mapname>
     * Switches the active map. Can only be used when no game is running.
     * Requires OP.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && !(sender == Bukkit.getConsoleSender())) {
            return false;
        }

        if (sender instanceof Player && !((Player) sender).isOp()) {
            sender.sendMessage(Component.text("You need OP to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(Component.text("Usage: /setmap <mapname>", NamedTextColor.RED));
            return true;
        }

        if (LobbySystem.gameStarted) {
            sender.sendMessage(Component.text("Cannot switch maps while a game is running! Use /gstop first.", NamedTextColor.RED));
            return true;
        }

        String mapName = args[0];
        File mapFile = new File("plugins/WoolBattle/maps/" + mapName + ".json");
        if (!mapFile.exists()) {
            sender.sendMessage(Component.text("Map \"" + mapName + "\" not found! (plugins/WoolBattle/maps/" + mapName + ".json missing)", NamedTextColor.RED));
            return true;
        }

        boolean success = MapConfig.load(mapName);
        if (!success) {
            sender.sendMessage(Component.text("Failed to load map \"" + mapName + "\". Check the server console.", NamedTextColor.RED));
            return true;
        }

        // Clear in-memory map blocks (they are specific to the old map's world)
        BlockBreakingSystem.setMapBlocks(new java.util.ArrayList<>());

        // Teleport all players to the new lobby spawn
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(MapConfig.lobbyLocation);
            LobbySystem.setLobbyScoreBoard(player);
        }

        Bukkit.broadcast(Component.text("Map switched to ", NamedTextColor.LIGHT_PURPLE).append(Component.text(mapName, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)).append(Component.text("!", NamedTextColor.LIGHT_PURPLE)));
        sender.sendMessage(Component.text("Map \"" + mapName + "\" loaded successfully.", NamedTextColor.GREEN));
        return true;
    }
}
