package me.devupdates.herbalism.hologram;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.crop.Crop;
import me.devupdates.herbalism.manager.CropManager;
import me.devupdates.herbalism.util.MessageUtil;
import me.devupdates.herbalism.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {
    
    private final HerbalismPlugin plugin;
    
    // Hologram storage - one hologram per crop
    private final Map<UUID, ArmorStandHologram> activeHolograms = new ConcurrentHashMap<>();
    
    // Player cooldowns to prevent spam
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    
    // Hologram expiration tracking
    private final Map<UUID, BukkitTask> hologramTasks = new ConcurrentHashMap<>();
    
    // Scheduled tasks
    private BukkitTask updateTask;
    private BukkitTask cleanupTask;
    
    // Performance settings
    private static final long PLAYER_COOLDOWN_MS = 1000; // 1 second cooldown between hologram requests
    private static final int MAX_HOLOGRAMS_PER_PLAYER = 5; // Maximum holograms one player can have active
    
    public HologramManager(HerbalismPlugin plugin) {
        this.plugin = plugin;
        startScheduledTasks();
        MessageUtil.info("Hologram manager initialized with optimized ArmorStand system");
    }
    
    /**
     * Shows a hologram for a crop to a specific player
     */
    public void showCropHologram(Player player, Crop crop) {
        if (!plugin.getConfigManager().isHologramEnabled()) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        UUID cropId = crop.getId();
        
        // Check cooldown to prevent spam
        if (isPlayerOnCooldown(playerId)) {
            MessageUtil.debug("Player " + player.getName() + " is on cooldown for hologram display");
            return;
        }
        
        // Check if player is within range
        if (!isPlayerInRange(player, crop.getLocation())) {
            return;
        }
        
        // Check if player already has too many holograms
        if (getPlayerHologramCount(playerId) >= MAX_HOLOGRAMS_PER_PLAYER) {
            MessageUtil.debug("Player " + player.getName() + " has reached maximum hologram limit");
            return;
        }
        
        // Set cooldown
        playerCooldowns.put(playerId, System.currentTimeMillis() + PLAYER_COOLDOWN_MS);
        
        // Get or create hologram
        ArmorStandHologram hologram = activeHolograms.get(cropId);
        if (hologram == null) {
            hologram = createCropHologram(crop);
            if (hologram != null) {
                activeHolograms.put(cropId, hologram);
            } else {
                return;
            }
        }
        
        // Show to player
        hologram.showToPlayer(player);
        
        // Cancel existing expiration task for this hologram
        BukkitTask existingTask = hologramTasks.get(cropId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Schedule automatic cleanup
        int duration = plugin.getConfigManager().getHologramDuration();
        if (duration > 0) {
            BukkitTask cleanupTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                destroyHologram(cropId);
            }, duration * 20L);
            
            hologramTasks.put(cropId, cleanupTask);
        }
        
        MessageUtil.debug("Hologram shown to player " + player.getName() + " for crop " + cropId);
    }
    
    /**
     * Hides a hologram for a crop from a specific player
     */
    public void hideCropHologram(Player player, Crop crop) {
        UUID cropId = crop.getId();
        ArmorStandHologram hologram = activeHolograms.get(cropId);
        
        if (hologram != null) {
            hologram.hideFromPlayer(player);
            
            // If no players are viewing this hologram, destroy it
            if (hologram.getViewerCount() == 0) {
                destroyHologram(cropId);
            }
        }
    }
    
    /**
     * Hides all holograms for a player
     */
    public void hideAllHolograms(Player player) {
        UUID playerId = player.getUniqueId();
        
        for (Map.Entry<UUID, ArmorStandHologram> entry : activeHolograms.entrySet()) {
            UUID cropId = entry.getKey();
            ArmorStandHologram hologram = entry.getValue();
            
            if (hologram.isVisibleTo(player)) {
                hologram.hideFromPlayer(player);
                
                // If no players are viewing this hologram, destroy it
                if (hologram.getViewerCount() == 0) {
                    destroyHologram(cropId);
                }
            }
        }
    }
    
    /**
     * Updates all active holograms
     */
    public void updateAllHolograms() {
        Iterator<Map.Entry<UUID, ArmorStandHologram>> iterator = activeHolograms.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, ArmorStandHologram> entry = iterator.next();
            UUID cropId = entry.getKey();
            ArmorStandHologram hologram = entry.getValue();
            
            Crop crop = plugin.getCropManager().getCrop(cropId);
            if (crop == null || crop.isHarvested()) {
                // Crop no longer exists, remove hologram
                hologram.destroy();
                iterator.remove();
                
                // Cancel associated task
                BukkitTask task = hologramTasks.remove(cropId);
                if (task != null) {
                    task.cancel();
                }
            } else {
                // Update hologram content
                updateHologramContent(hologram, crop);
            }
        }
    }
    
    /**
     * Destroys a specific hologram
     */
    private void destroyHologram(UUID cropId) {
        ArmorStandHologram hologram = activeHolograms.remove(cropId);
        if (hologram != null) {
            hologram.destroy();
        }
        
        // Cancel associated task
        BukkitTask task = hologramTasks.remove(cropId);
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Creates a hologram display for a crop
     */
    private ArmorStandHologram createCropHologram(Crop crop) {
        try {
            Location location = crop.getLocation().clone().add(0.5, 2.0, 0.5);
            List<String> lines = generateHologramLines(crop);
            return new ArmorStandHologram(location, lines);
        } catch (Exception e) {
            MessageUtil.error("Failed to create hologram display: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Generates hologram lines for a crop
     */
    private List<String> generateHologramLines(Crop crop) {
        List<String> lines = new ArrayList<>();
        
        CropManager.CropType cropType = plugin.getCropManager().getCropType(crop.getCropType());
        if (cropType == null) {
            lines.add("&c&lInvalid Crop");
            return lines;
        }
        
        // Crop name
        lines.add("&2&l" + cropType.getName());
        
        // Owner
        lines.add("&7Owner: &f" + crop.getOwnerName());
        
        // Status
        if (crop.isReadyForHarvest()) {
            lines.add("&a&lREADY FOR HARVEST!");
        } else if (crop.isDecayed()) {
            lines.add("&c&lDECAYED");
        } else {
            long timeLeft = crop.getTimeUntilHarvest();
            lines.add("&7Time left: &e" + TimeUtil.formatTimeMillis(timeLeft));
            
            // Progress bar
            long totalTime = crop.getOriginalDuration();
            long elapsed = totalTime - timeLeft;
            double progress = (double) elapsed / totalTime;
            lines.add(createProgressBar(progress));
        }
        
        // Fertilizer effect
        if (plugin.getFertilizerManager().hasFertilizerEffect(crop)) {
            var effect = plugin.getFertilizerManager().getFertilizerEffect(crop);
            lines.add("&d&lFertilized!");
            lines.add("&7Boost: &d+" + String.format("%.0f%%", (effect.getFertilizer().getGrowthMultiplier() - 1) * 100));
        }
        
        return lines;
    }
    
    /**
     * Updates hologram content
     */
    private void updateHologramContent(ArmorStandHologram hologram, Crop crop) {
        List<String> newLines = generateHologramLines(crop);
        hologram.updateLines(newLines);
    }
    
    /**
     * Creates a progress bar string
     */
    private String createProgressBar(double progress) {
        int barLength = 10;
        int filledLength = (int) (progress * barLength);
        
        StringBuilder bar = new StringBuilder("&8[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("&a█");
            } else {
                bar.append("&7█");
            }
        }
        bar.append("&8]");
        
        return bar.toString();
    }
    
    /**
     * Checks if a player is on cooldown
     */
    private boolean isPlayerOnCooldown(UUID playerId) {
        Long cooldownEnd = playerCooldowns.get(playerId);
        if (cooldownEnd == null) {
            return false;
        }
        
        boolean onCooldown = System.currentTimeMillis() < cooldownEnd;
        if (!onCooldown) {
            playerCooldowns.remove(playerId);
        }
        
        return onCooldown;
    }
    
    /**
     * Gets the number of holograms a player is currently viewing
     */
    private int getPlayerHologramCount(UUID playerId) {
        int count = 0;
        for (ArmorStandHologram hologram : activeHolograms.values()) {
            if (hologram.isVisibleTo(playerId)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Checks if a player is within range of a location
     */
    private boolean isPlayerInRange(Player player, Location location) {
        if (!player.getWorld().equals(location.getWorld())) {
            return false;
        }
        
        int range = plugin.getConfigManager().getHologramRange();
        return player.getLocation().distance(location) <= range;
    }
    
    /**
     * Starts scheduled tasks for hologram management
     */
    private void startScheduledTasks() {
        // Update task - updates hologram content every 5 seconds
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllHolograms();
            }
        }.runTaskTimer(plugin, 100L, 100L); // 5 seconds
        
        // Cleanup task - removes expired cooldowns and performs maintenance
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                performMaintenance();
            }
        }.runTaskTimer(plugin, 600L, 600L); // 30 seconds
    }
    
    /**
     * Performs maintenance tasks
     */
    private void performMaintenance() {
        // Clean up expired cooldowns
        long currentTime = System.currentTimeMillis();
        playerCooldowns.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        
        // Remove holograms without viewers
        Iterator<Map.Entry<UUID, ArmorStandHologram>> iterator = activeHolograms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ArmorStandHologram> entry = iterator.next();
            UUID cropId = entry.getKey();
            ArmorStandHologram hologram = entry.getValue();
            
            if (hologram.getViewerCount() == 0) {
                hologram.destroy();
                iterator.remove();
                
                // Cancel associated task
                BukkitTask task = hologramTasks.remove(cropId);
                if (task != null) {
                    task.cancel();
                }
            }
        }
    }
    
    /**
     * Shuts down the hologram manager
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        
        // Cancel all hologram tasks
        for (BukkitTask task : hologramTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        hologramTasks.clear();
        
        // Destroy all holograms
        for (ArmorStandHologram hologram : activeHolograms.values()) {
            hologram.destroy();
        }
        
        activeHolograms.clear();
        playerCooldowns.clear();
        
        MessageUtil.info("Hologram manager shut down");
    }
    
    /**
     * Reloads hologram configuration
     */
    public void reload() {
        if (!plugin.getConfigManager().isHologramEnabled()) {
            // Disable holograms
            for (ArmorStandHologram hologram : activeHolograms.values()) {
                hologram.destroy();
            }
            activeHolograms.clear();
            
            // Cancel all tasks
            for (BukkitTask task : hologramTasks.values()) {
                if (task != null) {
                    task.cancel();
                }
            }
            hologramTasks.clear();
        }
    }
    
    /**
     * Gets hologram statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", plugin.getConfigManager().isHologramEnabled());
        stats.put("activeHolograms", activeHolograms.size());
        stats.put("scheduledTasks", hologramTasks.size());
        stats.put("playerCooldowns", playerCooldowns.size());
        
        int totalViewers = 0;
        for (ArmorStandHologram hologram : activeHolograms.values()) {
            totalViewers += hologram.getViewerCount();
        }
        stats.put("totalViewers", totalViewers);
        
        return stats;
    }
} 