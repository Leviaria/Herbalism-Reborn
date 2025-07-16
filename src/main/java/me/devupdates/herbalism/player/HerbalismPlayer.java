package me.devupdates.herbalism.player;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.util.TimeUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HerbalismPlayer {
    
    private final UUID uuid;
    private final String name;
    
    // Core stats
    private int level;
    private long experience;
    private int totalPlants;
    private int maxPlants;
    
    // Statistics
    private int totalHarvests;
    private int totalFertilizersUsed;
    private int totalCropsPlanted;
    private long totalPlayTime;
    private long firstJoined;
    private long lastSeen;
    
    // Abilities - ability name -> level
    private final Map<String, Integer> abilities = new HashMap<>();
    
    // Cooldowns - fertilizer type -> end time
    private final Map<String, Long> fertilizerCooldowns = new HashMap<>();
    
    // Temporary effects
    private final Map<String, Long> activeEffects = new HashMap<>();
    
    // Crop ownership tracking
    private int activeCrops;
    
    public HerbalismPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.level = 1;
        this.experience = 0;
        this.totalPlants = HerbalismPlugin.getInstance().getConfigManager().getBaseTotalPlants();
        this.maxPlants = HerbalismPlugin.getInstance().getConfigManager().getMaxTotalPlants();
        this.totalHarvests = 0;
        this.totalFertilizersUsed = 0;
        this.totalCropsPlanted = 0;
        this.totalPlayTime = 0;
        this.firstJoined = TimeUtil.getCurrentTimeMillis();
        this.lastSeen = TimeUtil.getCurrentTimeMillis();
        this.activeCrops = 0;
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
    public String getName() {
        return name;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(level, getMaxLevel()));
    }
    
    public long getExperience() {
        return experience;
    }
    
    public void setExperience(long experience) {
        this.experience = Math.max(0, experience);
    }
    
    public void addExperience(long amount) {
        this.experience += amount;
        checkLevelUp();
    }
    
    public void removeExperience(long amount) {
        this.experience = Math.max(0, this.experience - amount);
    }
    
    public long getExperienceRequired() {
        return getExperienceRequired(level);
    }
    
    public long getExperienceRequired(int level) {
        HerbalismPlugin plugin = HerbalismPlugin.getInstance();
        double a = plugin.getConfigManager().getFormulaExpA();
        double b = plugin.getConfigManager().getFormulaExpB();
        double c = plugin.getConfigManager().getFormulaExpC();
        
        return (long) ((a * level * level) + (b * level) + c);
    }
    
    public long getExperienceToNextLevel() {
        return Math.max(0, getExperienceRequired() - experience);
    }
    
    public double getExperienceProgress() {
        long required = getExperienceRequired();
        if (required == 0) return 1.0;
        return Math.min(1.0, (double) experience / required);
    }
    
    public int getMaxLevel() {
        return HerbalismPlugin.getInstance().getConfigManager().getMaxLevel();
    }
    
    public boolean canLevelUp() {
        return level < getMaxLevel() && experience >= getExperienceRequired();
    }
    
    public void checkLevelUp() {
        while (canLevelUp()) {
            level++;
            
            // Trigger level up event
            Player player = getPlayer();
            if (player != null) {
                HerbalismPlugin plugin = HerbalismPlugin.getInstance();
                Map<String, Object> placeholders = new HashMap<>();
                placeholders.put("level", level);
                
                // Send title and message
                plugin.getLanguageManager().sendTitle(player, "messages.level-up.title", "messages.level-up.subtitle", placeholders);
                plugin.getLanguageManager().sendMessage(player, "messages.level-up.message", placeholders);
                
                // Check for new abilities
                checkAbilityUnlocks();
            }
        }
    }
    
    public int getTotalPlants() {
        return totalPlants;
    }
    
    public void setTotalPlants(int totalPlants) {
        this.totalPlants = Math.max(0, Math.min(totalPlants, maxPlants));
    }
    
    public int getMaxPlants() {
        return maxPlants;
    }
    
    public void setMaxPlants(int maxPlants) {
        this.maxPlants = Math.max(1, maxPlants);
    }
    
    public int getActiveCrops() {
        return activeCrops;
    }
    
    public void setActiveCrops(int activeCrops) {
        this.activeCrops = Math.max(0, activeCrops);
    }
    
    public boolean canPlantMore() {
        return activeCrops < totalPlants;
    }
    
    public int getTotalHarvests() {
        return totalHarvests;
    }
    
    public void addHarvest() {
        this.totalHarvests++;
    }
    
    public int getTotalFertilizersUsed() {
        return totalFertilizersUsed;
    }
    
    public void addFertilizerUsed() {
        this.totalFertilizersUsed++;
    }
    
    public int getTotalCropsPlanted() {
        return totalCropsPlanted;
    }
    
    public void addCropPlanted() {
        this.totalCropsPlanted++;
    }
    
    public long getTotalPlayTime() {
        return totalPlayTime;
    }
    
    public void addPlayTime(long time) {
        this.totalPlayTime += time;
    }
    
    public long getFirstJoined() {
        return firstJoined;
    }
    
    public void setFirstJoined(long firstJoined) {
        this.firstJoined = firstJoined;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public void updateLastSeen() {
        this.lastSeen = TimeUtil.getCurrentTimeMillis();
    }
    
    // Ability management
    public Map<String, Integer> getAbilities() {
        return new HashMap<>(abilities);
    }
    
    public int getAbilityLevel(String abilityName) {
        return abilities.getOrDefault(abilityName, 0);
    }
    
    public void setAbilityLevel(String abilityName, int level) {
        if (level <= 0) {
            abilities.remove(abilityName);
        } else {
            abilities.put(abilityName, level);
        }
    }
    
    public boolean hasAbility(String abilityName) {
        return abilities.containsKey(abilityName) && abilities.get(abilityName) > 0;
    }
    
    public void checkAbilityUnlocks() {
        // This would check configuration for ability unlocks at current level
        // Implementation depends on ability system
    }
    
    // Cooldown management
    public boolean hasCooldown(String fertilizerType) {
        Long endTime = fertilizerCooldowns.get(fertilizerType);
        if (endTime == null) return false;
        
        if (TimeUtil.getCurrentTimeMillis() >= endTime) {
            fertilizerCooldowns.remove(fertilizerType);
            return false;
        }
        return true;
    }
    
    public void setCooldown(String fertilizerType, long durationSeconds) {
        long endTime = TimeUtil.getCurrentTimeMillis() + (durationSeconds * 1000);
        fertilizerCooldowns.put(fertilizerType, endTime);
    }
    
    public long getCooldownRemaining(String fertilizerType) {
        Long endTime = fertilizerCooldowns.get(fertilizerType);
        if (endTime == null) return 0;
        
        return Math.max(0, endTime - TimeUtil.getCurrentTimeMillis()) / 1000;
    }
    
    // Active effects
    public void addEffect(String effectName, long durationSeconds) {
        long endTime = TimeUtil.getCurrentTimeMillis() + (durationSeconds * 1000);
        activeEffects.put(effectName, endTime);
    }
    
    public boolean hasEffect(String effectName) {
        Long endTime = activeEffects.get(effectName);
        if (endTime == null) return false;
        
        if (TimeUtil.getCurrentTimeMillis() >= endTime) {
            activeEffects.remove(effectName);
            return false;
        }
        return true;
    }
    
    public long getEffectRemaining(String effectName) {
        Long endTime = activeEffects.get(effectName);
        if (endTime == null) return 0;
        
        return Math.max(0, endTime - TimeUtil.getCurrentTimeMillis()) / 1000;
    }
    
    // Utility methods
    public Player getPlayer() {
        return HerbalismPlugin.getInstance().getServer().getPlayer(uuid);
    }
    
    public OfflinePlayer getOfflinePlayer() {
        return HerbalismPlugin.getInstance().getServer().getOfflinePlayer(uuid);
    }
    
    public boolean isOnline() {
        return getPlayer() != null;
    }
    
    public String getDisplayName() {
        Player player = getPlayer();
        return player != null ? player.getDisplayName() : name;
    }
    
    // Data serialization methods for database
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("uuid", uuid.toString());
        data.put("name", name);
        data.put("level", level);
        data.put("experience", experience);
        data.put("total_plants", totalPlants);
        data.put("max_plants", maxPlants);
        data.put("total_harvests", totalHarvests);
        data.put("total_fertilizers_used", totalFertilizersUsed);
        data.put("total_crops_planted", totalCropsPlanted);
        data.put("total_play_time", totalPlayTime);
        data.put("first_joined", firstJoined);
        data.put("last_seen", lastSeen);
        data.put("active_crops", activeCrops);
        data.put("abilities", abilities);
        data.put("fertilizer_cooldowns", fertilizerCooldowns);
        data.put("active_effects", activeEffects);
        return data;
    }
    
    @SuppressWarnings("unchecked")
    public void deserialize(Map<String, Object> data) {
        this.level = (Integer) data.getOrDefault("level", 1);
        this.experience = ((Number) data.getOrDefault("experience", 0L)).longValue();
        this.totalPlants = (Integer) data.getOrDefault("total_plants", HerbalismPlugin.getInstance().getConfigManager().getBaseTotalPlants());
        this.maxPlants = (Integer) data.getOrDefault("max_plants", HerbalismPlugin.getInstance().getConfigManager().getMaxTotalPlants());
        this.totalHarvests = (Integer) data.getOrDefault("total_harvests", 0);
        this.totalFertilizersUsed = (Integer) data.getOrDefault("total_fertilizers_used", 0);
        this.totalCropsPlanted = (Integer) data.getOrDefault("total_crops_planted", 0);
        this.totalPlayTime = ((Number) data.getOrDefault("total_play_time", 0L)).longValue();
        this.firstJoined = ((Number) data.getOrDefault("first_joined", TimeUtil.getCurrentTimeMillis())).longValue();
        this.lastSeen = ((Number) data.getOrDefault("last_seen", TimeUtil.getCurrentTimeMillis())).longValue();
        this.activeCrops = (Integer) data.getOrDefault("active_crops", 0);
        
        if (data.containsKey("abilities")) {
            this.abilities.clear();
            Map<String, Integer> abilitiesData = (Map<String, Integer>) data.get("abilities");
            this.abilities.putAll(abilitiesData);
        }
        
        if (data.containsKey("fertilizer_cooldowns")) {
            this.fertilizerCooldowns.clear();
            Map<String, Long> cooldownsData = (Map<String, Long>) data.get("fertilizer_cooldowns");
            this.fertilizerCooldowns.putAll(cooldownsData);
        }
        
        if (data.containsKey("active_effects")) {
            this.activeEffects.clear();
            Map<String, Long> effectsData = (Map<String, Long>) data.get("active_effects");
            this.activeEffects.putAll(effectsData);
        }
    }
    
    // Additional setter methods for database operations
    public void setTotalHarvests(int totalHarvests) {
        this.totalHarvests = totalHarvests;
    }
    
    public void setTotalFertilizersUsed(int totalFertilizersUsed) {
        this.totalFertilizersUsed = totalFertilizersUsed;
    }
    
    public void setTotalCropsPlanted(int totalCropsPlanted) {
        this.totalCropsPlanted = totalCropsPlanted;
    }
    
    public void setTotalPlayTime(long totalPlayTime) {
        this.totalPlayTime = totalPlayTime;
    }
} 