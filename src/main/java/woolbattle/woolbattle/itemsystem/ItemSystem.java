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

package woolbattle.woolbattle.itemsystem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bson.Document;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import woolbattle.woolbattle.WoolHelper;
import woolbattle.woolbattle.Cache;
import woolbattle.woolbattle.Main;
import woolbattle.woolbattle.PlayerDataCache;
import woolbattle.woolbattle.lobby.LobbySystem;
import woolbattle.woolbattle.perks.ActivePerk;
import woolbattle.woolbattle.perks.AllPassivePerks;
import woolbattle.woolbattle.perks.PassivePerk;

import java.util.ArrayList;
import java.util.HashMap;
import static woolbattle.woolbattle.team.TeamSystem.findTeamColor;
import static woolbattle.woolbattle.team.TeamSystem.findTeamDyeColor;

public class ItemSystem {

    //Creates the item-meta-specific values that are known, before a potential player is passed into the giveItems
    // method
    private static final HashMap<Integer, ItemMeta> armorStacks = new HashMap<Integer, ItemMeta>(){
        {
            put(0, new ItemStack(Material.LEATHER_BOOTS).getItemMeta());
            put(1, new ItemStack(Material.LEATHER_LEGGINGS).getItemMeta());
            put(2, new ItemStack(Material.LEATHER_CHESTPLATE).getItemMeta());
            put(3, new ItemStack(Material.LEATHER_HELMET).getItemMeta());
        }
    };
    public static final HashMap<String, Integer> defaultSlots = new HashMap<String, Integer>(){
        {
            put("shears", 0);
            put("bow", 1);
            put("enderpearl", 2);
            put("perk1", 3);
            put("perk2", 4);
        }
    };

    /**
     * Method to add the game's base items, additionally to this the individual player's perk-items into their
     * inventory, colours colour-able fragments of this equipment according to the player's team-colour.
     * @param player The player to be given the items specified above
     * @author Servaturus
     */
    public static void giveItems(Player player){
        Color color = findTeamColor(player);
        PlayerInventory inventory = player.getInventory();

        inventory.clear();

        int shearsSlot;
        int bowSlot;
        int enderPearlSlot;
        int perk1Slot;
        int perk2Slot;
        int passivePerkSlot = 27;

        Document foundDocument = PlayerDataCache.getPlayerInventories(player);
        if(foundDocument == null){
            shearsSlot = defaultSlots.get("shears");
            bowSlot = defaultSlots.get("bow");
            enderPearlSlot = defaultSlots.get("enderpearl");
            perk1Slot = defaultSlots.get("perk1");
            perk2Slot = defaultSlots.get("perk2");
        }
        else{
            shearsSlot = (int) foundDocument.get("shears");
            bowSlot = (int) foundDocument.get("bow");
            enderPearlSlot = (int) foundDocument.get("ender_pearl");
            perk1Slot = (int) foundDocument.get("active_perk1");
            perk2Slot = (int) foundDocument.get("active_perk2");
        }

        ItemStack shears = Cache.getActivePerks().get("Shears").getItemStack();

        inventory.setItem(shearsSlot, shears);

        ItemStack bow = Cache.getActivePerks().get("Bow").getItemStack();

        inventory.setItem(bowSlot, bow);

        ItemStack enderPearl = Cache.getActivePerks().get("Ender Pearl").getItemStack();

        inventory.setItem(enderPearlSlot, enderPearl);

        Document perksDocument = PlayerDataCache.getPlayerPerks(player);

        if(perksDocument != null) {

            String activePerk1String = null;
            String activePerk2String = null;
            String passivePerkString = null;

            if (perksDocument.get("first_active") != null){
                activePerk1String = (String) perksDocument.get("first_active");
            }

            if (perksDocument.get("second_active") != null){
                activePerk2String = (String) perksDocument.get("second_active");
            }

            if (perksDocument.get("passive") != null){
                passivePerkString = (String) perksDocument.get("passive");
            }

            if(activePerk1String != null) {

                ActivePerk activePerk1 = Cache.getActivePerks().get(activePerk1String);
                if (activePerk1 != null) {
                    inventory.setItem(perk1Slot, activePerk1.getItemStack());
                }
            }

            if(activePerk2String != null) {
                ActivePerk activePerk2 = Cache.getActivePerks().get(activePerk2String);
                if (activePerk2 != null) {
                    inventory.setItem(perk2Slot, activePerk2.getItemStack());
                }
            }

            if(passivePerkString != null) {
                PassivePerk<? extends Event,?> passivePerk = Cache.getPassivePerks().get(passivePerkString);
                if (passivePerk != null) {
                    inventory.setItem(passivePerkSlot, passivePerk.getItem());
                }
            }

        }

        for(Integer index : armorStacks.keySet()){
            LeatherArmorMeta meta = (LeatherArmorMeta) armorStacks.get(index);
            meta.setColor(color);

            Material armorMaterial;
            switch(index){
                case 0: armorMaterial = Material.LEATHER_BOOTS; meta.displayName(Component.text("Leather Boots", NamedTextColor.AQUA)); break;
                case 1: armorMaterial = Material.LEATHER_LEGGINGS; meta.displayName(Component.text("Leather Leggings", NamedTextColor.AQUA)); break;
                case 2: armorMaterial = Material.LEATHER_CHESTPLATE; meta.displayName(Component.text("Leather Chestplate", NamedTextColor.AQUA)); break;
                case 3: armorMaterial = Material.LEATHER_HELMET; meta.displayName(Component.text("Leather Helmet", NamedTextColor.AQUA)); break;
                default: armorMaterial = Material.LEATHER_BOOTS; break;
            }
            meta.setUnbreakable(true);
            ItemStack armorPiece = new ItemStack(armorMaterial, 1);
            armorPiece.setItemMeta(meta);
            armorPiece.setAmount(1);
            switch(index){
                case 0:
                    inventory.setBoots(armorPiece);
                    break;
                case 1:
                    inventory.setLeggings(armorPiece);
                    break;
                case 2:
                    inventory.setChestplate(armorPiece);
                    break;
                case 3:
                    inventory.setHelmet(armorPiece);
                    break;

            }

        }
        inventory.setItem(9, new ItemStack(Material.ARROW));

        player.getInventory().setContents(inventory.getContents());
    }

