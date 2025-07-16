package me.devupdates.herbalism.gui;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.crop.Crop;
import me.devupdates.herbalism.manager.CropManager;
import me.devupdates.herbalism.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CropsGui extends GuiBase {
    
    private int currentPage = 0;
    private final int cropsPerPage = 28; // 4 rows of 7 crops each
    
    public CropsGui(HerbalismPlugin plugin, Player player) {
        super(plugin, player, "&2&lCrops Manager", 54);
    }
    
    @Override
    protected void setupInventory() {
        // Fill border
        fillBorder();
        
        // Back button
        setItem(45, createBackButton(), (p, click) -> new MainMenuGui(plugin, p).open());
        
        // Close button
        setItem(53, createCloseButton(), (p, click) -> p.closeInventory());
        
        // Display crops
        displayCrops();
        
        // Navigation buttons
        setupNavigationButtons();
        
        // Info item
        setItem(4, createInfoItem());
    }
    
    private void displayCrops() {
        List<Crop> crops = new ArrayList<>();
        
        for (Crop crop : plugin.getCropManager().getPlayerCrops(player.getUniqueId())) {
            if (crop != null && !crop.isHarvested()) {
                crops.add(crop);
            }
        }
        
        int startIndex = currentPage * cropsPerPage;
        int endIndex = Math.min(startIndex + cropsPerPage, crops.size());
        
        // Clear crop display area
        for (int i = 10; i <= 43; i++) {
            if (i % 9 != 0 && i % 9 != 8) { // Skip border slots
                inventory.setItem(i, null);
            }
        }
        
        // Display crops for current page
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Crop crop = crops.get(i);
            int slot = getSlotForIndex(slotIndex);
            
            setItem(slot, createCropItem(crop), (p, click) -> {
                if (click.isRightClick()) {
                    // Teleport to crop
                    p.teleport(crop.getLocation());
                    p.closeInventory();
                    plugin.getLanguageManager().sendMessage(p, "messages.crops.teleported");
                }
            });
            
            slotIndex++;
        }
        
        // Fill empty slots in display area
        for (int i = slotIndex; i < cropsPerPage; i++) {
            int slot = getSlotForIndex(i);
            setItem(slot, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
        }
    }
    
    private int getSlotForIndex(int index) {
        int row = index / 7;
        int col = index % 7;
        return (row + 1) * 9 + (col + 1);
    }
    
    private ItemStack createCropItem(Crop crop) {
        CropManager.CropType cropType = plugin.getCropManager().getCropType(crop.getCropType());
        if (cropType == null) {
            return createItem(Material.BARRIER, "&cInvalid Crop", "&cThis crop type is no longer valid");
        }
        
        List<String> lore = new ArrayList<>();
        lore.add("&7Crop Type: &e" + cropType.getName());
        lore.add("&7Location: &f" + crop.getLocation().getBlockX() + ", " + 
                 crop.getLocation().getBlockY() + ", " + crop.getLocation().getBlockZ());
        lore.add("&7World: &f" + crop.getLocation().getWorld().getName());
        lore.add("");
        
        if (crop.isReadyForHarvest()) {
            lore.add("&a&lREADY FOR HARVEST!");
            lore.add("&7Go to your crop and harvest it!");
        } else {
            long timeLeft = crop.getTimeUntilHarvest();
            lore.add("&7Time until harvest: &e" + TimeUtil.formatTimeMillis(timeLeft));
            
            // Calculate progress
            long totalTime = crop.getOriginalDuration();
            long elapsed = totalTime - timeLeft;
            double progress = (double) elapsed / totalTime;
            
            lore.add("&7Progress: &f" + String.format("%.1f%%", progress * 100));
            lore.add(createProgressBar(progress));
        }
        
        lore.add("");
        lore.add("&7Planted: &f" + TimeUtil.formatDate(crop.getPlantTime()));
        
        // Check for fertilizer effects
        if (plugin.getFertilizerManager().hasFertilizerEffect(crop)) {
            var effect = plugin.getFertilizerManager().getFertilizerEffect(crop);
            lore.add("");
            lore.add("&d&lFertilized!");
            lore.add("&7Fertilizer: &d" + effect.getFertilizer().getName());
            lore.add("&7Growth Boost: &d" + String.format("%.0f%%", (effect.getFertilizer().getGrowthMultiplier() - 1) * 100));
            
            if (!effect.getFertilizer().isPermanent()) {
                long remaining = effect.getRemainingTime(System.currentTimeMillis());
                lore.add("&7Time Left: &d" + TimeUtil.formatTimeMillis(remaining));
            }
        }
        
        lore.add("");
        lore.add("&e&lRight-click to teleport!");
        
        return createItem(cropType.getPlantMaterial(), "&2" + cropType.getName(), lore);
    }
    
    private ItemStack createInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Manage your active crops");
        lore.add("");
        lore.add("&7Active Crops: &e" + herbalismPlayer.getActiveCrops());
        lore.add("&7Plant Limit: &e" + herbalismPlayer.getTotalPlants() + "&7/&e" + herbalismPlayer.getMaxPlants());
        lore.add("");
        lore.add("&7Right-click crops to teleport");
        lore.add("&7to their locations!");
        
        return createItem(Material.WHEAT, "&2&lCrop Information", lore);
    }
    
    private void setupNavigationButtons() {
        int totalCrops = 0;
        
        for (Crop crop : plugin.getCropManager().getPlayerCrops(player.getUniqueId())) {
            if (crop != null && !crop.isHarvested()) {
                totalCrops++;
            }
        }
        
        int totalPages = (int) Math.ceil((double) totalCrops / cropsPerPage);
        
        // Previous page
        if (currentPage > 0) {
            setItem(46, createPrevPageButton(), (p, click) -> {
                currentPage--;
                refresh();
            });
        }
        
        // Page indicator
        setItem(49, createItem(Material.PAPER, "&2&lPage " + (currentPage + 1) + "/" + Math.max(1, totalPages),
                "&7Showing crops " + (currentPage * cropsPerPage + 1) + " to " + 
                Math.min((currentPage + 1) * cropsPerPage, totalCrops)));
        
        // Next page
        if (currentPage < totalPages - 1) {
            setItem(52, createNextPageButton(), (p, click) -> {
                currentPage++;
                refresh();
            });
        }
    }
} 