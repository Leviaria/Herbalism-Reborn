package me.devupdates.herbalism.ability;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class Ability {
    private final String id;
    private final String name;
    private final String description;
    private final List<String> lore;
    private final AbilityType type;
    private final int maxLevel;
    private final List<AbilityLevel> levels;
    private final boolean passive;
    private final int cooldown;
    
    public Ability(String id, ConfigurationSection config) {
        this.id = id;
        this.name = config.getString("name", id);
        this.description = config.getString("description", "");
        this.lore = config.getStringList("lore");
        this.type = AbilityType.valueOf(config.getString("type", "PASSIVE").toUpperCase());
        this.maxLevel = config.getInt("max-level", 1);
        this.levels = new ArrayList<>();
        this.passive = config.getBoolean("passive", true);
        this.cooldown = config.getInt("cooldown", 0);
        
        // Load ability levels
        ConfigurationSection levelsSection = config.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String levelKey : levelsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelKey);
                    ConfigurationSection levelConfig = levelsSection.getConfigurationSection(levelKey);
                    if (levelConfig != null) {
                        AbilityLevel abilityLevel = new AbilityLevel(level, levelConfig);
                        levels.add(abilityLevel);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid level keys
                }
            }
        }
        
        // Sort levels by level number
        levels.sort((a, b) -> Integer.compare(a.getLevel(), b.getLevel()));
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }
    
    public AbilityType getType() {
        return type;
    }
    
    public int getMaxLevel() {
        return maxLevel;
    }
    
    public List<AbilityLevel> getLevels() {
        return new ArrayList<>(levels);
    }
    
    public boolean isPassive() {
        return passive;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    /**
     * Gets the ability level configuration for a specific level
     */
    public AbilityLevel getLevel(int level) {
        for (AbilityLevel abilityLevel : levels) {
            if (abilityLevel.getLevel() == level) {
                return abilityLevel;
            }
        }
        return null;
    }
    
    /**
     * Gets the highest unlocked level for a player based on their herbalism level
     */
    public int getUnlockedLevel(int playerLevel) {
        int highestLevel = 0;
        for (AbilityLevel level : levels) {
            if (playerLevel >= level.getRequiredLevel()) {
                highestLevel = Math.max(highestLevel, level.getLevel());
            }
        }
        return highestLevel;
    }
    
    /**
     * Checks if a player can use this ability at a specific level
     */
    public boolean canUse(int playerLevel, int abilityLevel) {
        AbilityLevel level = getLevel(abilityLevel);
        return level != null && playerLevel >= level.getRequiredLevel();
    }
    
    /**
     * Gets the effect value for a specific level
     */
    public double getEffectValue(int level) {
        AbilityLevel abilityLevel = getLevel(level);
        return abilityLevel != null ? abilityLevel.getEffectValue() : 0.0;
    }
    
    /**
     * Gets the chance for a specific level
     */
    public double getChance(int level) {
        AbilityLevel abilityLevel = getLevel(level);
        return abilityLevel != null ? abilityLevel.getChance() : 0.0;
    }
    
    @Override
    public String toString() {
        return "Ability{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", maxLevel=" + maxLevel +
                ", passive=" + passive +
                '}';
    }
    
    /**
     * Enum for different ability types
     */
    public enum AbilityType {
        PASSIVE,        // Always active
        ACTIVE,         // Must be activated
        CONDITIONAL,    // Activated under certain conditions
        TOGGLE          // Can be turned on/off
    }
    
    /**
     * Inner class representing a single level of an ability
     */
    public static class AbilityLevel {
        private final int level;
        private final int requiredLevel;
        private final double effectValue;
        private final double chance;
        private final String description;
        
        public AbilityLevel(int level, ConfigurationSection config) {
            this.level = level;
            this.requiredLevel = config.getInt("required-level", 1);
            this.effectValue = config.getDouble("effect-value", 0.0);
            this.chance = config.getDouble("chance", 1.0);
            this.description = config.getString("description", "");
        }
        
        public int getLevel() {
            return level;
        }
        
        public int getRequiredLevel() {
            return requiredLevel;
        }
        
        public double getEffectValue() {
            return effectValue;
        }
        
        public double getChance() {
            return chance;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return "AbilityLevel{" +
                    "level=" + level +
                    ", requiredLevel=" + requiredLevel +
                    ", effectValue=" + effectValue +
                    ", chance=" + chance +
                    '}';
        }
    }
} 