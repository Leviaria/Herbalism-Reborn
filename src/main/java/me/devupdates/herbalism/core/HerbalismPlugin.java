package me.devupdates.herbalism.core;

import me.devupdates.herbalism.command.HerbalismCommand;
import me.devupdates.herbalism.command.HerbalismTabCompleter;
import me.devupdates.herbalism.config.ConfigManager;
import me.devupdates.herbalism.language.LanguageManager;
import me.devupdates.herbalism.listener.BlockListener;
import me.devupdates.herbalism.manager.AbilityManager;
import me.devupdates.herbalism.manager.CropManager;
import me.devupdates.herbalism.manager.DatabaseManager;
import me.devupdates.herbalism.manager.FertilizerManager;
import me.devupdates.herbalism.manager.GuiManager;
import me.devupdates.herbalism.manager.PlayerManager;
// import me.devupdates.herbalism.manager.PlaceholderManager;
import me.devupdates.herbalism.hologram.HologramManager;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class HerbalismPlugin extends JavaPlugin {

    private static HerbalismPlugin instance;
    
    // Managers
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private PlayerManager playerManager;
    private CropManager cropManager;
    private FertilizerManager fertilizerManager;
    private AbilityManager abilityManager;
    private GuiManager guiManager;
    private DatabaseManager databaseManager;
    private HologramManager hologramManager;
    // private PlaceholderManager placeholderManager;
    
    // Commands
    private HerbalismCommand herbalismCommand;
    private HerbalismTabCompleter herbalismTabCompleter;
    
    // Listeners
    private BlockListener blockListener;
    
    // Scheduled tasks
    private BukkitTask saveTask;
    private BukkitTask cropUpdateTask;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize logger
        MessageUtil.setLogger(getLogger());
        
        MessageUtil.info("Herbalism Reborn v" + getDescription().getVersion() + " is starting...");
        
        if (!initializePlugin()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Start scheduled tasks
        startScheduledTasks();
        
        MessageUtil.info("Herbalism Reborn has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Cancel scheduled tasks
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (cropUpdateTask != null) {
            cropUpdateTask.cancel();
        }
        
        // Shutdown managers
        if (cropManager != null) {
            cropManager.shutdown();
        }
        if (fertilizerManager != null) {
            fertilizerManager.clearAllEffects();
        }
        if (abilityManager != null) {
            abilityManager.clearAllData();
        }
        if (guiManager != null) {
            guiManager.clearAllCooldowns();
        }
        if (databaseManager != null) {
            try {
                databaseManager.shutdown().get();
            } catch (Exception e) {
                MessageUtil.error("Error during database shutdown: " + e.getMessage());
            }
        }
        if (hologramManager != null) {
            hologramManager.shutdown();
        }
        // if (placeholderManager != null) {
        //     placeholderManager.unregisterExpansion();
        // }
        if (playerManager != null) {
            playerManager.shutdown();
        }
        
        // Save configuration files
        if (configManager != null) {
            configManager.saveAllConfigs();
        }
        
        MessageUtil.info("Herbalism Reborn has been disabled!");
        instance = null;
    }
    
    private boolean initializePlugin() {
        try {
            // Initialize configuration
            configManager = new ConfigManager(this);
            
            // Initialize language system
            languageManager = new LanguageManager(this);
            
            // Initialize player manager
            playerManager = new PlayerManager(this);
            
            // Initialize crop manager
            cropManager = new CropManager(this);
            
            // Initialize fertilizer manager
            fertilizerManager = new FertilizerManager(this);
            
            // Initialize ability manager
            abilityManager = new AbilityManager(this);
            
            // Initialize GUI manager
            guiManager = new GuiManager(this);
            
            // Initialize database manager
            databaseManager = new DatabaseManager(this);
            boolean dbInitialized = databaseManager.initialize().get();
            if (!dbInitialized) {
                MessageUtil.error("Failed to initialize database! Plugin will continue without data persistence.");
            }
            
            // Initialize hologram manager
            hologramManager = new HologramManager(this);
            
            // Initialize placeholder manager
            // placeholderManager = new PlaceholderManager(this);
            
            return true;
        } catch (Exception e) {
            MessageUtil.error("Error during plugin initialization: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void registerCommands() {
        // Initialize command handlers
        herbalismCommand = new HerbalismCommand(this);
        herbalismTabCompleter = new HerbalismTabCompleter(this);
        
        // Register herbalism command
        PluginCommand herbalismCmd = getCommand("herbalism");
        if (herbalismCmd != null) {
            herbalismCmd.setExecutor(herbalismCommand);
            herbalismCmd.setTabCompleter(herbalismTabCompleter);
            MessageUtil.info("Herbalism command registered successfully!");
        } else {
            MessageUtil.error("Failed to register herbalism command - check plugin.yml!");
        }
    }
    
    private void registerListeners() {
        // Initialize listeners
        blockListener = new BlockListener(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(blockListener, this);
        
        MessageUtil.info("Event listeners registered successfully!");
    }
    
    private void startScheduledTasks() {
        int saveInterval = configManager.getDatabaseSaveInterval();
        int cropUpdateInterval = configManager.getCropUpdateInterval();
        
        // Schedule periodic player data saving
        saveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerManager != null) {
                playerManager.performPeriodicSave();
            }
        }, 20L * saveInterval, 20L * saveInterval); // Convert seconds to ticks
        
        // Schedule crop updates
        cropUpdateTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (cropManager != null) {
                cropManager.updateCrops();
            }
        }, 20L, cropUpdateInterval); // Run every configured interval in ticks
        
        MessageUtil.info("Scheduled tasks started:");
        MessageUtil.info("- Save interval: " + saveInterval + " seconds");
        MessageUtil.info("- Crop update interval: " + cropUpdateInterval + " ticks");
    }
    
    public static HerbalismPlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
    
    public PlayerManager getPlayerManager() {
        return playerManager;
    }
    
    public CropManager getCropManager() {
        return cropManager;
    }
    
    public FertilizerManager getFertilizerManager() {
        return fertilizerManager;
    }
    
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }
    
    public GuiManager getGuiManager() {
        return guiManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public HologramManager getHologramManager() {
        return hologramManager;
    }
    
    // public PlaceholderManager getPlaceholderManager() {
    //     return placeholderManager;
    // }
    
    public void reloadPlugin() {
        MessageUtil.info("Reloading Herbalism Reborn...");
        
        // Reload configurations
        configManager.reloadConfigs();
        languageManager.reloadLanguage();
        fertilizerManager.reload();
        abilityManager.reload();
        hologramManager.reload();
        // placeholderManager.reload();
        
        // Restart scheduled tasks with new intervals
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (cropUpdateTask != null) {
            cropUpdateTask.cancel();
        }
        startScheduledTasks();
        
        MessageUtil.info("Herbalism Reborn reloaded successfully!");
    }
} 