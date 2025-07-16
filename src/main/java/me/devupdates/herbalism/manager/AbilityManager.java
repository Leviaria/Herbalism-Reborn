package me.devupdates.herbalism.manager;

import me.devupdates.herbalism.ability.Ability;
import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.crop.Crop;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityManager {
    
    private final HerbalismPlugin plugin;
    private final Map<String, Ability> abilities;
    private final Map<UUID, Map<String, Long>> cooldowns;
    private final Map<UUID, Set<String>> toggledAbilities;
    
    public AbilityManager(HerbalismPlugin plugin) {
        this.plugin = plugin;
        this.abilities = new HashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        this.toggledAbilities = new ConcurrentHashMap<>();
        loadAbilities();
    }
    
    /**
     * Loads abilities from abilities.yml configuration
     */
    private void loadAbilities() {
        abilities.clear();
        
        FileConfiguration config = plugin.getConfigManager().getAbilitiesConfig();
        ConfigurationSection abilitiesSection = config.getConfigurationSection("abilities");
        
        if (abilitiesSection == null) {
            MessageUtil.warn("No abilities section found in abilities.yml");
            return;
        }
        
        for (String id : abilitiesSection.getKeys(false)) {
            ConfigurationSection abilityConfig = abilitiesSection.getConfigurationSection(id);
            if (abilityConfig != null) {
                try {
                    Ability ability = new Ability(id, abilityConfig);
                    abilities.put(id, ability);
                    MessageUtil.debug("Loaded ability: " + id);
                } catch (Exception e) {
                    MessageUtil.error("Failed to load ability: " + id + " - " + e.getMessage());
                }
            }
        }
        
        MessageUtil.info("Loaded " + abilities.size() + " abilities");
    }
    
    /**
     * Gets an ability by ID
     */
    public Ability getAbility(String id) {
        return abilities.get(id);
    }
    
    /**
     * Gets all registered abilities
     */
    public Map<String, Ability> getAbilities() {
        return new HashMap<>(abilities);
    }
    
    /**
     * Gets abilities available to a player based on their level
     */
    public List<Ability> getAvailableAbilities(HerbalismPlayer player) {
        List<Ability> available = new ArrayList<>();
        for (Ability ability : abilities.values()) {
            if (ability.getUnlockedLevel(player.getLevel()) > 0) {
                available.add(ability);
            }
        }
        return available;
    }
    
    /**
     * Handles crop planting abilities
     */
    public void handleCropPlanting(HerbalismPlayer player, Crop crop) {
        Player bukkit = player.getPlayer();
        if (bukkit == null) return;
        
        // Handle Cultivator ability (increased planting speed)
        Ability cultivator = getAbility("cultivator");
        if (cultivator != null) {
            int level = cultivator.getUnlockedLevel(player.getLevel());
            if (level > 0) {
                // Cultivator reduces planting time
                double reduction = cultivator.getEffectValue(level);
                // Apply reduction to crop planting (implemented in crop system)
                MessageUtil.debug("Cultivator level " + level + " applied to " + bukkit.getName() + " - reduction: " + reduction);
            }
        }
        
        // Handle Farmer's Luck ability (chance for double seeds)
        Ability farmersLuck = getAbility("farmers_luck");
        if (farmersLuck != null) {
            int level = farmersLuck.getUnlockedLevel(player.getLevel());
            if (level > 0) {
                double chance = farmersLuck.getChance(level);
                if (Math.random() < chance) {
                    // Give bonus seeds (implemented in crop system)
                    MessageUtil.debug("Farmer's Luck activated for " + bukkit.getName() + " - chance: " + chance);
                }
            }
        }
    }
    
    /**
     * Handles crop harvesting abilities
     */
    public void handleCropHarvesting(HerbalismPlayer player, Crop crop) {
        Player bukkit = player.getPlayer();
        if (bukkit == null) return;
        
        // Handle Agriculturist ability (increased yield)
        Ability agriculturist = getAbility("agriculturist");
        if (agriculturist != null) {
            int level = agriculturist.getUnlockedLevel(player.getLevel());
            if (level > 0) {
                double multiplier = agriculturist.getEffectValue(level);
                // Apply yield multiplier (implemented in crop system)
                MessageUtil.debug("Agriculturist level " + level + " applied to " + bukkit.getName() + " - multiplier: " + multiplier);
            }
        }
        
        // Handle Farmer's Luck ability (chance for bonus drops)
        Ability farmersLuck = getAbility("farmers_luck");
        if (farmersLuck != null) {
            int level = farmersLuck.getUnlockedLevel(player.getLevel());
            if (level > 0) {
                double chance = farmersLuck.getChance(level);
                if (Math.random() < chance) {
                    // Give bonus drops (implemented in crop system)
                    MessageUtil.debug("Farmer's Luck bonus drops for " + bukkit.getName() + " - chance: " + chance);
                }
            }
        }
    }
    
    /**
     * Handles fertilizer application abilities
     */
    public void handleFertilizerApplication(HerbalismPlayer player, Crop crop) {
        Player bukkit = player.getPlayer();
        if (bukkit == null) return;
        
        // Handle Cultivator ability (improved fertilizer efficiency)
        Ability cultivator = getAbility("cultivator");
        if (cultivator != null) {
            int level = cultivator.getUnlockedLevel(player.getLevel());
            if (level > 0) {
                double efficiency = cultivator.getEffectValue(level);
                // Apply fertilizer efficiency boost
                MessageUtil.debug("Cultivator fertilizer efficiency for " + bukkit.getName() + " - efficiency: " + efficiency);
            }
        }
    }
    
    /**
     * Toggles an ability on or off (for toggle type abilities)
     */
    public boolean toggleAbility(UUID playerId, String abilityId) {
        Ability ability = getAbility(abilityId);
        if (ability == null || ability.getType() != Ability.AbilityType.TOGGLE) {
            return false;
        }
        
        Set<String> playerToggled = toggledAbilities.computeIfAbsent(playerId, k -> new HashSet<>());
        
        if (playerToggled.contains(abilityId)) {
            playerToggled.remove(abilityId);
            return false; // Turned off
        } else {
            playerToggled.add(abilityId);
            return true; // Turned on
        }
    }
    
    /**
     * Checks if an ability is toggled on for a player
     */
    public boolean isAbilityToggled(UUID playerId, String abilityId) {
        Set<String> playerToggled = toggledAbilities.get(playerId);
        return playerToggled != null && playerToggled.contains(abilityId);
    }
    
    /**
     * Checks if a player is on cooldown for an ability
     */
    public boolean isOnCooldown(UUID playerId, String abilityId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        
        Long cooldownEnd = playerCooldowns.get(abilityId);
        if (cooldownEnd == null) return false;
        
        return System.currentTimeMillis() < cooldownEnd;
    }
    
    /**
     * Gets the remaining cooldown time for an ability
     */
    public long getRemainingCooldown(UUID playerId, String abilityId) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        
        Long cooldownEnd = playerCooldowns.get(abilityId);
        if (cooldownEnd == null) return 0;
        
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }
    
    /**
     * Sets a cooldown for an ability
     */
    public void setCooldown(UUID playerId, String abilityId, long cooldownMillis) {
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        playerCooldowns.put(abilityId, System.currentTimeMillis() + cooldownMillis);
    }
    
    /**
     * Clears all cooldowns for a player
     */
    public void clearCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }
    
    /**
     * Clears all toggles for a player
     */
    public void clearToggles(UUID playerId) {
        toggledAbilities.remove(playerId);
    }
    
    /**
     * Gets the effective level of an ability for a player
     */
    public int getEffectiveLevel(HerbalismPlayer player, String abilityId) {
        Ability ability = getAbility(abilityId);
        if (ability == null) return 0;
        
        int unlockedLevel = ability.getUnlockedLevel(player.getLevel());
        
        // Check if ability is toggled off
        if (ability.getType() == Ability.AbilityType.TOGGLE && 
            !isAbilityToggled(player.getUUID(), abilityId)) {
            return 0;
        }
        
        // Check if ability is on cooldown
        if (isOnCooldown(player.getUUID(), abilityId)) {
            return 0;
        }
        
        return unlockedLevel;
    }
    
    /**
     * Gets the effect value for a player's ability
     */
    public double getEffectValue(HerbalismPlayer player, String abilityId) {
        int level = getEffectiveLevel(player, abilityId);
        if (level == 0) return 0.0;
        
        Ability ability = getAbility(abilityId);
        return ability != null ? ability.getEffectValue(level) : 0.0;
    }
    
    /**
     * Gets the chance for a player's ability
     */
    public double getChance(HerbalismPlayer player, String abilityId) {
        int level = getEffectiveLevel(player, abilityId);
        if (level == 0) return 0.0;
        
        Ability ability = getAbility(abilityId);
        return ability != null ? ability.getChance(level) : 0.0;
    }
    
    /**
     * Reloads ability configuration
     */
    public void reload() {
        loadAbilities();
    }
    
    /**
     * Clears all player data (used on plugin disable)
     */
    public void clearAllData() {
        cooldowns.clear();
        toggledAbilities.clear();
    }
} 