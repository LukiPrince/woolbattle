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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import woolbattle.woolbattle.Cache;
import woolbattle.woolbattle.PlayerDataCache;
import woolbattle.woolbattle.stats.StatsSystem;

import java.util.Collection;
import java.util.HashMap;
import static woolbattle.woolbattle.itemsystem.ItemSystem.*;

public class ActivePerk {
    private ItemStack itemStack;
    private String itemName;
    private String description = "No description provided.";
    private final boolean useOnExecute;
    private final boolean selectable;
    private final int cooldown;
    private final int woolCost;

    public ActivePerk(ItemStack itemStack, int cooldown, int woolCost, boolean useOnExecute, boolean selectable) {
        this.itemStack = itemStack;
        Component displayName = itemStack.getItemMeta().displayName();
        this.itemName = displayName != null ? PlainTextComponentSerializer.plainText().serialize(displayName) : "";
        this.useOnExecute = useOnExecute;
        this.cooldown = cooldown;
        this.woolCost = woolCost;
        this.selectable = selectable;
    }

    public ActivePerk(ItemStack itemStack, int cooldown, int woolCost, boolean useOnExecute) {
        this.itemStack = itemStack;
        Component displayName = itemStack.getItemMeta().displayName();
        this.itemName = displayName != null ? PlainTextComponentSerializer.plainText().serialize(displayName) : "";
        this.useOnExecute = useOnExecute;
        this.cooldown = cooldown;
        this.woolCost = woolCost;
        this.selectable = true;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack newItemStack) {
        itemStack = newItemStack;
    }

    public int getWoolCost() {
        return woolCost;
    }

    public int getCooldown() {
        return cooldown;
    }

    public boolean getSelectableStatus(){
        return selectable;
    }

    public String getDescription() {
        return description;
    }

    public ActivePerk setDescription(String description) {
        this.description = description;
        return this;
    }

    public ActivePerk setItemName(Component name){
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(name);
        itemStack.setItemMeta(itemMeta);
        itemName = PlainTextComponentSerializer.plainText().serialize(name);
        return this;
    }

