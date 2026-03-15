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

package woolbattle.woolbattle.achievements;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import woolbattle.woolbattle.Main;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class AchievementUI {

    private static ArrayList<String> getPlayerAchievements(Player player, MongoCollection<Document> collection) {
        String playerId = player.getUniqueId().toString();
        Document foundDocument = collection.find(eq("_id", playerId)).first();

        if (foundDocument == null) {
            Document document = new Document("_id", playerId).append("achievements", new ArrayList<String>());
            collection.insertOne(document);
            return new ArrayList<>();
        }

        ArrayList<String> achievements = new ArrayList<>();
        Object rawAchievements = foundDocument.get("achievements");
        if (rawAchievements instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                if (entry instanceof String) {
                    achievements.add((String) entry);
                }
            }
        }

        return achievements;
    }

    /** A function which creates the GUI which contains the Achievements.
     * @param player - A handle to the player object which is passed by a function in LobbySystem. The parameter is the player which has clicked the item in order to open the GUI.
     * @author Beelzebub
     */
    public static void showAchievementGUI(Player player) {
        MongoDatabase db = Main.getMongoDatabase();
        MongoCollection<Document> collection = db.getCollection("playerAchievements");
        ArrayList<String> arrayList = getPlayerAchievements(player, collection);
        Inventory achievements = Bukkit.createInventory(null, 27, Component.text("Achievements", NamedTextColor.GOLD));

        //adding glass
        ItemStack Glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta GlassMeta = Glass.getItemMeta();
        GlassMeta.displayName(Component.text(" "));
        Glass.setItemMeta(GlassMeta);
        for (int i = 0; i <= 26; i++) {
            achievements.setItem(i, Glass);
        }

        //adding fullwool achievement
        Material fullwoolMat = Material.COAL;
        if (arrayList.contains("fullwool")) {fullwoolMat = Material.DIAMOND;}
        ItemStack fullwool = new ItemStack(fullwoolMat);
        ItemMeta fullwoolmeta = fullwool.getItemMeta();
        List<Component> fullwoolLore = new ArrayList<>();

        fullwoolLore.add(Component.text("Have the maximum amount of wool in your inventory", NamedTextColor.WHITE));
        fullwoolLore.add(Component.text(" "));
        if (fullwoolMat == Material.COAL) {
            fullwoolLore.add(Component.text("Not Completed", NamedTextColor.RED));
        }
        else {
            fullwoolLore.add(Component.text("Completed", NamedTextColor.GREEN));
        }
        fullwoolmeta.displayName(Component.text("Strategist", NamedTextColor.GOLD));
        fullwoolmeta.lore(fullwoolLore);
        fullwool.setItemMeta(fullwoolmeta);
        achievements.setItem(13, fullwool);

        //adding Killstreak 5 achievement
        Material killstreak5Mat = Material.COAL;
        if (arrayList.contains("killstreak5")) {killstreak5Mat = Material.DIAMOND;}
        ItemStack Killstreak5 = new ItemStack(killstreak5Mat);
        ItemMeta Killstreak5meta = Killstreak5.getItemMeta();
        List<Component> Killstreak5Lore = new ArrayList<>();

        Killstreak5Lore.add(Component.text("Get a Killstreak of 5 in one game", NamedTextColor.WHITE));
        Killstreak5Lore.add(Component.text(" "));
        if (killstreak5Mat == Material.COAL) {
            Killstreak5Lore.add(Component.text("Not Completed", NamedTextColor.RED));
        }
        else {
            Killstreak5Lore.add(Component.text("Completed", NamedTextColor.GREEN));
        }
        Killstreak5meta.displayName(Component.text("Dominator", NamedTextColor.GOLD));
        Killstreak5meta.lore(Killstreak5Lore);
        Killstreak5.setItemMeta(Killstreak5meta);
        achievements.setItem(14, Killstreak5);

        //adding closeCall achievement
        Material closeCallMat = Material.COAL;
        if (arrayList.contains("closeCall")) {closeCallMat = Material.DIAMOND;}
        ItemStack closeCall = new ItemStack(closeCallMat);
        ItemMeta closeCallMeta = closeCall.getItemMeta();
        List<Component> closeCallLore = new ArrayList<>();

        closeCallLore.add(Component.text("Win a game of Woolbattle while only having a single life left", NamedTextColor.WHITE));
        closeCallLore.add(Component.text(" "));
        if (closeCallMat == Material.COAL) {
            closeCallLore.add(Component.text("Not Completed", NamedTextColor.RED));
        }
        else {
            closeCallLore.add(Component.text("Completed", NamedTextColor.GREEN));
        }
        closeCallMeta.displayName(Component.text("Close Call", NamedTextColor.GOLD));
        closeCallMeta.lore(closeCallLore);
        closeCall.setItemMeta(closeCallMeta);
        achievements.setItem(12, closeCall);

        //adding losing achievement
        Material losingMat = Material.COAL;
        if (arrayList.contains("losing")) {losingMat = Material.DIAMOND;}
        ItemStack losing = new ItemStack(losingMat);
        ItemMeta losingMeta = losing.getItemMeta();
        List<Component> losingLore = new ArrayList<>();

        losingLore.add(Component.text("Lose a game of Woolbattle without having a single Kill", NamedTextColor.WHITE));
        losingLore.add(Component.text(" "));
        if (losingMat == Material.COAL) {
            losingLore.add(Component.text("Not Completed", NamedTextColor.RED));
        }
        else {
            losingLore.add(Component.text("Completed", NamedTextColor.GREEN));
        }
        losingMeta.displayName(Component.text("Losing is the new winning", NamedTextColor.GOLD));
        losingMeta.lore(losingLore);
        losing.setItemMeta(losingMeta);
        achievements.setItem(15, losing);

        //adding carried achievement
        Material carriedMat = Material.COAL;
        if (arrayList.contains("carried")) {carriedMat = Material.DIAMOND;}
        ItemStack carried = new ItemStack(carriedMat);
        ItemMeta carriedMeta = carried.getItemMeta();
        List<Component> carriedLore = new ArrayList<>();

        carriedLore.add(Component.text("Have someone else fight the battle for you - in other words,", NamedTextColor.WHITE));
        carriedLore.add(Component.text("win a game of Woolbattle without having a single kill", NamedTextColor.WHITE));
        carriedLore.add(Component.text(" "));
        if (carriedMat == Material.COAL) {
            carriedLore.add(Component.text("Not Completed", NamedTextColor.RED));
        }
        else {
            carriedLore.add(Component.text("Completed", NamedTextColor.GREEN));
        }
        carriedMeta.displayName(Component.text("The British way", NamedTextColor.GOLD));
        carriedMeta.lore(carriedLore);
        carried.setItemMeta(carriedMeta);
        achievements.setItem(11, carried);

        player.openInventory(achievements);
    }
}
