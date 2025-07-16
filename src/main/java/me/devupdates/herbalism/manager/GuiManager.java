package me.devupdates.herbalism.manager;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.gui.MainMenuGui;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager {
    
    private final HerbalismPlugin plugin;
    private final Map<UUID, Long> lastMenuOpen = new HashMap<>();
    private final long menuCooldown = 1000; // 1 second cooldown
    
    public GuiManager(HerbalismPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Opens the main menu for a player
     */
    public void openMainMenu(Player player) {
        if (!canOpenMenu(player)) {
            plugin.getLanguageManager().sendMessage(player, "gui.cooldown");
            return;
        }
        
        try {
            new MainMenuGui(plugin, player).open();
            lastMenuOpen.put(player.getUniqueId(), System.currentTimeMillis());
            MessageUtil.debug("Opened main menu for player: " + player.getName());
        } catch (Exception e) {
            MessageUtil.error("Error opening main menu for " + player.getName() + ": " + e.getMessage());
            plugin.getLanguageManager().sendMessage(player, "gui.error");
        }
    }
    
    /**
     * Checks if a player can open a menu (cooldown check)
     */
    private boolean canOpenMenu(Player player) {
        Long lastOpen = lastMenuOpen.get(player.getUniqueId());
        if (lastOpen == null) {
            return true;
        }
        
        return System.currentTimeMillis() - lastOpen > menuCooldown;
    }
    
    /**
     * Clears all cooldowns for a player
     */
    public void clearCooldowns(UUID playerId) {
        lastMenuOpen.remove(playerId);
    }
    
    /**
     * Clears all cooldowns (used on plugin disable)
     */
    public void clearAllCooldowns() {
        lastMenuOpen.clear();
    }
    
    /**
     * Gets the remaining cooldown time for a player
     */
    public long getRemainingCooldown(Player player) {
        Long lastOpen = lastMenuOpen.get(player.getUniqueId());
        if (lastOpen == null) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - lastOpen;
        return Math.max(0, menuCooldown - elapsed);
    }
} 