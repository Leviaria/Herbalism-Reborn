package me.devupdates.herbalism.listener;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.crop.Crop;
import me.devupdates.herbalism.fertilizer.Fertilizer;
import me.devupdates.herbalism.manager.CropManager;
import me.devupdates.herbalism.player.HerbalismPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class BlockListener implements Listener {
    
    private final HerbalismPlugin plugin;
    
    public BlockListener(HerbalismPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = event.getItemInHand();
        
        // Check if player is trying to plant a crop
        if (isCropSeed(item)) {
            handleCropPlanting(event, player, block, item);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Check if player is trying to break a crop - first check the broken block
        Crop crop = plugin.getCropManager().getCropAtLocation(block.getLocation());
        
        // If not found, check the block below (since crops are stored at farmland position)
        if (crop == null) {
            Block blockBelow = block.getRelative(0, -1, 0);
            crop = plugin.getCropManager().getCropAtLocation(blockBelow.getLocation());
        }
        
        if (crop != null) {
            handleCropBreaking(event, player, crop);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        
        if (block == null) return;
        
        // Check for crop harvesting - first check the clicked block
        Crop crop = plugin.getCropManager().getCropAtLocation(block.getLocation());
        
        // If not found, check the block below (since crops are stored at farmland position)
        if (crop == null) {
            Block blockBelow = block.getRelative(0, -1, 0);
            crop = plugin.getCropManager().getCropAtLocation(blockBelow.getLocation());
        }
        
        if (crop != null) {
            // Check if player is trying to apply fertilizer
            if (item != null && event.getAction().name().contains("RIGHT_CLICK")) {
                Fertilizer fertilizer = plugin.getFertilizerManager().identifyFertilizer(item);
                if (fertilizer != null) {
                    handleFertilizerApplication(event, player, crop, fertilizer);
                    return;
                }
            }
            
            // Handle regular crop interaction (harvesting)
            handleCropInteraction(event, player, crop);
        }
    }
    
    private boolean isCropSeed(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Check if the item is a crop seed
        for (CropManager.CropType cropType : plugin.getCropManager().getCropTypes()) {
            if (cropType.getSeedMaterial() == item.getType()) {
                return true;
            }
        }
        
        return false;
    }
    
    private void handleCropPlanting(BlockPlaceEvent event, Player player, Block block, ItemStack item) {
        // Find the crop type for this seed
        CropManager.CropType cropType = null;
        for (CropManager.CropType type : plugin.getCropManager().getCropTypes()) {
            if (type.getSeedMaterial() == item.getType()) {
                cropType = type;
                break;
            }
        }
        
        if (cropType == null) return;
        
        // Check if player can plant this crop
        if (!plugin.getCropManager().canPlantCrop(player, cropType.getId())) {
            event.setCancelled(true);
            
            HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(player);
            if (herbalismPlayer != null) {
                if (herbalismPlayer.getLevel() < cropType.getRequiredLevel()) {
                    Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                        "level", cropType.getRequiredLevel()
                    );
                    plugin.getLanguageManager().sendMessage(player, "messages.crops.plant-level-required", placeholders);
                } else if (!herbalismPlayer.canPlantMore()) {
                    Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                        "limit", herbalismPlayer.getTotalPlants()
                    );
                    plugin.getLanguageManager().sendMessage(player, "messages.crops.plant-limit-reached", placeholders);
                } else {
                    plugin.getLanguageManager().sendMessage(player, "messages.crops.plant-no-permission");
                }
            }
            return;
        }
        
        // Check if the block below is suitable for planting
        Block belowBlock = block.getRelative(0, -1, 0);
        if (belowBlock.getType() != Material.FARMLAND) {
            event.setCancelled(true);
            plugin.getLanguageManager().sendMessage(player, "messages.crops.plant-not-allowed");
            return;
        }
        
        // Plant the crop
        boolean success = plugin.getCropManager().plantCrop(player, cropType.getId(), belowBlock.getLocation());
        if (success) {
            Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                "crop_name", cropType.getName()
            );
            plugin.getLanguageManager().sendMessage(player, "messages.crops.planted", placeholders);
            
            // Add experience for planting
            HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(player);
            if (herbalismPlayer != null) {
                herbalismPlayer.addExperience(10); // Small XP for planting
            }
        } else {
            event.setCancelled(true);
            plugin.getLanguageManager().sendMessage(player, "messages.crops.plant-failed");
        }
    }
    
    private void handleCropBreaking(BlockBreakEvent event, Player player, Crop crop) {
        // Check if player is the owner or has permission
        if (!crop.isOwner(player) && !player.hasPermission("herbalism.admin")) {
            event.setCancelled(true);
            Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                "owner", crop.getOwnerName()
            );
            plugin.getLanguageManager().sendMessage(player, "messages.crops.broken-not-owner", placeholders);
            return;
        }
        
        // If crop is ready for harvest, try to harvest instead of breaking
        if (crop.isReadyForHarvest()) {
            event.setCancelled(true);
            handleCropHarvesting(player, crop);
        } else {
            // Allow breaking, but remove from crop manager
            plugin.getCropManager().breakCrop(player, crop.getLocation());
            
            CropManager.CropType cropType = plugin.getCropManager().getCropType(crop.getCropType());
            if (cropType != null) {
                Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                    "crop_name", cropType.getName()
                );
                plugin.getLanguageManager().sendMessage(player, "messages.crops.broken", placeholders);
            }
        }
    }
    
    private void handleCropInteraction(PlayerInteractEvent event, Player player, Crop crop) {
        // Check if player is right-clicking
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            // Check if crop is ready for harvest
            if (crop.isReadyForHarvest()) {
                event.setCancelled(true);
                handleCropHarvesting(player, crop);
            } else {
                // Show crop information
                showCropInfo(player, crop);
            }
        }
    }
    
    private void handleCropHarvesting(Player player, Crop crop) {
        // Check if player is the owner
        if (!crop.isOwner(player)) {
            plugin.getLanguageManager().sendMessage(player, "messages.crops.harvest-not-owner");
            return;
        }
        
        // Check if crop is ready
        if (!crop.isReadyForHarvest()) {
            Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                "time", crop.getFormattedTimeUntilHarvest()
            );
            plugin.getLanguageManager().sendMessage(player, "messages.crops.harvest-time-left", placeholders);
            return;
        }
        
        // Harvest the crop
        boolean success = plugin.getCropManager().harvestCrop(player, crop.getLocation());
        if (success) {
            CropManager.CropType cropType = plugin.getCropManager().getCropType(crop.getCropType());
            if (cropType != null) {
                Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                    "crop_name", cropType.getName()
                );
                plugin.getLanguageManager().sendMessage(player, "messages.crops.harvested", placeholders);
            }
        } else {
            plugin.getLanguageManager().sendMessage(player, "messages.crops.harvest-failed");
        }
    }
    
    private void showCropInfo(Player player, Crop crop) {
        CropManager.CropType cropType = plugin.getCropManager().getCropType(crop.getCropType());
        if (cropType == null) return;
        
        Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
            "crop_name", cropType.getName(),
            "owner", crop.getOwnerName(),
            "time_left", crop.getFormattedTimeUntilHarvest()
        );
        
        if (crop.isReadyForHarvest()) {
            plugin.getLanguageManager().sendActionBar(player, "messages.actionbar.crop-ready", placeholders);
        } else {
            plugin.getLanguageManager().sendActionBar(player, "messages.actionbar.crop-growing", placeholders);
        }
        
        // Show hologram for the crop
        plugin.getLogger().info("Attempting to show hologram for crop: " + crop.getCropType());
        plugin.getHologramManager().showCropHologram(player, crop);
    }
    
    private void handleFertilizerApplication(PlayerInteractEvent event, Player player, Crop crop, Fertilizer fertilizer) {
        HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(player);
        if (herbalismPlayer == null) return;
        
        // Check if crop is already harvested
        if (crop.isHarvested()) {
            plugin.getLanguageManager().sendMessage(player, "messages.fertilizer.already-harvested");
            return;
        }
        
        // Check if crop is ready for harvest
        if (crop.isReadyForHarvest()) {
            plugin.getLanguageManager().sendMessage(player, "messages.fertilizer.already-ready");
            return;
        }
        
        // Check if player owns the crop
        if (!crop.getOwnerId().equals(player.getUniqueId())) {
            plugin.getLanguageManager().sendMessage(player, "messages.fertilizer.not-owner");
            return;
        }
        
        // Check if crop already has fertilizer effect
        if (plugin.getFertilizerManager().hasFertilizerEffect(crop)) {
            plugin.getLanguageManager().sendMessage(player, "messages.fertilizer.already-fertilized");
            return;
        }
        
        // Try to apply fertilizer
        if (plugin.getFertilizerManager().applyFertilizer(crop, fertilizer, herbalismPlayer)) {
            // Remove one fertilizer item from player's hand
            ItemStack item = event.getItem();
            if (item != null && item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            event.setCancelled(true);
        }
    }
} 