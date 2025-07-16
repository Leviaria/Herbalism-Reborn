package me.devupdates.herbalism.gui;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GuiBase implements Listener {
    
    protected final HerbalismPlugin plugin;
    protected final Player player;
    protected final HerbalismPlayer herbalismPlayer;
    protected final Inventory inventory;
    protected final Map<Integer, GuiAction> actions = new HashMap<>();
    protected final String title;
    protected final int size;
    
    public GuiBase(HerbalismPlugin plugin, Player player, String title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.herbalismPlayer = plugin.getPlayerManager().getPlayer(player);
        this.title = MessageUtil.colorize(title);
        this.size = size;
        this.inventory = Bukkit.createInventory(null, size, this.title);
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Opens the GUI for the player
     */
    public void open() {
        setupInventory();
        player.openInventory(inventory);
    }
    
    /**
     * Sets up the inventory with items - to be implemented by subclasses
     */
    protected abstract void setupInventory();
    
    /**
     * Creates an item with display name and lore
     */
    protected ItemStack createItem(Material material, String displayName, List<String> lore) {
        // Fallback to BARRIER if material is invalid
        if (material == null || !material.isItem()) {
            material = Material.BARRIER;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(displayName));
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(MessageUtil.colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Creates an item with display name and lore (varargs)
     */
    protected ItemStack createItem(Material material, String displayName, String... loreLines) {
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(line);
        }
        return createItem(material, displayName, lore);
    }
    
    /**
     * Creates an item with display name and lore from string material
     */
    protected ItemStack createItem(String materialName, String displayName, List<String> lore) {
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            MessageUtil.warning("Invalid material: " + materialName + ", using BARRIER instead");
            material = Material.BARRIER;
        }
        return createItem(material, displayName, lore);
    }
    
    /**
     * Creates a placeholder item (glass pane)
     */
    protected ItemStack createPlaceholder() {
        return createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }
    
    /**
     * Creates a back button
     */
    protected ItemStack createBackButton() {
        return createItem(Material.ARROW, "&c&lBack", "&7Click to go back");
    }
    
    /**
     * Creates a close button
     */
    protected ItemStack createCloseButton() {
        return createItem(Material.BARRIER, "&c&lClose", "&7Click to close this menu");
    }
    
    /**
     * Creates a next page button
     */
    protected ItemStack createNextPageButton() {
        return createItem(Material.SPECTRAL_ARROW, "&a&lNext Page", "&7Click to go to next page");
    }
    
    /**
     * Creates a previous page button
     */
    protected ItemStack createPrevPageButton() {
        return createItem(Material.SPECTRAL_ARROW, "&a&lPrevious Page", "&7Click to go to previous page");
    }
    
    /**
     * Sets an item at a specific slot with an action
     */
    protected void setItem(int slot, ItemStack item, GuiAction action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }
    
    /**
     * Sets an item at a specific slot without an action
     */
    protected void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }
    
    /**
     * Fills empty slots with placeholder items
     */
    protected void fillEmptySlots() {
        ItemStack placeholder = createPlaceholder();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, placeholder);
            }
        }
    }
    
    /**
     * Fills specific slots with placeholder items
     */
    protected void fillSlots(int... slots) {
        ItemStack placeholder = createPlaceholder();
        for (int slot : slots) {
            inventory.setItem(slot, placeholder);
        }
    }
    
    /**
     * Fills a border around the inventory
     */
    protected void fillBorder() {
        ItemStack placeholder = createPlaceholder();
        int rows = size / 9;
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, placeholder);
            inventory.setItem(size - 9 + i, placeholder);
        }
        
        // Left and right columns
        for (int i = 1; i < rows - 1; i++) {
            inventory.setItem(i * 9, placeholder);
            inventory.setItem(i * 9 + 8, placeholder);
        }
    }
    
    /**
     * Refreshes the inventory
     */
    public void refresh() {
        inventory.clear();
        actions.clear();
        setupInventory();
    }
    
    /**
     * Handles inventory click events
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().equals(inventory)) {
            event.setCancelled(true);
            
            if (event.getWhoClicked() instanceof Player) {
                Player clickedPlayer = (Player) event.getWhoClicked();
                
                if (!clickedPlayer.equals(player)) {
                    return;
                }
                
                int slot = event.getRawSlot();
                GuiAction action = actions.get(slot);
                
                if (action != null) {
                    try {
                        action.execute(clickedPlayer, event.getClick());
                    } catch (Exception e) {
                        MessageUtil.error("Error executing GUI action: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * Handles inventory close events
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory)) {
            // Unregister event listener
            InventoryClickEvent.getHandlerList().unregister(this);
            InventoryCloseEvent.getHandlerList().unregister(this);
        }
    }
    
    /**
     * Creates a progress bar item
     */
    protected ItemStack createProgressItem(Material material, String name, double progress, String progressText) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Progress: " + progressText);
        lore.add("");
        lore.add(createProgressBar(progress));
        
        return createItem(material, name, lore);
    }
    
    /**
     * Creates a progress bar string
     */
    protected String createProgressBar(double progress) {
        int barLength = 20;
        int filledLength = (int) (progress * barLength);
        
        StringBuilder bar = new StringBuilder("&8[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("&a█");
            } else {
                bar.append("&7█");
            }
        }
        bar.append("&8] &f").append(String.format("%.1f%%", progress * 100));
        
        return bar.toString();
    }
    
    /**
     * Formats a number with suffixes (K, M, B, etc.)
     */
    protected String formatNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1000000) return String.format("%.1fK", number / 1000.0);
        if (number < 1000000000) return String.format("%.1fM", number / 1000000.0);
        return String.format("%.1fB", number / 1000000000.0);
    }
    
    /**
     * Functional interface for GUI actions
     */
    @FunctionalInterface
    public interface GuiAction {
        void execute(Player player, org.bukkit.event.inventory.ClickType clickType);
    }
} 