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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static woolbattle.woolbattle.lobby.LobbySystem.endGame;

public class StopGameCommand implements CommandExecutor {

    /**
     * A Command that calls the endGame() method to stop the game.
     * @author SimsumMC
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if(!player.isOp()){
                player.sendMessage(Component.text("You need OP to use this command!", NamedTextColor.RED));
                return true;
            }
            if(endGame("\u00A7eYellow")){
                player.sendMessage(Component.text("Stopped the game successfully.", NamedTextColor.GREEN));
            } else{
                player.sendMessage(Component.text("There is no running game!", NamedTextColor.RED));
            }
            return true;
        }
        return false;
    }
}