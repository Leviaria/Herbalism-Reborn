package me.devupdates.herbalism.config;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private final HerbalismPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();
    
    public ConfigManager(HerbalismPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }
    
    private void loadConfigs() {
        // Load main configuration files
        loadConfig("config.yml");
        loadConfig("crops.yml");
        loadConfig("fertilizers.yml");
        loadConfig("abilities.yml");
        loadConfig("items.yml");
        loadConfig("language.yml");
        
        MessageUtil.info("Configuration files loaded successfully!");
    }
    
    private void loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load defaults from plugin jar
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            config.setDefaults(defConfig);
        }
        
        configs.put(fileName, config);
        configFiles.put(fileName, configFile);
    }
    
    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }
    
    public FileConfiguration getMainConfig() {
        return getConfig("config.yml");
    }
    
    public FileConfiguration getCropsConfig() {
        return getConfig("crops.yml");
    }
    
    public FileConfiguration getFertilizersConfig() {
        return getConfig("fertilizers.yml");
    }
    
    public FileConfiguration getAbilitiesConfig() {
        return getConfig("abilities.yml");
    }
    
    public FileConfiguration getItemsConfig() {
        return getConfig("items.yml");
    }
    
    public FileConfiguration getLanguageConfig() {
        return getConfig("language.yml");
    }
    
    public void saveConfig(String fileName) {
        try {
            FileConfiguration config = configs.get(fileName);
            File configFile = configFiles.get(fileName);
            
            if (config != null && configFile != null) {
                config.save(configFile);
            }
        } catch (IOException e) {
            MessageUtil.error("Failed to save config " + fileName + ": " + e.getMessage());
        }
    }
    
    public void saveAllConfigs() {
        for (String fileName : configs.keySet()) {
            saveConfig(fileName);
        }
    }
    
    public void reloadConfigs() {
        configs.clear();
        configFiles.clear();
        loadConfigs();
        MessageUtil.info("Configuration files reloaded!");
    }
    
    // Convenience methods for main config
    public String getLocale() {
        return getMainConfig().getString("locale", "en");
    }
    
    public boolean isDebugMode() {
        return getMainConfig().getBoolean("debug", false);
    }
    
    public int getMaxLevel() {
        return getMainConfig().getInt("herbalism.max-level", 100);
    }
    
    public int getMaxTotalPlants() {
        return getMainConfig().getInt("herbalism.max-total-plants", 100);
    }
    
    public int getBaseTotalPlants() {
        return getMainConfig().getInt("herbalism.base-total-plants", 40);
    }
    
    public double getFormulaExpA() {
        return getMainConfig().getDouble("herbalism.formula-exp-a", 1.0);
    }
    
    public double getFormulaExpB() {
        return getMainConfig().getDouble("herbalism.formula-exp-b", 40.0);
    }
    
    public double getFormulaExpC() {
        return getMainConfig().getDouble("herbalism.formula-exp-c", 400.0);
    }
    
    public String getDatabaseType() {
        return getMainConfig().getString("database.type", "SQLite");
    }
    
    public String getDatabaseHost() {
        return getMainConfig().getString("database.host", "localhost");
    }
    
    public int getDatabasePort() {
        return getMainConfig().getInt("database.port", 3306);
    }
    
    public String getDatabaseName() {
        return getMainConfig().getString("database.name", "herbalism");
    }
    
    public String getDatabaseUsername() {
        return getMainConfig().getString("database.username", "root");
    }
    
    public String getDatabasePassword() {
        return getMainConfig().getString("database.password", "");
    }
    
    public int getDatabaseSaveInterval() {
        return getMainConfig().getInt("database.save-interval", 300);
    }
    
    public int getCropUpdateInterval() {
        return getMainConfig().getInt("crops.update-interval", 20);
    }
    
    public double getCropDecayMultiplier() {
        return getMainConfig().getDouble("crops.decay-multiplier", 2.0);
    }
    
    public boolean isHologramEnabled() {
        return getMainConfig().getBoolean("hologram.enabled", true);
    }
    
    public int getHologramRange() {
        return getMainConfig().getInt("hologram.range", 10);
    }
    
    public int getHologramDuration() {
        return getMainConfig().getInt("hologram.duration", 5);
    }
} 