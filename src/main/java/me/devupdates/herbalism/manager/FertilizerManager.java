package me.devupdates.herbalism.manager;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.crop.Crop;
import me.devupdates.herbalism.fertilizer.Fertilizer;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FertilizerManager {
    private final HerbalismPlugin plugin;
    private final Map<String, Fertilizer> fertilizers;
    private final Map<Location, FertilizerEffect> activeFertilizers;
    
    public FertilizerManager(HerbalismPlugin plugin) {
        this.plugin = plugin;
        this.fertilizers = new HashMap<>();
        this.activeFertilizers = new ConcurrentHashMap<>();
        loadFertilizers();
        startFertilizerTask();
    }
    
    /**
     * Loads fertilizers from fertilizers.yml configuration
     */
    private void loadFertilizers() {
        fertilizers.clear();
        
        FileConfiguration config = plugin.getConfigManager().getFertilizersConfig();
        ConfigurationSection fertilizerSection = config.getConfigurationSection("fertilizers");
        
        if (fertilizerSection == null) {
            MessageUtil.warn("No fertilizers section found in fertilizers.yml");
            return;
        }
        
        for (String id : fertilizerSection.getKeys(false)) {
            ConfigurationSection fertilizerConfig = fertilizerSection.getConfigurationSection(id);
            if (fertilizerConfig != null) {
                try {
                    Fertilizer fertilizer = new Fertilizer(id, fertilizerConfig);
                    fertilizers.put(id, fertilizer);
                    MessageUtil.debug("Loaded fertilizer: " + id);
                } catch (Exception e) {
                    MessageUtil.error("Failed to load fertilizer: " + id + " - " + e.getMessage());
                }
            }
        }
        
        MessageUtil.info("Loaded " + fertilizers.size() + " fertilizers");
    }
    
    /**
     * Gets a fertilizer by ID
     */
    public Fertilizer getFertilizer(String id) {
        return fertilizers.get(id);
    }
    
    /**
     * Gets all registered fertilizers
     */
    public Map<String, Fertilizer> getFertilizers() {
        return new HashMap<>(fertilizers);
    }
    
    /**
     * Attempts to identify a fertilizer from an ItemStack
     */
    public Fertilizer identifyFertilizer(ItemStack item) {
        for (Fertilizer fertilizer : fertilizers.values()) {
            if (fertilizer.matches(item)) {
                return fertilizer;
            }
        }
        return null;
    }
    
    /**
     * Applies fertilizer to a crop
     */
    public boolean applyFertilizer(Crop crop, Fertilizer fertilizer, HerbalismPlayer player) {
        if (crop == null || fertilizer == null) {
            return false;
        }
        
        // Check if player meets level requirement
        if (player.getLevel() < fertilizer.getRequiredLevel()) {
            Map<String, Object> placeholders = new HashMap<>();
            placeholders.put("level", String.valueOf(fertilizer.getRequiredLevel()));
            plugin.getLanguageManager().sendMessage(player.getPlayer(), 
                "messages.fertilizer.level-required", placeholders);
            return false;
        }
        
        // Check success chance
        if (Math.random() > fertilizer.getSuccessChance()) {
            plugin.getLanguageManager().sendMessage(player.getPlayer(), 
                "messages.fertilizer.failed");
            return false;
        }
        
        // Apply fertilizer effect
        Location location = crop.getLocation();
        FertilizerEffect effect = new FertilizerEffect(fertilizer, System.currentTimeMillis());
        
        // Remove existing fertilizer effect if any
        activeFertilizers.remove(location);
        
        // Add new effect
        activeFertilizers.put(location, effect);
        
        // Update crop growth modifier
        crop.setGrowthModifier(fertilizer.getGrowthMultiplier());
        
        // Give experience to player
        player.addExperience(fertilizer.getExperienceGain());
        
        // Send success message
        Map<String, Object> successPlaceholders = new HashMap<>();
        successPlaceholders.put("fertilizer", fertilizer.getName());
        successPlaceholders.put("crop", crop.getCropType());
        plugin.getLanguageManager().sendMessage(player.getPlayer(), 
            "messages.fertilizer.applied", successPlaceholders);
        
        MessageUtil.debug("Applied fertilizer " + fertilizer.getId() + " to crop at " + location);
        return true;
    }
    
    /**
     * Checks if a crop has an active fertilizer effect
     */
    public boolean hasFertilizerEffect(Crop crop) {
        return activeFertilizers.containsKey(crop.getLocation());
    }
    
    /**
     * Gets the active fertilizer effect for a crop
     */
    public FertilizerEffect getFertilizerEffect(Crop crop) {
        return activeFertilizers.get(crop.getLocation());
    }
    
    /**
     * Removes fertilizer effect from a crop
     */
    public void removeFertilizerEffect(Crop crop) {
        Location location = crop.getLocation();
        activeFertilizers.remove(location);
        crop.setGrowthModifier(1.0); // Reset to default
    }
    
    /**
     * Starts the fertilizer effect cleanup task
     */
    private void startFertilizerTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredFertilizers();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }
    
    /**
     * Cleans up expired fertilizer effects
     */
    private void cleanupExpiredFertilizers() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<Location, FertilizerEffect>> iterator = activeFertilizers.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<Location, FertilizerEffect> entry = iterator.next();
            FertilizerEffect effect = entry.getValue();
            
            if (!effect.fertilizer.isPermanent() && effect.isExpired(currentTime)) {
                Location location = entry.getKey();
                iterator.remove();
                
                // Reset crop growth modifier
                Crop crop = plugin.getCropManager().getCropAtLocation(location);
                if (crop != null) {
                    crop.setGrowthModifier(1.0);
                }
                
                MessageUtil.debug("Fertilizer effect expired at " + location);
            }
        }
    }
    
    /**
     * Clears all fertilizer effects (used on plugin disable)
     */
    public void clearAllEffects() {
        activeFertilizers.clear();
    }
    
    /**
     * Reloads fertilizer configuration
     */
    public void reload() {
        loadFertilizers();
    }
    
    /**
     * Inner class to represent an active fertilizer effect
     */
    public static class FertilizerEffect {
        private final Fertilizer fertilizer;
        private final long appliedTime;
        
        public FertilizerEffect(Fertilizer fertilizer, long appliedTime) {
            this.fertilizer = fertilizer;
            this.appliedTime = appliedTime;
        }
        
        public Fertilizer getFertilizer() {
            return fertilizer;
        }
        
        public long getAppliedTime() {
            return appliedTime;
        }
        
        public boolean isExpired(long currentTime) {
            return currentTime - appliedTime > (fertilizer.getDuration() * 1000L);
        }
        
        public long getRemainingTime(long currentTime) {
            long elapsed = currentTime - appliedTime;
            long duration = fertilizer.getDuration() * 1000L;
            return Math.max(0, duration - elapsed);
        }
    }
} 