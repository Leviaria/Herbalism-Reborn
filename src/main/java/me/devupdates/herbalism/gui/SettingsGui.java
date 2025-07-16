package me.devupdates.herbalism.gui;

import me.devupdates.herbalism.core.HerbalismPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SettingsGui extends GuiBase {
    
    public SettingsGui(HerbalismPlugin plugin, Player player) {
        super(plugin, player, "&2&lSettings", 45);
    }
    
    @Override
    protected void setupInventory() {
        // Fill border
        fillBorder();
        
        // Back button
        setItem(36, createBackButton(), (p, click) -> new MainMenuGui(plugin, p).open());
        
        // Close button
        setItem(44, createCloseButton(), (p, click) -> p.closeInventory());
        
        // Settings items
        setupSettingsItems();
        
        // Info item
        setItem(4, createInfoItem());
    }
    
    private void setupSettingsItems() {
        // Notification settings
        setItem(19, createNotificationSettings());
        
        // Language settings
        setItem(21, createLanguageSettings());
        
        // Display settings
        setItem(23, createDisplaySettings());
        
        // Sound settings
        setItem(25, createSoundSettings());
    }
    
    private ItemStack createNotificationSettings() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Configure notification preferences");
        lore.add("");
        lore.add("&7Current Settings:");
        lore.add("&8• &7Crop Ready Notifications: &aEnabled");
        lore.add("&8• &7Level Up Notifications: &aEnabled");
        lore.add("&8• &7Ability Unlock Notifications: &aEnabled");
        lore.add("&8• &7Fertilizer Notifications: &aEnabled");
        lore.add("");
        lore.add("&c&lComing Soon!");
        lore.add("&7Click to configure notifications");
        
        return createItem(Material.BELL, "&2&lNotifications", lore);
    }
    
    private ItemStack createLanguageSettings() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Configure language preferences");
        lore.add("");
        lore.add("&7Current Language: &aEnglish");
        lore.add("");
        lore.add("&7Available Languages:");
        lore.add("&8• &aEnglish &7(Default)");
        lore.add("&8• &7German &8(Coming Soon)");
        lore.add("&8• &7Spanish &8(Coming Soon)");
        lore.add("&8• &7French &8(Coming Soon)");
        lore.add("");
        lore.add("&c&lComing Soon!");
        lore.add("&7Click to change language");
        
        return createItem(Material.BOOK, "&2&lLanguage", lore);
    }
    
    private ItemStack createDisplaySettings() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Configure display preferences");
        lore.add("");
        lore.add("&7Current Settings:");
        lore.add("&8• &7Action Bar Messages: &aEnabled");
        lore.add("&8• &7Chat Messages: &aEnabled");
        lore.add("&8• &7Title Messages: &aEnabled");
        lore.add("&8• &7Particle Effects: &aEnabled");
        lore.add("&8• &7Hologram Display: &aEnabled");
        lore.add("");
        lore.add("&c&lComing Soon!");
        lore.add("&7Click to configure display");
        
        return createItem(Material.PAINTING, "&2&lDisplay", lore);
    }
    
    private ItemStack createSoundSettings() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Configure sound preferences");
        lore.add("");
        lore.add("&7Current Settings:");
        lore.add("&8• &7Planting Sounds: &aEnabled");
        lore.add("&8• &7Harvesting Sounds: &aEnabled");
        lore.add("&8• &7Level Up Sounds: &aEnabled");
        lore.add("&8• &7Fertilizer Sounds: &aEnabled");
        lore.add("&8• &7Notification Sounds: &aEnabled");
        lore.add("");
        lore.add("&c&lComing Soon!");
        lore.add("&7Click to configure sounds");
        
        return createItem(Material.NOTE_BLOCK, "&2&lSounds", lore);
    }
    
    private ItemStack createInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Customize your Herbalism experience");
        lore.add("");
        lore.add("&7Configure notifications, language,");
        lore.add("&7display options, and sounds to");
        lore.add("&7make farming perfect for you!");
        lore.add("");
        lore.add("&7Player: &f" + player.getName());
        lore.add("&7Level: &a" + herbalismPlayer.getLevel());
        
        return createItem(Material.COMPARATOR, "&2&lSettings Overview", lore);
    }
} 