package me.devupdates.herbalism.command;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.manager.CropManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HerbalismTabCompleter implements TabCompleter {
    
    private final HerbalismPlugin plugin;
    
    public HerbalismTabCompleter(HerbalismPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // First argument - subcommands
            return filterSuggestions(Arrays.asList(
                "help", "about", "reload", "menu", "stats", "exp", "level", "load"
            ), args[0]);
        }
        
        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "stats":
                case "status":
                case "check":
                    return getPlayerNames(args[1]);
                    
                case "exp":
                case "experience":
                case "xp":
                    return handleExpTabComplete(sender, args);
                    
                case "level":
                    return handleLevelTabComplete(sender, args);
                    
                case "load":
                case "give":
                    return handleLoadTabComplete(sender, args);
                    
                default:
                    return Collections.emptyList();
            }
        }
        
        return Collections.emptyList();
    }
    
    private List<String> handleExpTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herbalism.command.exp")) {
            return Collections.emptyList();
        }
        
        if (args.length == 2) {
            // Second argument - action
            return filterSuggestions(Arrays.asList("set", "add", "take"), args[1]);
        }
        
        if (args.length == 3) {
            // Third argument - amount
            return filterSuggestions(Arrays.asList("100", "500", "1000", "5000"), args[2]);
        }
        
        if (args.length == 4) {
            // Fourth argument - player name
            return getPlayerNames(args[3]);
        }
        
        return Collections.emptyList();
    }
    
    private List<String> handleLevelTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herbalism.command.level")) {
            return Collections.emptyList();
        }
        
        if (args.length == 2) {
            // Second argument - action
            return filterSuggestions(Arrays.asList("set", "add", "take"), args[1]);
        }
        
        if (args.length == 3) {
            // Third argument - level
            return filterSuggestions(Arrays.asList("1", "5", "10", "25", "50", "100"), args[2]);
        }
        
        if (args.length == 4) {
            // Fourth argument - player name
            return getPlayerNames(args[3]);
        }
        
        return Collections.emptyList();
    }
    
    private List<String> handleLoadTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herbalism.command.load")) {
            return Collections.emptyList();
        }
        
        if (args.length == 2) {
            // Second argument - type
            return filterSuggestions(Arrays.asList("item", "fertilizer", "crop"), args[1]);
        }
        
        if (args.length == 3) {
            // Third argument - item ID
            String type = args[1].toLowerCase();
            switch (type) {
                case "item":
                    return getItemIds(args[2]);
                case "fertilizer":
                    return getFertilizerIds(args[2]);
                case "crop":
                    return getCropIds(args[2]);
                default:
                    return Collections.emptyList();
            }
        }
        
        if (args.length == 4) {
            // Fourth argument - amount
            return filterSuggestions(Arrays.asList("1", "5", "10", "64"), args[3]);
        }
        
        if (args.length == 5) {
            // Fifth argument - player name
            return getPlayerNames(args[4]);
        }
        
        return Collections.emptyList();
    }
    
    private List<String> getPlayerNames(String partial) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    private List<String> getItemIds(String partial) {
        // TODO: Get item IDs from configuration
        List<String> itemIds = Arrays.asList(
            "magic_wheat_seeds", "golden_carrot_seeds", "farmers_hoe", "herbalists_staff",
            "crop_guide", "soil_tester", "growth_detector", "time_crystal", "essence_of_growth"
        );
        return filterSuggestions(itemIds, partial);
    }
    
    private List<String> getFertilizerIds(String partial) {
        // TODO: Get fertilizer IDs from configuration
        List<String> fertilizerIds = Arrays.asList(
            "bone_meal", "compost", "super_fertilizer", "miracle_grow", "instant_grow",
            "root_booster", "fruit_enhancer", "grain_accelerator", "time_warp_dust", "divine_essence"
        );
        return filterSuggestions(fertilizerIds, partial);
    }
    
    private List<String> getCropIds(String partial) {
        CropManager cropManager = plugin.getCropManager();
        if (cropManager == null) {
            return Collections.emptyList();
        }
        
        return cropManager.getCropTypes().stream()
                .map(CropManager.CropType::getId)
                .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    private List<String> filterSuggestions(List<String> suggestions, String partial) {
        if (partial.isEmpty()) {
            return suggestions;
        }
        
        return suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }
} 