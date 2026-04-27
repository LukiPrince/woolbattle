package woolbattle.woolbattle.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import woolbattle.woolbattle.Cache;
import woolbattle.woolbattle.Enums.PerkType;
import woolbattle.woolbattle.Main;
import woolbattle.woolbattle.perks.ActivePerk;
import woolbattle.woolbattle.perks.AllActivePerks;

import java.util.ArrayList;
import java.util.List;

public class AnimatedPerkGUI implements InventoryHolder, Listener {

    private final Player player;
    private final Inventory inventory;
    private MenuTab currentTab = MenuTab.ACTIVE_1;
    private BukkitTask animationTask;
    private int tick = 0;

    private static final int[] BORDER_SLOTS = {
            0, 1, 7, 8,
            9, 17,
            18, 26,
            27, 35,
            36, 44,
            45, 46, 47, 48, 50, 51, 52, 53
    };

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public enum MenuTab {
        ACTIVE_1(2, "Active Perk #1", Material.IRON_SWORD, PerkType.FIRST_ACTIVE),
        ACTIVE_2(3, "Active Perk #2", Material.CROSSBOW, PerkType.SECOND_ACTIVE),
        PASSIVE(4, "Passive Perk", Material.ENDER_CHEST, PerkType.PASSIVE),
        ULTIMATE(5, "Ultimate", Material.NETHER_STAR, PerkType.ULTIMATE);

        public final int slot;
        public final String title;
        public final Material material;
        public final PerkType perkType;

        MenuTab(int slot, String title, Material material, PerkType perkType) {
            this.slot = slot;
            this.title = title;
            this.material = material;
            this.perkType = perkType;
        }
    }

    public AnimatedPerkGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("Moderne Perks", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
        renderTabs();
        renderContent();
        
        // Close item
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(Component.text("Close Menu", NamedTextColor.RED, TextDecoration.BOLD));
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(49, closeItem);

        startAnimation();
    }

    private void startAnimation() {
        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory() != inventory) {
                    cancel();
                    return;
                }

                Material[] borderMaterials = {
                        Material.MAGENTA_STAINED_GLASS_PANE,
                        Material.PURPLE_STAINED_GLASS_PANE,
                        Material.PINK_STAINED_GLASS_PANE,
                        Material.BLACK_STAINED_GLASS_PANE
                };

                Material currentMat = borderMaterials[(tick / 5) % borderMaterials.length];
                ItemStack glass = new ItemStack(currentMat);
                ItemMeta meta = glass.getItemMeta();
                meta.displayName(Component.text(" "));
                glass.setItemMeta(meta);

                for (int slot : BORDER_SLOTS) {
                    inventory.setItem(slot, glass);
                }
                
