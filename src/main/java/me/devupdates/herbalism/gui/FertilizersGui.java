package me.devupdates.herbalism.gui;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.fertilizer.Fertilizer;
import me.devupdates.herbalism.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FertilizersGui extends GuiBase {
    
    private int currentPage = 0;
    private final int fertilizersPerPage = 28; // 4 rows of 7 fertilizers each
    
    public FertilizersGui(HerbalismPlugin plugin, Player player) {
        super(plugin, player, "&2&lFertilizers Guide", 54);
    }
    
    @Override
    protected void setupInventory() {
        // Fill border
        fillBorder();
        
        // Back button
        setItem(45, createBackButton(), (p, click) -> new MainMenuGui(plugin, p).open());
        
        // Close button
        setItem(53, createCloseButton(), (p, click) -> p.closeInventory());
        
        // Display fertilizers
        displayFertilizers();
        
        // Navigation buttons
        setupNavigationButtons();
        
        // Info item
        setItem(4, createInfoItem());
    }
    
    private void displayFertilizers() {
        Map<String, Fertilizer> fertilizers = plugin.getFertilizerManager().getFertilizers();
        List<Fertilizer> fertilizerList = new ArrayList<>(fertilizers.values());
        
        int startIndex = currentPage * fertilizersPerPage;
        int endIndex = Math.min(startIndex + fertilizersPerPage, fertilizerList.size());
        
        // Clear fertilizer display area
        for (int i = 10; i <= 43; i++) {
            if (i % 9 != 0 && i % 9 != 8) { // Skip border slots
                inventory.setItem(i, null);
            }
        }
        
        // Display fertilizers for current page
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Fertilizer fertilizer = fertilizerList.get(i);
            int slot = getSlotForIndex(slotIndex);
            
            setItem(slot, createFertilizerItem(fertilizer));
            slotIndex++;
        }
        
        // Fill empty slots in display area
        for (int i = slotIndex; i < fertilizersPerPage; i++) {
            int slot = getSlotForIndex(i);
            setItem(slot, createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " "));
        }
    }
    
    private int getSlotForIndex(int index) {
        int row = index / 7;
        int col = index % 7;
        return (row + 1) * 9 + (col + 1);
    }
    
    private ItemStack createFertilizerItem(Fertilizer fertilizer) {
        List<String> lore = new ArrayList<>();
        
        // Add description
        lore.addAll(fertilizer.getLore());
        lore.add("");
        
        // Add effects
        lore.add("&7Effects:");
        lore.add("&8• &7Growth Speed: &a+" + String.format("%.0f%%", (fertilizer.getGrowthMultiplier() - 1) * 100));
        
        if (fertilizer.isPermanent()) {
            lore.add("&8• &7Duration: &dPermanent");
        } else {
            lore.add("&8• &7Duration: &e" + TimeUtil.formatTime(fertilizer.getDuration()));
        }
        
        lore.add("&8• &7Success Rate: &e" + String.format("%.0f%%", fertilizer.getSuccessChance() * 100));
        lore.add("&8• &7Experience: &b+" + fertilizer.getExperienceGain());
        lore.add("");
        
        // Add requirements
        lore.add("&7Requirements:");
        lore.add("&8• &7Level: &e" + fertilizer.getRequiredLevel());
        
        if (herbalismPlayer.getLevel() >= fertilizer.getRequiredLevel()) {
            lore.add("&8• &aYou can use this fertilizer!");
        } else {
            lore.add("&8• &cYou need " + (fertilizer.getRequiredLevel() - herbalismPlayer.getLevel()) + " more levels");
        }
        
        lore.add("");
        lore.add("&7Material: &f" + fertilizer.getMaterial().name());
        lore.add("&7Stack Size: &f" + fertilizer.getStackSize());
        
        return createItem(fertilizer.getMaterial(), fertilizer.getName(), lore);
    }
    
    private ItemStack createInfoItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Learn about fertilizers");
        lore.add("");
        lore.add("&7Fertilizers boost crop growth");
        lore.add("&7by applying them to planted crops.");
        lore.add("");
        lore.add("&7Right-click fertilizers on crops");
        lore.add("&7to apply their effects!");
        lore.add("");
        lore.add("&7Your Level: &a" + herbalismPlayer.getLevel());
        
        return createItem(Material.BONE_MEAL, "&2&lFertilizer Guide", lore);
    }
    
    private void setupNavigationButtons() {
        Map<String, Fertilizer> fertilizers = plugin.getFertilizerManager().getFertilizers();
        int totalFertilizers = fertilizers.size();
        int totalPages = (int) Math.ceil((double) totalFertilizers / fertilizersPerPage);
        
        // Previous page
        if (currentPage > 0) {
            setItem(46, createPrevPageButton(), (p, click) -> {
                currentPage--;
                refresh();
            });
        }
        
        // Page indicator
        setItem(49, createItem(Material.PAPER, "&2&lPage " + (currentPage + 1) + "/" + Math.max(1, totalPages),
                "&7Showing fertilizers " + (currentPage * fertilizersPerPage + 1) + " to " + 
                Math.min((currentPage + 1) * fertilizersPerPage, totalFertilizers)));
        
        // Next page
        if (currentPage < totalPages - 1) {
            setItem(52, createNextPageButton(), (p, click) -> {
                currentPage++;
                refresh();
            });
        }
    }
} 