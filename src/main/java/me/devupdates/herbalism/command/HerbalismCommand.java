package me.devupdates.herbalism.command;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class HerbalismCommand implements CommandExecutor {
    
    private final HerbalismPlugin plugin;
    
    public HerbalismCommand(HerbalismPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Default to menu command if no arguments
            return executeMenu(sender, args);
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                return executeHelp(sender, args);
            case "about":
                return executeAbout(sender, args);
            case "reload":
                return executeReload(sender, args);
            case "menu":
            case "gui":
                return executeMenu(sender, args);
            case "stats":
            case "status":
            case "check":
                return executeStats(sender, args);
            case "exp":
            case "experience":
            case "xp":
                return executeExp(sender, args);
            case "level":
                return executeLevel(sender, args);
            case "load":
            case "give":
                return executeLoad(sender, args);
            default:
                return executeHelp(sender, args);
        }
    }
    
    private boolean executeHelp(CommandSender sender, String[] args) {
        plugin.getLanguageManager().sendMultilineMessage(sender, "messages.command.help.header");
        plugin.getLanguageManager().sendMultilineMessage(sender, "messages.command.help.lines");
        plugin.getLanguageManager().sendMultilineMessage(sender, "messages.command.help.footer");
        return true;
    }
    
    private boolean executeAbout(CommandSender sender, String[] args) {
        Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
            "version", plugin.getDescription().getVersion()
        );
        
        plugin.getLanguageManager().sendMultilineMessage(sender, "messages.command.about.header");
        plugin.getLanguageManager().sendMultilineMessage(sender, "messages.command.about.lines", placeholders);
        plugin.getLanguageManager().sendMultilineMessage(sender, "messages.command.about.footer");
        return true;
    }
    
    private boolean executeReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herbalism.command.reload")) {
            plugin.getLanguageManager().sendMessage(sender, "messages.no-permission");
            return true;
        }
        
        try {
            plugin.reloadPlugin();
            plugin.getLanguageManager().sendMessage(sender, "messages.plugin-reloaded");
        } catch (Exception e) {
            MessageUtil.error("Error during plugin reload: " + e.getMessage());
            plugin.getLanguageManager().sendMessage(sender, "messages.errors.unknown-error");
        }
        
        return true;
    }
    
    private boolean executeMenu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getLanguageManager().sendMessage(sender, "messages.player-only");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getGuiManager().openMainMenu(player);
        return true;
    }
    
    private boolean executeStats(CommandSender sender, String[] args) {
        Player targetPlayer;
        
        if (args.length > 1) {
            // Check stats of another player
            if (!sender.hasPermission("herbalism.command.stats.others")) {
                plugin.getLanguageManager().sendMessage(sender, "messages.no-permission");
                return true;
            }
            
            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                    "player", args[1]
                );
                plugin.getLanguageManager().sendMessage(sender, "messages.invalid-player", placeholders);
                return true;
            }
        } else {
            // Check own stats
            if (!(sender instanceof Player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.player-only");
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(targetPlayer);
        if (herbalismPlayer == null) {
            plugin.getLanguageManager().sendMessage(sender, "messages.errors.unknown-error");
            return true;
        }
        
        Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
            "player", herbalismPlayer.getName(),
            "level", herbalismPlayer.getLevel(),
            "max_level", herbalismPlayer.getMaxLevel(),
            "exp", herbalismPlayer.getExperience(),
            "exp_required", herbalismPlayer.getExperienceToNextLevel(),
            "plants", herbalismPlayer.getTotalPlants(),
            "max_plants", herbalismPlayer.getMaxPlants(),
            "active_crops", herbalismPlayer.getActiveCrops(),
            "harvests", herbalismPlayer.getTotalHarvests(),
            "fertilizers_used", herbalismPlayer.getTotalFertilizersUsed()
        );
        
        plugin.getLanguageManager().sendMultilineMessage(sender, "messages.command.stats.header");
        plugin.getLanguageManager().sendMultilineMessage(sender, "messages.command.stats.lines", placeholders);
        plugin.getLanguageManager().sendMultilineMessage(sender, "messages.command.stats.footer");
        
        return true;
    }
    
    private boolean executeExp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herbalism.command.exp")) {
            plugin.getLanguageManager().sendMessage(sender, "messages.no-permission");
            return true;
        }
        
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, "messages.command.exp.usage");
            return true;
        }
        
        String action = args[1].toLowerCase();
        String amountStr = args[2];
        
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                "input", amountStr
            );
            plugin.getLanguageManager().sendMessage(sender, "messages.invalid-number", placeholders);
            return true;
        }
        
        Player targetPlayer;
        if (args.length > 3) {
            targetPlayer = Bukkit.getPlayer(args[3]);
            if (targetPlayer == null) {
                Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                    "player", args[3]
                );
                plugin.getLanguageManager().sendMessage(sender, "messages.invalid-player", placeholders);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.player-only");
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(targetPlayer);
        if (herbalismPlayer == null) {
            plugin.getLanguageManager().sendMessage(sender, "messages.errors.unknown-error");
            return true;
        }
        
        Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
            "player", targetPlayer.getName(),
            "amount", amount
        );
        
        switch (action) {
            case "set":
                herbalismPlayer.setExperience(amount);
                plugin.getLanguageManager().sendMessage(sender, "messages.command.exp.set-sender", placeholders);
                if (!targetPlayer.equals(sender)) {
                    plugin.getLanguageManager().sendMessage(targetPlayer, "messages.command.exp.set-target", placeholders);
                }
                break;
            case "add":
                herbalismPlayer.addExperience(amount);
                plugin.getLanguageManager().sendMessage(sender, "messages.command.exp.add-sender", placeholders);
                if (!targetPlayer.equals(sender)) {
                    plugin.getLanguageManager().sendMessage(targetPlayer, "messages.command.exp.add-target", placeholders);
                }
                break;
            case "take":
                herbalismPlayer.removeExperience(amount);
                plugin.getLanguageManager().sendMessage(sender, "messages.command.exp.take-sender", placeholders);
                if (!targetPlayer.equals(sender)) {
                    plugin.getLanguageManager().sendMessage(targetPlayer, "messages.command.exp.take-target", placeholders);
                }
                break;
            default:
                plugin.getLanguageManager().sendMessage(sender, "messages.command.exp.usage");
                return true;
        }
        
        return true;
    }
    
    private boolean executeLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herbalism.command.level")) {
            plugin.getLanguageManager().sendMessage(sender, "messages.no-permission");
            return true;
        }
        
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, "messages.command.level.usage");
            return true;
        }
        
        String action = args[1].toLowerCase();
        String levelStr = args[2];
        
        int level;
        try {
            level = Integer.parseInt(levelStr);
        } catch (NumberFormatException e) {
            Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                "input", levelStr
            );
            plugin.getLanguageManager().sendMessage(sender, "messages.invalid-number", placeholders);
            return true;
        }
        
        Player targetPlayer;
        if (args.length > 3) {
            targetPlayer = Bukkit.getPlayer(args[3]);
            if (targetPlayer == null) {
                Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                    "player", args[3]
                );
                plugin.getLanguageManager().sendMessage(sender, "messages.invalid-player", placeholders);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.player-only");
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        HerbalismPlayer herbalismPlayer = plugin.getPlayerManager().getPlayer(targetPlayer);
        if (herbalismPlayer == null) {
            plugin.getLanguageManager().sendMessage(sender, "messages.errors.unknown-error");
            return true;
        }
        
        Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
            "player", targetPlayer.getName(),
            "level", level
        );
        
        switch (action) {
            case "set":
                herbalismPlayer.setLevel(level);
                plugin.getLanguageManager().sendMessage(sender, "messages.command.level.set-sender", placeholders);
                if (!targetPlayer.equals(sender)) {
                    plugin.getLanguageManager().sendMessage(targetPlayer, "messages.command.level.set-target", placeholders);
                }
                break;
            case "add":
                herbalismPlayer.setLevel(herbalismPlayer.getLevel() + level);
                plugin.getLanguageManager().sendMessage(sender, "messages.command.level.add-sender", placeholders);
                if (!targetPlayer.equals(sender)) {
                    plugin.getLanguageManager().sendMessage(targetPlayer, "messages.command.level.add-target", placeholders);
                }
                break;
            case "take":
                herbalismPlayer.setLevel(Math.max(1, herbalismPlayer.getLevel() - level));
                plugin.getLanguageManager().sendMessage(sender, "messages.command.level.take-sender", placeholders);
                if (!targetPlayer.equals(sender)) {
                    plugin.getLanguageManager().sendMessage(targetPlayer, "messages.command.level.take-target", placeholders);
                }
                break;
            default:
                plugin.getLanguageManager().sendMessage(sender, "messages.command.level.usage");
                return true;
        }
        
        return true;
    }
    
    private boolean executeLoad(CommandSender sender, String[] args) {
        if (!sender.hasPermission("herbalism.command.load")) {
            plugin.getLanguageManager().sendMessage(sender, "messages.no-permission");
            return true;
        }
        
        if (args.length < 3) {
            plugin.getLanguageManager().sendMessage(sender, "messages.command.load.usage");
            return true;
        }
        
        String type = args[1].toLowerCase();
        String itemId = args[2];
        int amount = 1;
        
        if (args.length > 3) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                    "input", args[3]
                );
                plugin.getLanguageManager().sendMessage(sender, "messages.invalid-number", placeholders);
                return true;
            }
        }
        
        Player targetPlayer;
        if (args.length > 4) {
            targetPlayer = Bukkit.getPlayer(args[4]);
            if (targetPlayer == null) {
                Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                    "player", args[4]
                );
                plugin.getLanguageManager().sendMessage(sender, "messages.invalid-player", placeholders);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                plugin.getLanguageManager().sendMessage(sender, "messages.player-only");
                return true;
            }
            targetPlayer = (Player) sender;
        }
        
        // Handle different item types
        switch (type) {
            case "item":
            case "harvest":
                return handleLoadItem(sender, targetPlayer, itemId, amount);
            case "fertilizer":
                return handleLoadFertilizer(sender, targetPlayer, itemId, amount);
            case "crop":
                return handleLoadCrop(sender, targetPlayer, itemId, amount);
            default:
                Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                    "type", type
                );
                plugin.getLanguageManager().sendMessage(sender, "messages.command.load.invalid-type", placeholders);
                return true;
        }
    }
    
    private boolean handleLoadItem(CommandSender sender, Player targetPlayer, String itemId, int amount) {
        // Try to create item from Material
        try {
            Material material = Material.valueOf(itemId.toUpperCase());
            if (material.isItem()) {
                ItemStack item = new ItemStack(material, amount);
                targetPlayer.getInventory().addItem(item);
                
                Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                    "amount", amount,
                    "item", material.name().toLowerCase().replace("_", " "),
                    "player", targetPlayer.getName()
                );
                plugin.getLanguageManager().sendMessage(sender, "messages.command.load.success", placeholders);
                
                if (!sender.equals(targetPlayer)) {
                    plugin.getLanguageManager().sendMessage(targetPlayer, "messages.command.load.received", placeholders);
                }
                
                return true;
            }
        } catch (IllegalArgumentException e) {
            // Material not found, continue
        }
        
        Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
            "item", itemId
        );
        plugin.getLanguageManager().sendMessage(sender, "messages.command.load.item-not-found", placeholders);
        return true;
    }
    
    private boolean handleLoadFertilizer(CommandSender sender, Player targetPlayer, String itemId, int amount) {
        // Try to get fertilizer from FertilizerManager
        me.devupdates.herbalism.fertilizer.Fertilizer fertilizer = plugin.getFertilizerManager().getFertilizer(itemId);
        if (fertilizer != null) {
            ItemStack item = fertilizer.createItemStack(amount);
            targetPlayer.getInventory().addItem(item);
            
            Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                "amount", amount,
                "item", fertilizer.getName(),
                "player", targetPlayer.getName()
            );
            plugin.getLanguageManager().sendMessage(sender, "messages.command.load.success", placeholders);
            
            if (!sender.equals(targetPlayer)) {
                plugin.getLanguageManager().sendMessage(targetPlayer, "messages.command.load.received", placeholders);
            }
            
            return true;
        }
        
        Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
            "item", itemId
        );
        plugin.getLanguageManager().sendMessage(sender, "messages.command.load.fertilizer-not-found", placeholders);
        return true;
    }
    
    private boolean handleLoadCrop(CommandSender sender, Player targetPlayer, String itemId, int amount) {
        // Try to get crop type from CropManager
        me.devupdates.herbalism.manager.CropManager.CropType cropType = plugin.getCropManager().getCropType(itemId);
        if (cropType != null) {
            ItemStack item = new ItemStack(cropType.getSeedMaterial(), amount);
            targetPlayer.getInventory().addItem(item);
            
            Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
                "amount", amount,
                "item", cropType.getName() + " Seeds",
                "player", targetPlayer.getName()
            );
            plugin.getLanguageManager().sendMessage(sender, "messages.command.load.success", placeholders);
            
            if (!sender.equals(targetPlayer)) {
                plugin.getLanguageManager().sendMessage(targetPlayer, "messages.command.load.received", placeholders);
            }
            
            return true;
        }
        
        Map<String, Object> placeholders = plugin.getLanguageManager().createPlaceholders(
            "item", itemId
        );
        plugin.getLanguageManager().sendMessage(sender, "messages.command.load.crop-not-found", placeholders);
        return true;
    }
} 