    /**
     * Method that subtracts wool from a players inventory.
     * @author Servaturus
     * @param player The player, to remove the wool from
     * @param subtractWool The wool subtractWool to subtract
     * @author Servaturus & SimsumMC
     */
    public static boolean subtractWool(Player player, int subtractWool){

        PlayerInventory inv = player.getInventory();

        int existingWoolAmount = 0;

        for(ItemStack iterStack : inv.getContents()){
            if(iterStack != null && WoolHelper.isWool(iterStack.getType())){
                existingWoolAmount += iterStack.getAmount();
            }
        }

        if(existingWoolAmount - subtractWool < 0){
            return false;
        }

        if(subtractWool > 0 && AllPassivePerks.shouldSkipActivePerkWoolCost(player)) {
            return true;
        }

        int woolAmount = 0;

        ArrayList<ItemStack> woolStacks = new ArrayList<>();
        DyeColor color = findTeamDyeColor(player);

        for(ItemStack is : inv.getContents()){
            if(is != null && WoolHelper.isWool(is.getType())){
                woolAmount += is.getAmount();
            }
        }
        if(woolAmount - subtractWool ==0){
            WoolHelper.removeAllWool(player.getInventory());
            return true;
        }
        woolAmount = woolAmount - subtractWool;

        int modulo = woolAmount%64;

        WoolHelper.removeAllWool(inv);

        ItemStack woolInstance = new ItemStack(WoolHelper.getWoolMaterial(color));

        if(woolAmount%64 !=0){
            ItemStack wool = new ItemStack(woolInstance);
            wool.setAmount(modulo);
            woolStacks.add(new ItemStack(wool));
            woolAmount-=(woolAmount%64);

        }

        for(int i = 0; i<woolAmount/64;i++){
            ItemStack wool = new ItemStack(woolInstance);
            wool.setAmount(64);
            woolStacks.add(wool);
        }

        for(ItemStack woolStack : woolStacks){
            inv.addItem(woolStack);
        }
        return true;

    }

    /**
     * Method that replaces the specified itemSlot with a gunpowder item-stack, lowering its amount every second by one,
     * until the specified cool-down has run out, which makes it replace said slot with the original item, illustrated by
     * itemStack passed in.
     * @param p The player, to add a cool-down to one of their items.
     * @param slot The slot, to condone the cool-down in.
     * @param cooldownInSeconds The cool-down's duration.
     * @param item The item, to replace the specified slot with after the cool-down.
     * @author Servaturus & SimsumMC
     */
    public static void setItemCooldown(Player p, int slot, ItemStack item, int cooldownInSeconds){
        ItemStack expiredItem = new ItemStack(Material.GUNPOWDER){
            {
                ItemMeta meta = getItemMeta();
                meta.displayName(Component.text("Item on Cooldown", NamedTextColor.RED));
                setItemMeta(meta);
            }
        };

        new BukkitRunnable(){

            int loops = 0;

            @Override
            public void run() {
                if(!LobbySystem.gameStarted){
                    this.cancel();
                    return;
                }

                if(loops == cooldownInSeconds){
                    if(item.getAmount() < 1){
                        item.setAmount(item.getAmount() + 1);
                    }
                    p.getInventory().setItem(slot, item);
                    this.cancel();
                }else{
                    expiredItem.setAmount((cooldownInSeconds - loops));
                    p.getInventory().setItem(slot,expiredItem);
                    loops++;

                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
}