    public ActivePerk setCustomModelData(Integer customModelData){
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setCustomModelData(customModelData);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ActivePerk addEnchantment(Enchantment enchantment, int level, boolean invisible){
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addEnchant(enchantment, level, true);

        if(invisible){
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        itemStack.setItemMeta(itemMeta);

        return this;
    }

    public ActivePerk addEnchantment(Enchantment enchantment, boolean invisible){
        return addEnchantment(enchantment,1, invisible);
    }

    public void register(){
        HashMap<String, ActivePerk> activePerks = Cache.getActivePerks();
        activePerks.put(itemName, this);
        Cache.setActivePerks(activePerks);
    }

    public void execute(PlayerInteractEvent event, Player player){
        if(AllActivePerks.denyPerkUseIfSilenced(player)){
            event.setCancelled(true);
            return;
        }

        if(AllActivePerks.denyPerkUseByUltimateStates(player, this.itemName)){
            event.setCancelled(true);
            return;
        }

        if(!useOnExecute){
            return;
        }

        int slot = getSlotCache(player);

        int effectiveWoolCost = AllActivePerks.applyUltimateWoolCostModifier(player, woolCost);
        if(!subtractWool(player, effectiveWoolCost)){
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.4f);
            return;
        }

        int effectiveCooldown = AllActivePerks.applyUltimateCooldownModifier(player, cooldown);
        if(effectiveCooldown != 0){
            setItemCooldown(player, slot, itemStack, effectiveCooldown);
        }

        onExecute(event, player);

        StatsSystem.addActivePerkUsage(player);
    }

    public void onExecute(PlayerInteractEvent event, Player player) {}

    /**
     * A Method that returns the Slot of the Active Perk. -> normally from the cache
     * @param player - the player of the perk
     * @author SimsumMC
     */
    public int getSlotCache(Player player) {
        String activePerkName = this.itemName;

        HashMap<Player, HashMap<String, Integer>> activePerkSlots = Cache.getActivePerkSlots();

        if(!activePerkSlots.containsKey(player) || !activePerkSlots.get(player).containsKey(activePerkName)){
            return getSlotDB(player);
        }

        return activePerkSlots.get(player).get(activePerkName);
    }

    /**
     * A Method that returns the Slot of the Active Perk. -> directly from the database
     * @param player - the player of the perk
     * @author SimsumMC
     */
    private int getSlotDB(Player player){
        String activePerkName = this.itemName;
        Document foundDocument = PlayerDataCache.getPlayerInventories(player);

        int shearsSlot;
        int bowSlot;
        int enderPearlSlot;
        int perk1Slot;
        int perk2Slot;

        if(foundDocument == null){
            shearsSlot = defaultSlots.get("shears");
            bowSlot = defaultSlots.get("bow");
            enderPearlSlot = defaultSlots.get("enderpearl");
            perk1Slot = defaultSlots.get("perk1");
            perk2Slot = defaultSlots.get("perk2");
        }
        else{
            if(foundDocument.get("active_perk1") instanceof Integer){
                perk1Slot = (int) foundDocument.get("active_perk1");
            }
            else {
                perk1Slot = defaultSlots.get("perk1");
            }

            if(foundDocument.get("active_perk2") instanceof Integer){
                perk2Slot = (int) foundDocument.get("active_perk2");
            }
            else {
                perk2Slot = defaultSlots.get("perk2");
            }

            shearsSlot = (int) foundDocument.get("shears");
            bowSlot = (int) foundDocument.get("bow");
            enderPearlSlot = (int) foundDocument.get("ender_pearl");
        }

        if(!this.selectable){
            if(activePerkName.equals("Shears")){
                return shearsSlot;
            }
            if(activePerkName.equals("Bow")){
                return bowSlot;
            }
            if(activePerkName.equals("Ender Pearl")){
                return enderPearlSlot;
            }
        }

        Document perksDocument = PlayerDataCache.getPlayerPerks(player);

        if(perksDocument != null) {

            String activePerk1String;
            String activePerk2String;

            if (perksDocument.get("first_active") != null){
                activePerk1String = (String) perksDocument.get("first_active");
                if(activePerk1String.equals(activePerkName)) {
                    return perk1Slot;
                }
            }

            if (perksDocument.get("second_active") != null){
                activePerk2String = (String) perksDocument.get("second_active");
                if(activePerk2String.equals(activePerkName)) {
                    return perk2Slot;
                }
            }
        }
        return perk1Slot;
    }

    /**
     * A Method that puts all the slots from all active perks for every player in a HashMap, in the so-called "Cache"
     * to reduce database calls.
     * @author SimsumMC
     */
    public static void loadActivePerkSlots(){
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

        for(Player player : onlinePlayers){
            Document foundDocument = PlayerDataCache.getPlayerInventories(player);

            int shearsSlot;
            int bowSlot;
            int enderPearlSlot;
            int perk1Slot;
            int perk2Slot;

            if(foundDocument == null){
                shearsSlot = defaultSlots.get("shears");
                bowSlot = defaultSlots.get("bow");
                enderPearlSlot = defaultSlots.get("enderpearl");
                perk1Slot = defaultSlots.get("perk1");
                perk2Slot = defaultSlots.get("perk2");
            }
            else{
                if(foundDocument.get("active_perk1") instanceof Integer){
                    perk1Slot = (int) foundDocument.get("active_perk1");
                }
                else {
                    perk1Slot = defaultSlots.get("perk1");
                }

                if(foundDocument.get("active_perk2") instanceof Integer){
                    perk2Slot = (int) foundDocument.get("active_perk2");
                }
                else {
                    perk2Slot = defaultSlots.get("perk2");
                }

                shearsSlot = (int) foundDocument.get("shears");
                bowSlot = (int) foundDocument.get("bow");
                enderPearlSlot = (int) foundDocument.get("ender_pearl");
            }

            HashMap<String, Integer> playerSlots = new HashMap<>();

            playerSlots.put("Shears", shearsSlot);
            playerSlots.put("Bow", bowSlot);
            playerSlots.put("Ender Pearl", enderPearlSlot);

            Document perksDocument = PlayerDataCache.getPlayerPerks(player);

            if(perksDocument != null) {

                String activePerk1String;
                String activePerk2String;


                if (perksDocument.get("first_active") != null){
                    activePerk1String = (String) perksDocument.get("first_active");
                    playerSlots.put(activePerk1String, perk1Slot);
                }

                if (perksDocument.get("second_active") != null){
                    activePerk2String = (String) perksDocument.get("second_active");
                    playerSlots.put(activePerk2String, perk2Slot);
                }

            }

            HashMap<Player, HashMap<String, Integer>> activePerkSlots = Cache.getActivePerkSlots();
            activePerkSlots.put(player, playerSlots);
            Cache.setActivePerkSlots(activePerkSlots);
        }
    }


}
