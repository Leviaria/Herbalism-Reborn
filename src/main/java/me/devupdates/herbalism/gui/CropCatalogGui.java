package me.devupdates.herbalism.gui;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.manager.CropManager;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CropCatalogGui extends GuiBase {
    
    private int currentPage = 0;
    private final int cropsPerPage = 28; // 4 rows of 7 crops each
    
    public CropCatalogGui(HerbalismPlugin plugin, Player player) {
        super(plugin, player, "&2&lCrop Catalog", 54);
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
        List<CropManager.CropType> allCrops = new ArrayList<>();
        List<CropManager.CropType> unlockedCrops = new ArrayList<>();
        List<CropManager.CropType> lockedCrops = new ArrayList<>();
        
        // Get all crop types
        for (CropManager.CropType cropType : plugin.getCropManager().getCropTypes()) {
            allCrops.add(cropType);
            
            if (canPlayerPlant(cropType)) {
                unlockedCrops.add(cropType);
            } else {
                lockedCrops.add(cropType);
            }
        }
        
        // Sort by category and level
        unlockedCrops.sort((a, b) -> {
            int categoryCompare = a.getCategory().compareTo(b.getCategory());
            if (categoryCompare != 0) return categoryCompare;
            return Integer.compare(a.getRequiredLevel(), b.getRequiredLevel());
        });
        
        lockedCrops.sort((a, b) -> {
            int categoryCompare = a.getCategory().compareTo(b.getCategory());
            if (categoryCompare != 0) return categoryCompare;
            return Integer.compare(a.getRequiredLevel(), b.getRequiredLevel());
        });
        
        // Combine lists (unlocked first, then locked)
        List<CropManager.CropType> displayCrops = new ArrayList<>();
        displayCrops.addAll(unlockedCrops);
        displayCrops.addAll(lockedCrops);
        
        int startIndex = currentPage * cropsPerPage;
        int endIndex = Math.min(startIndex + cropsPerPage, displayCrops.size());
        
        // Clear crop display area
        for (int i = 10; i <= 43; i++) {
            if (i % 9 != 0 && i % 9 != 8) { // Skip border slots
                inventory.setItem(i, null);
            }
        }
        
        // Display crops for current page
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            CropManager.CropType cropType = displayCrops.get(i);
            int slot = getSlotForIndex(slotIndex);
            
            boolean isUnlocked = canPlayerPlant(cropType);
            
            setItem(slot, createCropTypeItem(cropType, isUnlocked), (p, click) -> {
                if (isUnlocked && click.isLeftClick()) {
                    // TODO: Could add crop planting instructions or seed giving here
                    Map<String, Object> placeholders = new HashMap<>();
                    placeholders.put("crop_name", cropType.getName());
                    plugin.getLanguageManager().sendMessage(p, "messages.crops.catalog-info", placeholders);
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
    
    private boolean canPlayerPlant(CropManager.CropType cropType) {
        // Check level requirement
        if (herbalismPlayer.getLevel() < cropType.getRequiredLevel()) {
            return false;
        }
        
        // Check permission
        if (cropType.getPermission() != null && !player.hasPermission(cropType.getPermission())) {
            return false;
        }
        
        return true;
    }
    
    private ItemStack createCropTypeItem(CropManager.CropType cropType, boolean isUnlocked) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Category: &e" + cropType.getCategory());
        lore.add("&7Growth Time: &e" + TimeUtil.formatTime(cropType.getDuration()));
        lore.add("&7Required Level: &e" + cropType.getRequiredLevel());
        
        if (cropType.getPermission() != null) {
            lore.add("&7Permission: &e" + cropType.getPermission());
        }
        
        lore.add("");
        
        if (cropType.isSustainable()) {
            lore.add("&a&lSustainable Crop");
            lore.add("&7This crop regrows after harvest");
        } else {
            lore.add("&c&lSingle Harvest");
            lore.add("&7This crop must be replanted");
        }
        
        lore.add("");
        lore.add("&7Drops:");
        
        for (CropManager.DropItem drop : cropType.getDrops()) {
            String dropLine = "&8• &f" + drop.getAmount() + "x &e" + 
                drop.getMaterial().name().replace("_", " ");
            
            if (drop.getChance() < 100.0) {
                dropLine += " &7(&e" + String.format("%.1f%%", drop.getChance()) + "&7)";
            }
            
            lore.add(dropLine);
            
            if (drop.getBonusAmount() > 0) {
                lore.add("&8• &f+" + drop.getBonusAmount() + "x &7(bonus, &e" + 
                    String.format("%.1f%%", drop.getBonusChance()) + "&7)");
            }
        }
        
        lore.add("");
        
        if (isUnlocked) {
            lore.add("&a&lUNLOCKED");
            lore.add("&7You can plant this crop!");
            lore.add("&7Seeds needed: &e" + cropType.getSeedMaterial().name().replace("_", " "));
        } else {
            lore.add("&c&lLOCKED");
            
            if (herbalismPlayer.getLevel() < cropType.getRequiredLevel()) {
                lore.add("&7Need level &e" + cropType.getRequiredLevel() + "&7!");
                lore.add("&7Current level: &e" + herbalismPlayer.getLevel());
            }
            
            if (cropType.getPermission() != null && !player.hasPermission(cropType.getPermission())) {
                lore.add("&7Missing permission!");
            }
        }
        
        Material displayMaterial = isUnlocked ? cropType.getHarvestMaterial() : Material.RED_STAINED_GLASS_PANE;
        String displayName = (isUnlocked ? "&2" : "&c") + cropType.getName();
        
        return createItem(displayMaterial, displayName, lore);
    }
    
    private ItemStack createInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Browse all available crops");
        lore.add("&7and their requirements");
        lore.add("");
        lore.add("&7Your Level: &e" + herbalismPlayer.getLevel());
        lore.add("&7Plant Limit: &e" + herbalismPlayer.getTotalPlants() + "&7/&e" + herbalismPlayer.getMaxPlants());
        lore.add("");
        lore.add("&a&lGreen items &7are unlocked");
        lore.add("&c&lRed items &7are locked");
        
        return createItem(Material.KNOWLEDGE_BOOK, "&2&lCrop Catalog", lore);
    }
    
    private void setupNavigationButtons() {
        List<CropManager.CropType> allCrops = new ArrayList<>(plugin.getCropManager().getCropTypes());
        int totalCrops = allCrops.size();
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