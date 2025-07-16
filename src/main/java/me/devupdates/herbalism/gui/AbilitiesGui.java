package me.devupdates.herbalism.gui;

import me.devupdates.herbalism.ability.Ability;
import me.devupdates.herbalism.core.HerbalismPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AbilitiesGui extends GuiBase {
    
    private final Map<String, Ability> abilities;
    
    public AbilitiesGui(HerbalismPlugin plugin, Player player) {
        super(plugin, player, "&2&lAbilities Overview", 54);
        this.abilities = plugin.getAbilityManager().getAbilities();
    }
    
    @Override
    protected void setupInventory() {
        // Fill border
        fillBorder();
        
        // Back button
        setItem(45, createBackButton(), (p, click) -> new MainMenuGui(plugin, p).open());
        
        // Close button
        setItem(53, createCloseButton(), (p, click) -> p.closeInventory());
        
        // Display abilities
        displayAbilities();
    }
    
    private void displayAbilities() {
        int[] slots = {10, 12, 14, 16, 28, 30, 32, 34}; // 2 rows of 4 abilities each
        int slotIndex = 0;
        
        for (Ability ability : abilities.values()) {
            if (slotIndex >= slots.length) break;
            
            setItem(slots[slotIndex], createAbilityItem(ability));
            slotIndex++;
        }
    }
    
    private ItemStack createAbilityItem(Ability ability) {
        int playerLevel = herbalismPlayer.getLevel();
        int abilityLevel = ability.getUnlockedLevel(playerLevel);
        
        List<String> lore = new ArrayList<>();
        lore.add("&7" + ability.getDescription());
        lore.add("");
        
        if (abilityLevel > 0) {
            lore.add("&a&lUNLOCKED &7- Level " + abilityLevel + "/" + ability.getMaxLevel());
            lore.add("");
            
            // Current level effects
            Ability.AbilityLevel currentLevel = ability.getLevel(abilityLevel);
            if (currentLevel != null) {
                lore.add("&7Current Effects:");
                lore.add("&8• &f" + currentLevel.getDescription());
                if (currentLevel.getChance() < 1.0) {
                    lore.add("&8• &7Chance: &e" + String.format("%.1f%%", currentLevel.getChance() * 100));
                }
            }
            
            // Next level preview
            if (abilityLevel < ability.getMaxLevel()) {
                Ability.AbilityLevel nextLevel = ability.getLevel(abilityLevel + 1);
                if (nextLevel != null) {
                    lore.add("");
                    lore.add("&7Next Level (" + (abilityLevel + 1) + "):");
                    lore.add("&8• &7Required Level: &c" + nextLevel.getRequiredLevel());
                    lore.add("&8• &f" + nextLevel.getDescription());
                    if (nextLevel.getChance() < 1.0) {
                        lore.add("&8• &7Chance: &e" + String.format("%.1f%%", nextLevel.getChance() * 100));
                    }
                }
            } else {
                lore.add("");
                lore.add("&a&lMAX LEVEL REACHED!");
            }
        } else {
            lore.add("&c&lLOCKED");
            lore.add("");
            
            // Show first level requirements
            Ability.AbilityLevel firstLevel = ability.getLevel(1);
            if (firstLevel != null) {
                lore.add("&7Requirements:");
                lore.add("&8• &7Level: &c" + firstLevel.getRequiredLevel() + " &7(You: &a" + playerLevel + "&7)");
                lore.add("");
                lore.add("&7Level 1 Effects:");
                lore.add("&8• &f" + firstLevel.getDescription());
                if (firstLevel.getChance() < 1.0) {
                    lore.add("&8• &7Chance: &e" + String.format("%.1f%%", firstLevel.getChance() * 100));
                }
            }
        }
        
        // Add ability type and cooldown info
        lore.add("");
        lore.add("&7Type: &e" + ability.getType().name());
        if (ability.getCooldown() > 0) {
            lore.add("&7Cooldown: &e" + (ability.getCooldown() / 1000) + "s");
        }
        
        // Choose material based on ability
        Material material = getMaterialForAbility(ability.getId());
        
        return createItem(material, ability.getName(), lore);
    }
    
    private Material getMaterialForAbility(String abilityId) {
        switch (abilityId.toLowerCase()) {
            case "cultivator":
                return Material.IRON_HOE;
            case "agriculturist":
                return Material.GOLDEN_HOE;
            case "farmers_luck":
                return Material.DIAMOND_HOE;
            case "green_thumb":
                return Material.NETHERITE_HOE;
            case "harvest_master":
                return Material.ENCHANTED_BOOK;
            case "natures_blessing":
                return Material.NETHER_STAR;
            default:
                return Material.BOOK;
        }
    }
} 