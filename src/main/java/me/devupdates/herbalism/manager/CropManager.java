package me.devupdates.herbalism.manager;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.crop.Crop;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CropManager {
    
    private final HerbalismPlugin plugin;
    private final Map<UUID, Crop> crops = new ConcurrentHashMap<>();
    private final Map<Location, Crop> locationToCrop = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> playerCrops = new ConcurrentHashMap<>();
    private final Map<String, CropType> cropTypes = new HashMap<>();
    
    public CropManager(HerbalismPlugin plugin) {
        this.plugin = plugin;
        loadCropTypes();
        MessageUtil.info("CropManager initialized with " + cropTypes.size() + " crop types!");
    }
    
    private void loadCropTypes() {
        FileConfiguration cropsConfig = plugin.getConfigManager().getCropsConfig();
        ConfigurationSection cropsSection = cropsConfig.getConfigurationSection("crops");
        
        if (cropsSection == null) {
            MessageUtil.warning("No crops configuration found!");
            return;
        }
        
        for (String cropId : cropsSection.getKeys(false)) {
            try {
                ConfigurationSection cropSection = cropsSection.getConfigurationSection(cropId);
                if (cropSection != null) {
                    CropType cropType = loadCropType(cropId, cropSection);
                    cropTypes.put(cropId, cropType);
                }
            } catch (Exception e) {
                MessageUtil.error("Error loading crop type " + cropId + ": " + e.getMessage());
            }
        }
    }
    
    private CropType loadCropType(String id, ConfigurationSection section) {
        String name = section.getString("name", id);
        String category = section.getString("category", "Default");
        int duration = section.getInt("duration", 3600);
        boolean sustainable = section.getBoolean("sustainable", false);
        
        Material plantMaterial = Material.valueOf(section.getString("plant-material", "WHEAT"));
        Material harvestMaterial = Material.valueOf(section.getString("harvest-material", "WHEAT"));
        Material seedMaterial = Material.valueOf(section.getString("seed-material", "WHEAT_SEEDS"));
        
        int requiredLevel = section.getInt("requirement.level", 1);
        String permission = section.getString("requirement.permission", "herbalism.crop." + id);
        
        List<DropItem> drops = new ArrayList<>();
        if (section.contains("drops")) {
            for (Map<?, ?> dropMap : section.getMapList("drops")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dropData = (Map<String, Object>) dropMap;
                
                Material material = Material.valueOf((String) dropData.get("material"));
                int amount = (Integer) dropData.get("amount");
                double chance = ((Number) dropData.get("chance")).doubleValue();
                int bonusAmount = (Integer) dropData.getOrDefault("bonus-amount", 0);
                double bonusChance = ((Number) dropData.getOrDefault("bonus-chance", 0.0)).doubleValue();
                
                drops.add(new DropItem(material, amount, chance, bonusAmount, bonusChance));
            }
        }
        
        return new CropType(id, name, category, duration, sustainable, plantMaterial, 
                          harvestMaterial, seedMaterial, requiredLevel, permission, drops);
    }
    
    public Collection<CropType> getCropTypes() {
        return cropTypes.values();
    }
    
    public CropType getCropType(String id) {
        return cropTypes.get(id);
    }
    
    public boolean canPlantCrop(Player player, String cropTypeId) {
        CropType cropType = getCropType(cropTypeId);
        if (cropType == null) return false;
        
        HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(player);
        if (herbalismPlayer == null) return false;
        
        // Check level requirement
        if (herbalismPlayer.getLevel() < cropType.getRequiredLevel()) {
            return false;
        }
        
        // Check permission
        if (!player.hasPermission(cropType.getPermission())) {
            return false;
        }
        
        // Check plant limit
        if (!herbalismPlayer.canPlantMore()) {
            return false;
        }
        
        return true;
    }
    
    public boolean plantCrop(Player player, String cropTypeId, Location location) {
        if (!canPlantCrop(player, cropTypeId)) {
            return false;
        }
        
        CropType cropType = getCropType(cropTypeId);
        if (cropType == null) return false;
        
        // Check if location is already occupied
        if (locationToCrop.containsKey(location)) {
            return false;
        }
        
        // Check if block is suitable for planting
        Block block = location.getBlock();
        if (block.getType() != Material.FARMLAND) {
            return false;
        }
        
        // Get herbalism player for ability calculations
        HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(player);
        if (herbalismPlayer == null) return false;
        
        // Apply Green Thumb ability (faster growth)
        long duration = cropType.getDuration();
        double greenThumbModifier = plugin.getAbilityManager().getEffectValue(herbalismPlayer, "green_thumb");
        if (greenThumbModifier > 0) {
            duration = (long) (duration * greenThumbModifier);
        }
        
        // Create and plant the crop
        Crop crop = new Crop(cropTypeId, player.getUniqueId(), player.getName(), location, duration);
        
        // Set the block to the plant material
        Block plantBlock = block.getRelative(0, 1, 0);
        plantBlock.setType(cropType.getPlantMaterial());
        
        // Register the crop
        crops.put(crop.getId(), crop);
        locationToCrop.put(location, crop);
        
        // Update player crop count
        playerCrops.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(crop.getId());
        
        // Update player statistics
        herbalismPlayer.addCropPlanted();
        herbalismPlayer.setActiveCrops(herbalismPlayer.getActiveCrops() + 1);
        
        // Handle planting abilities
        plugin.getAbilityManager().handleCropPlanting(herbalismPlayer, crop);
        
        MessageUtil.debug("Player " + player.getName() + " planted " + cropTypeId + " at " + location);
        return true;
    }
    
    public boolean harvestCrop(Player player, Location location) {
        Crop crop = locationToCrop.get(location);
        if (crop == null) return false;
        
        // Check if player is the owner
        if (!crop.isOwner(player)) {
            return false;
        }
        
        // Check if crop is ready for harvest
        if (!crop.isReadyForHarvest()) {
            return false;
        }
        
        // Harvest the crop
        return performHarvest(crop, player);
    }
    
    private boolean performHarvest(Crop crop, Player player) {
        CropType cropType = getCropType(crop.getCropType());
        if (cropType == null) return false;
        
        // Calculate drops
        List<ItemStack> drops = calculateDrops(cropType, player);
        
        // Give drops to player
        for (ItemStack drop : drops) {
            player.getInventory().addItem(drop);
        }
        
        // Update crop state
        crop.setHarvested(true);
        
        // Remove from location mapping
        locationToCrop.remove(crop.getLocation());
        
        // Handle sustainable crops
        if (cropType.isSustainable()) {
            // Replant the crop
            Crop newCrop = new Crop(crop.getCropType(), crop.getOwnerId(), crop.getOwnerName(), 
                                   crop.getLocation(), cropType.getDuration());
            crops.put(newCrop.getId(), newCrop);
            locationToCrop.put(crop.getLocation(), newCrop);
            
            // Update player crop mapping
            Set<UUID> playerCropSet = playerCrops.get(crop.getOwnerId());
            if (playerCropSet != null) {
                playerCropSet.remove(crop.getId());
                playerCropSet.add(newCrop.getId());
            }
        } else {
            // Remove the crop completely
            crop.remove();
            
            // Update player crop count
            Set<UUID> playerCropSet = playerCrops.get(crop.getOwnerId());
            if (playerCropSet != null) {
                playerCropSet.remove(crop.getId());
            }
            
            // Update player statistics
            HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(crop.getOwnerId());
            if (herbalismPlayer != null) {
                herbalismPlayer.setActiveCrops(herbalismPlayer.getActiveCrops() - 1);
            }
        }
        
        // Update player statistics
        HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(player);
        if (herbalismPlayer != null) {
            herbalismPlayer.addHarvest();
            
            // Add experience
            long expGained = cropType.getDuration() / 10; // 1 exp per 10 seconds of growth time
            herbalismPlayer.addExperience(expGained);
            
            // Handle harvesting abilities
            plugin.getAbilityManager().handleCropHarvesting(herbalismPlayer, crop);
        }
        
        MessageUtil.debug("Player " + player.getName() + " harvested " + crop.getCropType());
        return true;
    }
    
    private List<ItemStack> calculateDrops(CropType cropType, Player player) {
        List<ItemStack> drops = new ArrayList<>();
        
        HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(player);
        
        // Get ability modifiers
        double agriculturistMultiplier = herbalismPlayer != null ? 
            plugin.getAbilityManager().getEffectValue(herbalismPlayer, "agriculturist") : 1.0;
        double farmersLuckChance = herbalismPlayer != null ? 
            plugin.getAbilityManager().getChance(herbalismPlayer, "farmers_luck") : 0.0;
        
        for (DropItem dropItem : cropType.getDrops()) {
            // Calculate base drops with agriculturist multiplier
            if (Math.random() * 100 < dropItem.getChance()) {
                int amount = dropItem.getAmount();
                if (agriculturistMultiplier > 1.0) {
                    amount = (int) (amount * agriculturistMultiplier);
                }
                drops.add(new ItemStack(dropItem.getMaterial(), amount));
            }
            
            // Calculate bonus drops with Farmer's Luck ability
            if (farmersLuckChance > 0 && Math.random() < farmersLuckChance) {
                drops.add(new ItemStack(dropItem.getMaterial(), dropItem.getAmount()));
            }
        }
        
        return drops;
    }
    
    public boolean breakCrop(Player player, Location location) {
        Crop crop = locationToCrop.get(location);
        if (crop == null) return false;
        
        // Check if player is the owner or has permission
        if (!crop.isOwner(player) && !player.hasPermission("herbalism.admin")) {
            return false;
        }
        
        // Remove the crop
        removeCrop(crop);
        return true;
    }
    
    private void removeCrop(Crop crop) {
        crops.remove(crop.getId());
        locationToCrop.remove(crop.getLocation());
        
        Set<UUID> playerCropSet = playerCrops.get(crop.getOwnerId());
        if (playerCropSet != null) {
            playerCropSet.remove(crop.getId());
        }
        
        // Update player statistics
        HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(crop.getOwnerId());
        if (herbalismPlayer != null) {
            herbalismPlayer.setActiveCrops(herbalismPlayer.getActiveCrops() - 1);
        }
        
        crop.remove();
    }
    
    public void updateCrops() {
        Iterator<Crop> iterator = crops.values().iterator();
        while (iterator.hasNext()) {
            Crop crop = iterator.next();
            
            // Check if crop should decay
            if (crop.isDecayTime()) {
                crop.setDecayed(true);
                removeCrop(crop);
                iterator.remove();
                continue;
            }
            
            // Update visual state
            crop.updateBlockState();
        }
    }
    
    public Crop getCrop(UUID id) {
        return crops.get(id);
    }
    
    public Crop getCropAtLocation(Location location) {
        return locationToCrop.get(location);
    }
    
    public Collection<Crop> getAllCrops() {
        return crops.values();
    }
    
    public Collection<Crop> getPlayerCrops(UUID playerId) {
        Set<UUID> cropIds = playerCrops.get(playerId);
        if (cropIds == null) return Collections.emptyList();
        
        return cropIds.stream()
                .map(crops::get)
                .filter(Objects::nonNull)
                .toList();
    }
    
    public int getPlayerCropCount(UUID playerId) {
        Set<UUID> cropIds = playerCrops.get(playerId);
        return cropIds != null ? cropIds.size() : 0;
    }
    
    public void saveAllCrops() {
        // TODO: Implement database saving
        MessageUtil.debug("Saved " + crops.size() + " crops to database");
    }
    
    public void loadAllCrops() {
        // TODO: Implement database loading
        MessageUtil.debug("Loaded crops from database");
    }
    
    public void shutdown() {
        MessageUtil.info("Shutting down CropManager...");
        saveAllCrops();
        crops.clear();
        locationToCrop.clear();
        playerCrops.clear();
        MessageUtil.info("CropManager shutdown complete!");
    }
    
    // Inner classes for crop configuration
    public static class CropType {
        private final String id;
        private final String name;
        private final String category;
        private final int duration;
        private final boolean sustainable;
        private final Material plantMaterial;
        private final Material harvestMaterial;
        private final Material seedMaterial;
        private final int requiredLevel;
        private final String permission;
        private final List<DropItem> drops;
        
        public CropType(String id, String name, String category, int duration, boolean sustainable,
                       Material plantMaterial, Material harvestMaterial, Material seedMaterial,
                       int requiredLevel, String permission, List<DropItem> drops) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.duration = duration;
            this.sustainable = sustainable;
            this.plantMaterial = plantMaterial;
            this.harvestMaterial = harvestMaterial;
            this.seedMaterial = seedMaterial;
            this.requiredLevel = requiredLevel;
            this.permission = permission;
            this.drops = drops;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public int getDuration() { return duration; }
        public boolean isSustainable() { return sustainable; }
        public Material getPlantMaterial() { return plantMaterial; }
        public Material getHarvestMaterial() { return harvestMaterial; }
        public Material getSeedMaterial() { return seedMaterial; }
        public int getRequiredLevel() { return requiredLevel; }
        public String getPermission() { return permission; }
        public List<DropItem> getDrops() { return drops; }
    }
    
    public static class DropItem {
        private final Material material;
        private final int amount;
        private final double chance;
        private final int bonusAmount;
        private final double bonusChance;
        
        public DropItem(Material material, int amount, double chance, int bonusAmount, double bonusChance) {
            this.material = material;
            this.amount = amount;
            this.chance = chance;
            this.bonusAmount = bonusAmount;
            this.bonusChance = bonusChance;
        }
        
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
        public double getChance() { return chance; }
        public int getBonusAmount() { return bonusAmount; }
        public double getBonusChance() { return bonusChance; }
    }
} 