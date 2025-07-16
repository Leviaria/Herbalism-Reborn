package me.devupdates.herbalism.fertilizer;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Fertilizer {
    private final String id;
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final double growthMultiplier;
    private final int duration; // in seconds
    private final double successChance;
    private final int requiredLevel;
    private final int experienceGain;
    private final boolean permanent;
    private final int stackSize;
    
    public Fertilizer(String id, ConfigurationSection config) {
        this.id = id;
        this.name = config.getString("name", id);
        this.material = Material.valueOf(config.getString("material", "BONE_MEAL"));
        this.lore = config.getStringList("lore");
        this.growthMultiplier = config.getDouble("growth-multiplier", 1.5);
        this.duration = config.getInt("duration", 300); // 5 minutes default
        this.successChance = config.getDouble("success-chance", 1.0);
        this.requiredLevel = config.getInt("required-level", 1);
        this.experienceGain = config.getInt("experience-gain", 5);
        this.permanent = config.getBoolean("permanent", false);
        this.stackSize = config.getInt("stack-size", 64);
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }
    
    public double getGrowthMultiplier() {
        return growthMultiplier;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public double getSuccessChance() {
        return successChance;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public int getExperienceGain() {
        return experienceGain;
    }
    
    public boolean isPermanent() {
        return permanent;
    }
    
    public int getStackSize() {
        return stackSize;
    }
    
    /**
     * Creates an ItemStack representation of this fertilizer
     */
    public ItemStack createItemStack() {
        return createItemStack(1);
    }
    
    /**
     * Creates an ItemStack representation of this fertilizer with specified amount
     */
    public ItemStack createItemStack(int amount) {
        ItemStack item = new ItemStack(material, Math.min(amount, stackSize));
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Checks if an ItemStack matches this fertilizer type
     */
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return name.equals(meta.getDisplayName());
    }
    
    @Override
    public String toString() {
        return "Fertilizer{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", material=" + material +
                ", growthMultiplier=" + growthMultiplier +
                ", duration=" + duration +
                ", successChance=" + successChance +
                ", requiredLevel=" + requiredLevel +
                '}';
    }
} 