                tick++;
            }
        }.runTaskTimer(Main.getInstance(), 0L, 2L);
    }

    private void renderTabs() {
        for (MenuTab tab : MenuTab.values()) {
            ItemStack item = new ItemStack(tab.material);
            ItemMeta meta = item.getItemMeta();
            
            boolean isSelected = (tab == currentTab);
            NamedTextColor color = isSelected ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            
            meta.displayName(Component.text(tab.title, color, TextDecoration.BOLD));
            
            if (isSelected) {
                meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(" "));
            if (isSelected) {
                lore.add(Component.text("Currently Viewing", NamedTextColor.GREEN));
            } else {
                lore.add(Component.text("Click to view", NamedTextColor.YELLOW));
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            
            item.setItemMeta(meta);
            inventory.setItem(tab.slot, item);
        }
    }

    private void renderContent() {
        for (int slot : CONTENT_SLOTS) {
            inventory.setItem(slot, null);
        }

        String selectedPerk = getSelectedPerkForTab(currentTab);
        int index = 0;

        if (currentTab == MenuTab.ACTIVE_1 || currentTab == MenuTab.ACTIVE_2) {
            for (ActivePerk perk : Cache.getActivePerks().values()) {
                if (!perk.getSelectableStatus()) continue;
                if (index >= CONTENT_SLOTS.length) break;

                ItemStack item = perk.getItemStack().clone();
                ItemMeta meta = item.getItemMeta();
                String internalName = plainName(meta);
                
                boolean isSelected = internalName.equals(selectedPerk);
                
                meta.displayName(Component.text(LobbySystem.translatePerkName(internalName), NamedTextColor.AQUA, TextDecoration.BOLD));
                
                List<Component> lore = new ArrayList<>(LobbySystem.buildPerkDescriptionLore(perk.getDescription()));
                lore.add(Component.text(" "));
                lore.add(Component.text("Wool: ", NamedTextColor.GOLD).append(Component.text(perk.getWoolCost(), NamedTextColor.WHITE)));
                lore.add(Component.text("Cooldown: ", NamedTextColor.GOLD).append(Component.text(perk.getCooldown() + "s", NamedTextColor.WHITE)));
                lore.add(Component.text(" "));
                
                if (isSelected) {
                    meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    lore.add(Component.text("▶ SELECTED", NamedTextColor.GREEN, TextDecoration.BOLD));
                } else {
                    lore.add(Component.text("Click to select", NamedTextColor.YELLOW));
                }
                
                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
                
                inventory.setItem(CONTENT_SLOTS[index++], item);
            }
        } else if (currentTab == MenuTab.ULTIMATE) {
            for (AllActivePerks.UltimateDefinition def : AllActivePerks.getUltimateDefinitions().values()) {
                if (index >= CONTENT_SLOTS.length) break;
                
                ItemStack item = new ItemStack(def.getIconMaterial());
                ItemMeta meta = item.getItemMeta();
                
                boolean isSelected = def.getDisplayName().equals(selectedPerk);
                
                meta.displayName(Component.text(LobbySystem.translatePerkName(def.getDisplayName()), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
                
                List<Component> lore = new ArrayList<>(LobbySystem.buildPerkDescriptionLore(def.getDescription()));
                lore.add(Component.text(" "));
                
                if (isSelected) {
                    meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    lore.add(Component.text("▶ SELECTED", NamedTextColor.GREEN, TextDecoration.BOLD));
                } else {
                    lore.add(Component.text("Click to select", NamedTextColor.YELLOW));
                }
                
                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
                
                inventory.setItem(CONTENT_SLOTS[index++], item);
            }
        } else if (currentTab == MenuTab.PASSIVE) {
            for (woolbattle.woolbattle.perks.PassivePerk<?, ?> perk : Cache.getPassivePerks().values()) {
                if (index >= CONTENT_SLOTS.length) break;
                
                ItemStack item = perk.getItem().clone();
                ItemMeta meta = item.getItemMeta();
                
                String internalName = perk.getName();
                boolean isSelected = internalName.equals(selectedPerk);
                
                meta.displayName(Component.text(LobbySystem.translatePerkName(internalName), NamedTextColor.GOLD, TextDecoration.BOLD));
                
                List<Component> lore = new ArrayList<>(LobbySystem.buildPerkDescriptionLore(perk.getDescription()));
                lore.add(Component.text(" "));
                
                if (isSelected) {
                    meta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    lore.add(Component.text("▶ SELECTED", NamedTextColor.GREEN, TextDecoration.BOLD));
                } else {
                    lore.add(Component.text("Click to select", NamedTextColor.YELLOW));
                }
                
                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
                
                inventory.setItem(CONTENT_SLOTS[index++], item);
            }
        }
    }

    private String getSelectedPerkForTab(MenuTab tab) {
        if (tab == MenuTab.ACTIVE_1) {
            org.bson.Document doc = woolbattle.woolbattle.PlayerDataCache.getPlayerPerks(player);
            if (doc != null && doc.get("first_active") instanceof String) return (String) doc.get("first_active");
            return null;
        } else if (tab == MenuTab.ACTIVE_2) {
            org.bson.Document doc = woolbattle.woolbattle.PlayerDataCache.getPlayerPerks(player);
            if (doc != null && doc.get("second_active") instanceof String) return (String) doc.get("second_active");
            return null;
        } else if (tab == MenuTab.PASSIVE) {
            return LobbySystem.getSelectedPassivePerk(player);
        } else {
            return LobbySystem.getSelectedUltimate(player);
        }
    }

    private String plainName(ItemMeta meta) {
        Component display = meta.displayName();
        return display != null ? PlainTextComponentSerializer.plainText().serialize(display) : "";
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        int slot = event.getRawSlot();
        
        if (slot == 49) {
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1f);
            return;
        }

        for (MenuTab tab : MenuTab.values()) {
            if (slot == tab.slot) {
                if (currentTab != tab) {
                    currentTab = tab;
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                    renderTabs();
                    
                    // Simple clear effect before rendering content
                    for (int cSlot : CONTENT_SLOTS) {
                        inventory.setItem(cSlot, null);
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            renderContent();
                            p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.2f);
                        }
                    }.runTaskLater(Main.getInstance(), 2L);
                }
                return;
            }
        }

        // Handle perk selection
        boolean isContentSlot = false;
        for (int cSlot : CONTENT_SLOTS) {
            if (slot == cSlot) {
                isContentSlot = true;
                break;
            }
        }

        if (isContentSlot) {
            String clickedName = plainName(clicked.getItemMeta());
            // It might have "▶ SELECTED" text, but we use the display name which is the translated perk name
            String internalName = LobbySystem.toInternalPerkName(clickedName);
            
            LobbySystem.savePerkSelection(p, internalName, currentTab.perkType);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            renderContent();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
            if (animationTask != null) {
                animationTask.cancel();
            }
        }
    }
}
