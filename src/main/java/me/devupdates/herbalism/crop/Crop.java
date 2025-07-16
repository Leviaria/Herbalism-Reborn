package me.devupdates.herbalism.crop;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.TimeUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;

import java.util.*;

public class Crop {
    
    private final UUID id;
    private final String cropType;
    private final UUID ownerId;
    private final String ownerName;
    private final Location location;
    private final long plantTime;
    private final long originalDuration;
    
    private long harvestTime;
    private boolean harvested;
    private boolean decayed;
    private final Set<UUID> fertilizedBy;
    private double growthModifier = 1.0;
    
    public Crop(String cropType, UUID ownerId, String ownerName, Location location, long durationSeconds) {
        this.id = UUID.randomUUID();
        this.cropType = cropType;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.location = location.clone();
        this.plantTime = TimeUtil.getCurrentTimeMillis();
        this.originalDuration = durationSeconds * 1000; // Convert to milliseconds
        this.harvestTime = plantTime + originalDuration;
        this.harvested = false;
        this.decayed = false;
        this.fertilizedBy = new HashSet<>();
    }
    
    public UUID getId() {
        return id;
    }
    
    public String getCropType() {
        return cropType;
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public String getOwnerName() {
        return ownerName;
    }
    
    public Location getLocation() {
        return location.clone();
    }
    
    public long getPlantTime() {
        return plantTime;
    }
    
    public long getOriginalDuration() {
        return originalDuration;
    }
    
    public long getHarvestTime() {
        return harvestTime;
    }
    
    public void setHarvestTime(long harvestTime) {
        this.harvestTime = harvestTime;
    }
    
    public boolean isHarvested() {
        return harvested;
    }
    
    public void setHarvested(boolean harvested) {
        this.harvested = harvested;
    }
    
    public boolean isDecayed() {
        return decayed;
    }
    
    public void setDecayed(boolean decayed) {
        this.decayed = decayed;
    }
    
    public Set<UUID> getFertilizedBy() {
        return new HashSet<>(fertilizedBy);
    }
    
    public boolean hasFertilizedBy(UUID playerId) {
        return fertilizedBy.contains(playerId);
    }
    
    public void addFertilizedBy(UUID playerId) {
        fertilizedBy.add(playerId);
    }
    
    public boolean isReadyForHarvest() {
        return !harvested && !decayed && TimeUtil.getCurrentTimeMillis() >= harvestTime;
    }
    
    public boolean isDecayTime() {
        if (harvested || decayed) return false;
        
        HerbalismPlugin plugin = HerbalismPlugin.getInstance();
        double decayMultiplier = plugin.getConfigManager().getCropDecayMultiplier();
        long decayTime = harvestTime + (long) (originalDuration * decayMultiplier);
        
        return TimeUtil.getCurrentTimeMillis() >= decayTime;
    }
    
    public long getTimeUntilHarvest() {
        if (isReadyForHarvest()) return 0;
        return Math.max(0, harvestTime - TimeUtil.getCurrentTimeMillis());
    }
    
    public long getTimeUntilDecay() {
        if (isDecayTime()) return 0;
        
        HerbalismPlugin plugin = HerbalismPlugin.getInstance();
        double decayMultiplier = plugin.getConfigManager().getCropDecayMultiplier();
        long decayTime = harvestTime + (long) (originalDuration * decayMultiplier);
        
        return Math.max(0, decayTime - TimeUtil.getCurrentTimeMillis());
    }
    
    public double getGrowthProgress() {
        if (harvested || decayed) return 1.0;
        
        long elapsed = TimeUtil.getCurrentTimeMillis() - plantTime;
        return Math.min(1.0, (double) elapsed / originalDuration);
    }
    
    public String getFormattedTimeUntilHarvest() {
        return TimeUtil.formatTimeMillis(getTimeUntilHarvest());
    }
    
    public String getFormattedTimeUntilDecay() {
        return TimeUtil.formatTimeMillis(getTimeUntilDecay());
    }
    
    public Player getOwner() {
        return HerbalismPlugin.getInstance().getServer().getPlayer(ownerId);
    }
    
    public HerbalismPlayer getHerbalismPlayer() {
        return HerbalismPlugin.getInstance().getPlayerManager().getPlayer(ownerId);
    }
    
    public Block getBlock() {
        return location.getBlock();
    }
    
    public boolean isValidBlock() {
        Block block = getBlock();
        return block != null && block.getType() != Material.AIR;
    }
    
    public boolean isOwner(Player player) {
        return player != null && player.getUniqueId().equals(ownerId);
    }
    
    public boolean isOwner(UUID playerId) {
        return playerId != null && playerId.equals(ownerId);
    }
    
    public void fertilize(UUID playerId, long timeReduction) {
        if (!hasFertilizedBy(playerId)) {
            addFertilizedBy(playerId);
            harvestTime = Math.max(plantTime, harvestTime - timeReduction);
        }
    }
    
    public void fertilize(UUID playerId, double percentReduction) {
        if (!hasFertilizedBy(playerId)) {
            addFertilizedBy(playerId);
            long reduction = (long) (originalDuration * (percentReduction / 100.0));
            harvestTime = Math.max(plantTime, harvestTime - reduction);
        }
    }
    
    public void updateBlockState() {
        if (harvested || decayed) return;
        
        Block block = getBlock();
        if (block == null || block.getType() == Material.AIR) {
            setDecayed(true);
            return;
        }
        
        // Update the visual growth state of the crop
        if (block.getBlockData() instanceof Ageable ageable) {
            int maxAge = ageable.getMaximumAge();
            int currentAge = (int) (getGrowthProgress() * maxAge);
            
            if (isReadyForHarvest()) {
                currentAge = maxAge;
            }
            
            if (ageable.getAge() != currentAge) {
                ageable.setAge(currentAge);
                block.setBlockData(ageable);
            }
        }
    }
    
    public void remove() {
        Block block = getBlock();
        if (block != null && block.getType() != Material.AIR) {
            block.setType(Material.AIR);
        }
    }
    
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id.toString());
        data.put("crop_type", cropType);
        data.put("owner_id", ownerId.toString());
        data.put("owner_name", ownerName);
        data.put("world", location.getWorld().getName());
        data.put("x", location.getX());
        data.put("y", location.getY());
        data.put("z", location.getZ());
        data.put("plant_time", plantTime);
        data.put("original_duration", originalDuration);
        data.put("harvest_time", harvestTime);
        data.put("harvested", harvested);
        data.put("decayed", decayed);
        data.put("fertilized_by", fertilizedBy.stream().map(UUID::toString).toList());
        data.put("growth_modifier", growthModifier);
        return data;
    }
    
    @SuppressWarnings("unchecked")
    public static Crop deserialize(Map<String, Object> data) {
        UUID id = UUID.fromString((String) data.get("id"));
        String cropType = (String) data.get("crop_type");
        UUID ownerId = UUID.fromString((String) data.get("owner_id"));
        String ownerName = (String) data.get("owner_name");
        
        String worldName = (String) data.get("world");
        double x = ((Number) data.get("x")).doubleValue();
        double y = ((Number) data.get("y")).doubleValue();
        double z = ((Number) data.get("z")).doubleValue();
        
        Location location = new Location(
            HerbalismPlugin.getInstance().getServer().getWorld(worldName),
            x, y, z
        );
        
        long plantTime = ((Number) data.get("plant_time")).longValue();
        long originalDuration = ((Number) data.get("original_duration")).longValue();
        long harvestTime = ((Number) data.get("harvest_time")).longValue();
        boolean harvested = (Boolean) data.get("harvested");
        boolean decayed = (Boolean) data.get("decayed");
        
        // Create crop with dummy duration since we'll set the actual times
        Crop crop = new Crop(cropType, ownerId, ownerName, location, 1);
        
        // Set the actual field values using reflection or a constructor
        try {
            java.lang.reflect.Field idField = Crop.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(crop, id);
            
            java.lang.reflect.Field plantTimeField = Crop.class.getDeclaredField("plantTime");
            plantTimeField.setAccessible(true);
            plantTimeField.set(crop, plantTime);
            
            java.lang.reflect.Field originalDurationField = Crop.class.getDeclaredField("originalDuration");
            originalDurationField.setAccessible(true);
            originalDurationField.set(crop, originalDuration);
            
            crop.setHarvestTime(harvestTime);
            crop.setHarvested(harvested);
            crop.setDecayed(decayed);
            
            // Load fertilized by list
            if (data.containsKey("fertilized_by")) {
                List<String> fertilizedByStrings = (List<String>) data.get("fertilized_by");
                for (String uuidString : fertilizedByStrings) {
                    crop.addFertilizedBy(UUID.fromString(uuidString));
                }
            }
            
            // Load growth modifier
            if (data.containsKey("growth_modifier")) {
                crop.setGrowthModifier((Double) data.get("growth_modifier"));
            }
            
        } catch (Exception e) {
            HerbalismPlugin.getInstance().getLogger().severe("Error deserializing crop: " + e.getMessage());
        }
        
        return crop;
    }
    
    /**
     * Sets the growth modifier for this crop (used by fertilizers)
     */
    public void setGrowthModifier(double growthModifier) {
        this.growthModifier = growthModifier;
    }
    
    /**
     * Gets the current growth modifier for this crop
     */
    public double getGrowthModifier() {
        return growthModifier;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Crop crop = (Crop) obj;
        return Objects.equals(id, crop.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Crop{" +
                "id=" + id +
                ", cropType='" + cropType + '\'' +
                ", ownerId=" + ownerId +
                ", ownerName='" + ownerName + '\'' +
                ", location=" + location +
                ", readyForHarvest=" + isReadyForHarvest() +
                ", decayed=" + decayed +
                '}';
    }
} 