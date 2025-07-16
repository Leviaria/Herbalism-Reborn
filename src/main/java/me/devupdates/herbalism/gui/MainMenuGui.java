package me.devupdates.herbalism.gui;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MainMenuGui extends GuiBase {
    
    public MainMenuGui(HerbalismPlugin plugin, Player player) {
        super(plugin, player, "&2&lHerbalism Menu", 45);
    }
    
    @Override
    protected void setupInventory() {
        // Fill border
        fillBorder();
        
        // Player head with stats
        setItem(4, createPlayerStatsItem());
        
        // Navigation items
        setItem(19, createAbilitiesItem(), (p, click) -> new AbilitiesGui(plugin, p).open());
        setItem(20, createCropsItem(), (p, click) -> new CropsGui(plugin, p).open());
        setItem(22, createCropCatalogItem(), (p, click) -> new CropCatalogGui(plugin, p).open());
        setItem(24, createFertilizersItem(), (p, click) -> new FertilizersGui(plugin, p).open());
        setItem(25, createSettingsItem(), (p, click) -> new SettingsGui(plugin, p).open());
        
        // Close button
        setItem(40, createCloseButton(), (p, click) -> p.closeInventory());
        
        // Decorative items
        setItem(13, createLevelProgressItem());
        setItem(31, createStatisticsItem());
    }
    
    /**
     * Creates the player stats item (player head)
     */
    private ItemStack createPlayerStatsItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Player: &f" + player.getName());
        lore.add("&7Level: &a" + herbalismPlayer.getLevel() + "&7/&a" + plugin.getConfigManager().getMaxLevel());
        lore.add("&7Experience: &b" + formatNumber(herbalismPlayer.getExperience()) + "&7/&b" + formatNumber(herbalismPlayer.getExperienceRequired()));
        lore.add("");
        lore.add("&7Active Crops: &e" + herbalismPlayer.getActiveCrops());
        lore.add("&7Plant Limit: &e" + herbalismPlayer.getTotalPlants() + "&7/&e" + herbalismPlayer.getMaxPlants());
        lore.add("");
        lore.add("&7Total Harvests: &6" + formatNumber(herbalismPlayer.getTotalHarvests()));
        lore.add("&7Fertilizers Used: &d" + formatNumber(herbalismPlayer.getTotalFertilizersUsed()));
        lore.add("");
        lore.add("&8First Joined: &7" + TimeUtil.formatDate(herbalismPlayer.getFirstJoined()));
        lore.add("&8Last Seen: &7" + TimeUtil.formatDate(herbalismPlayer.getLastSeen()));
        
        return createItem(Material.PLAYER_HEAD, "&2&l" + player.getName(), lore);
    }
    
    /**
     * Creates the abilities navigation item
     */
    private ItemStack createAbilitiesItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7View and manage your farming abilities");
        lore.add("");
        lore.add("&7Available abilities:");
        lore.add("&8• &2Cultivator &7- Faster planting");
        lore.add("&8• &6Agriculturist &7- Increased yield");
        lore.add("&8• &eFarmer's Luck &7- Bonus drops");
        lore.add("&8• &aGreen Thumb &7- Faster growth");
        lore.add("&8• &dHarvest Master &7- Resource efficiency");
        lore.add("&8• &bNature's Blessing &7- Instant maturation");
        lore.add("");
        lore.add("&e&lClick to view abilities!");
        
        return createItem(Material.ENCHANTED_BOOK, "&2&lAbilities", lore);
    }
    
    /**
     * Creates the crops navigation item
     */
    private ItemStack createCropsItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Manage your active crops");
        lore.add("");
        lore.add("&7Active Crops: &e" + herbalismPlayer.getActiveCrops());
        lore.add("&7Plant Limit: &e" + herbalismPlayer.getTotalPlants() + "&7/&e" + herbalismPlayer.getMaxPlants());
        lore.add("");
        lore.add("&7View crop status, harvest times,");
        lore.add("&7and teleport to your farms!");
        lore.add("");
        lore.add("&e&lClick to manage crops!");
        
        return createItem(Material.WHEAT, "&2&lCrops Manager", lore);
    }
    
    /**
     * Creates the crop catalog navigation item
     */
    private ItemStack createCropCatalogItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Browse all available crops");
        lore.add("&7and their requirements");
        lore.add("");
        lore.add("&7View crop information:");
        lore.add("&8• &7Growth times");
        lore.add("&8• &7Level requirements");
        lore.add("&8• &7Drop chances");
        lore.add("&8• &7Seed requirements");
        lore.add("");
        lore.add("&e&lClick to browse catalog!");
        
        return createItem(Material.KNOWLEDGE_BOOK, "&2&lCrop Catalog", lore);
    }
    
    /**
     * Creates the fertilizers navigation item
     */
    private ItemStack createFertilizersItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7View available fertilizers");
        lore.add("");
        lore.add("&7Learn about different fertilizer types,");
        lore.add("&7their effects, and requirements.");
        lore.add("");
        lore.add("&7From basic &7Bone Meal &7to legendary");
        lore.add("&bEternal Fertilizer&7, discover them all!");
        lore.add("");
        lore.add("&e&lClick to view fertilizers!");
        
        return createItem(Material.BONE_MEAL, "&2&lFertilizers", lore);
    }
    
    /**
     * Creates the settings navigation item
     */
    private ItemStack createSettingsItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Configure plugin settings");
        lore.add("");
        lore.add("&7Customize your farming experience:");
        lore.add("&8• &7Notification settings");
        lore.add("&8• &7Language preferences");
        lore.add("&8• &7Display options");
        lore.add("&8• &7Sound settings");
        lore.add("");
        lore.add("&e&lClick to open settings!");
        
        return createItem(Material.COMPARATOR, "&2&lSettings", lore);
    }
    
    /**
     * Creates the level progress item
     */
    private ItemStack createLevelProgressItem() {
        double progress = (double) herbalismPlayer.getExperience() / herbalismPlayer.getExperienceRequired();
        String progressText = formatNumber(herbalismPlayer.getExperience()) + " / " + formatNumber(herbalismPlayer.getExperienceRequired());
        
        List<String> lore = new ArrayList<>();
        lore.add("&7Current Level: &a" + herbalismPlayer.getLevel());
        lore.add("&7Next Level: &a" + (herbalismPlayer.getLevel() + 1));
        lore.add("");
        lore.add("&7Experience Progress:");
        lore.add("&7" + progressText);
        lore.add("");
        lore.add(createProgressBar(progress));
        
        if (herbalismPlayer.getLevel() >= plugin.getConfigManager().getMaxLevel()) {
            lore.clear();
            lore.add("&a&lMAX LEVEL REACHED!");
            lore.add("&7You have reached the maximum level!");
            lore.add("");
            lore.add("&7Total Experience: &b" + formatNumber(herbalismPlayer.getExperience()));
        }
        
        return createItem(Material.EXPERIENCE_BOTTLE, "&2&lLevel Progress", lore);
    }
    
    /**
     * Creates the statistics item
     */
    private ItemStack createStatisticsItem() {
        List<String> lore = new ArrayList<>();
        lore.add("&7Farming Statistics");
        lore.add("");
        lore.add("&7Total Crops Planted: &a" + formatNumber(herbalismPlayer.getTotalCropsPlanted()));
        lore.add("&7Total Harvests: &6" + formatNumber(herbalismPlayer.getTotalHarvests()));
        lore.add("&7Fertilizers Used: &d" + formatNumber(herbalismPlayer.getTotalFertilizersUsed()));
        lore.add("");
        lore.add("&7Active Crops: &e" + herbalismPlayer.getActiveCrops());
        lore.add("&7Plant Capacity: &e" + herbalismPlayer.getTotalPlants() + "&7/&e" + herbalismPlayer.getMaxPlants());
        lore.add("");
        
        // Calculate efficiency
        if (herbalismPlayer.getTotalCropsPlanted() > 0) {
            double efficiency = (double) herbalismPlayer.getTotalHarvests() / herbalismPlayer.getTotalCropsPlanted() * 100;
            lore.add("&7Harvest Efficiency: &b" + String.format("%.1f%%", efficiency));
        }
        
        // Calculate average exp per harvest
        if (herbalismPlayer.getTotalHarvests() > 0) {
            double avgExp = (double) herbalismPlayer.getExperience() / herbalismPlayer.getTotalHarvests();
            lore.add("&7Avg EXP per Harvest: &b" + String.format("%.1f", avgExp));
        }
        
        return createItem(Material.WRITABLE_BOOK, "&2&lStatistics", lore);
    }
} 