package me.devupdates.herbalism.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class MessageUtil {
    
    private static final String PREFIX = "&8[&2Herbalism&8] &r";
    private static Logger logger;
    
    public static void setLogger(Logger logger) {
        MessageUtil.logger = logger;
    }
    
    public static void info(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }
    
    public static void warning(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }
    
    public static void error(String message) {
        if (logger != null) {
            logger.severe(message);
        }
    }
    
    public static void debug(String message) {
        if (logger != null) {
            logger.info("[DEBUG] " + message);
        }
    }
    
    public static void warn(String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }
    
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(colorize(PREFIX + message));
    }
    
    public static void sendMessage(CommandSender sender, String message, Object... args) {
        sendMessage(sender, String.format(message, args));
    }
    
    public static void sendError(CommandSender sender, String message) {
        sender.sendMessage(colorize(PREFIX + "&c" + message));
    }
    
    public static void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(colorize(PREFIX + "&a" + message));
    }
    
    public static void sendWarning(CommandSender sender, String message) {
        sender.sendMessage(colorize(PREFIX + "&e" + message));
    }
} 