package me.devupdates.herbalism.language;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {
    
    private final HerbalismPlugin plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String prefix;
    
    public LanguageManager(HerbalismPlugin plugin) {
        this.plugin = plugin;
        loadLanguage();
    }
    
    public void loadLanguage() {
        FileConfiguration langConfig = plugin.getConfigManager().getLanguageConfig();
        prefix = MessageUtil.colorize(langConfig.getString("messages.prefix", "&8[&2Herbalism&8] &r"));
        
        // Clear existing messages
        messages.clear();
        
        // Load all messages recursively
        loadMessagesRecursively(langConfig.getConfigurationSection("messages"), "messages");
        
        MessageUtil.info("Language system loaded with " + messages.size() + " messages!");
        
        // Debug: Print all loaded messages
        if (plugin.getConfigManager().isDebugMode()) {
            MessageUtil.debug("Loaded messages:");
            for (Map.Entry<String, String> entry : messages.entrySet()) {
                MessageUtil.debug("  " + entry.getKey() + " = " + entry.getValue());
            }
        }
    }
    
    private void loadMessagesRecursively(org.bukkit.configuration.ConfigurationSection section, String path) {
        if (section == null) {
            MessageUtil.debug("Section is null for path: " + path);
            return;
        }
        
        MessageUtil.debug("Loading messages from section: " + path);
        
        for (String key : section.getKeys(false)) {
            String fullPath = path + "." + key;
            
            if (section.isConfigurationSection(key)) {
                // Recursive call for nested sections
                MessageUtil.debug("  Found subsection: " + fullPath);
                loadMessagesRecursively(section.getConfigurationSection(key), fullPath);
            } else if (section.isList(key)) {
                // Handle lists (like help command lines)
                List<String> list = section.getStringList(key);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    sb.append(list.get(i));
                    if (i < list.size() - 1) {
                        sb.append("\n");
                    }
                }
                messages.put(fullPath, sb.toString());
                MessageUtil.debug("  Loaded list message: " + fullPath);
            } else {
                // Handle single strings
                String value = section.getString(key);
                messages.put(fullPath, value);
                MessageUtil.debug("  Loaded message: " + fullPath + " = " + value);
            }
        }
    }
    
    public String getMessage(String key) {
        String message = messages.get(key);
        if (message == null) {
            MessageUtil.debug("Message not found: " + key);
            MessageUtil.debug("Available messages: " + messages.keySet());
            return "§cMissing message: " + key;
        }
        return message;
    }
    
    public String getMessage(String key, Map<String, Object> placeholders) {
        String message = getMessage(key);
        
        if (placeholders != null) {
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = String.valueOf(entry.getValue());
                message = message.replace(placeholder, value);
            }
        }
        
        return MessageUtil.colorize(message);
    }
    
    public String getFormattedMessage(String key) {
        return MessageUtil.colorize(prefix + getMessage(key));
    }
    
    public String getFormattedMessage(String key, Map<String, Object> placeholders) {
        return MessageUtil.colorize(prefix + getMessage(key, placeholders));
    }
    
    public void sendMessage(CommandSender sender, String key) {
        sender.sendMessage(getFormattedMessage(key));
    }
    
    public void sendMessage(CommandSender sender, String key, Map<String, Object> placeholders) {
        sender.sendMessage(getFormattedMessage(key, placeholders));
    }
    
    public void sendRawMessage(CommandSender sender, String key) {
        sender.sendMessage(MessageUtil.colorize(getMessage(key)));
    }
    
    public void sendRawMessage(CommandSender sender, String key, Map<String, Object> placeholders) {
        sender.sendMessage(getMessage(key, placeholders));
    }
    
    public void sendMultilineMessage(CommandSender sender, String key) {
        String message = getMessage(key);
        if (message.contains("\n")) {
            String[] lines = message.split("\n");
            for (String line : lines) {
                sender.sendMessage(MessageUtil.colorize(line));
            }
        } else {
            sender.sendMessage(MessageUtil.colorize(message));
        }
    }
    
    public void sendMultilineMessage(CommandSender sender, String key, Map<String, Object> placeholders) {
        String message = getMessage(key, placeholders);
        if (message.contains("\n")) {
            String[] lines = message.split("\n");
            for (String line : lines) {
                sender.sendMessage(line);
            }
        } else {
            sender.sendMessage(message);
        }
    }
    
    public void sendTitle(Player player, String titleKey, String subtitleKey) {
        String title = MessageUtil.colorize(getMessage(titleKey));
        String subtitle = MessageUtil.colorize(getMessage(subtitleKey));
        
        player.sendTitle(title, subtitle, 10, 70, 20);
    }
    
    public void sendTitle(Player player, String titleKey, String subtitleKey, Map<String, Object> placeholders) {
        String title = getMessage(titleKey, placeholders);
        String subtitle = getMessage(subtitleKey, placeholders);
        
        player.sendTitle(title, subtitle, 10, 70, 20);
    }
    
    public void sendActionBar(Player player, String key) {
        String message = MessageUtil.colorize(getMessage(key));
        player.sendActionBar(message);
    }
    
    public void sendActionBar(Player player, String key, Map<String, Object> placeholders) {
        String message = getMessage(key, placeholders);
        player.sendActionBar(message);
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void reloadLanguage() {
        messages.clear();
        loadLanguage();
    }
    
    // Convenience methods for common placeholders
    public Map<String, Object> createPlaceholders() {
        return new HashMap<>();
    }
    
    public Map<String, Object> createPlaceholders(String key, Object value) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put(key, value);
        return placeholders;
    }
    
    public Map<String, Object> createPlaceholders(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put(key1, value1);
        placeholders.put(key2, value2);
        return placeholders;
    }
    
    public Map<String, Object> createPlaceholders(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put(key1, value1);
        placeholders.put(key2, value2);
        placeholders.put(key3, value3);
        return placeholders;
    }
    
    // General method for creating placeholders with variable arguments
    public Map<String, Object> createPlaceholders(Object... keyValuePairs) {
        Map<String, Object> placeholders = new HashMap<>();
        
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be even in number");
        }
        
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = String.valueOf(keyValuePairs[i]);
            Object value = keyValuePairs[i + 1];
            placeholders.put(key, value);
        }
        
        return placeholders;
    }
    
    // Builder-style method for creating placeholders
    public PlaceholderBuilder placeholder() {
        return new PlaceholderBuilder();
    }
    
    public static class PlaceholderBuilder {
        private final Map<String, Object> placeholders = new HashMap<>();
        
        public PlaceholderBuilder add(String key, Object value) {
            placeholders.put(key, value);
            return this;
        }
        
        public Map<String, Object> build() {
            return new HashMap<>(placeholders);
        }
    }
} 