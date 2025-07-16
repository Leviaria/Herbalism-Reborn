package me.devupdates.herbalism.manager;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.database.Database;
import me.devupdates.herbalism.database.SQLiteDatabase;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.crop.Crop;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {
    
    private final HerbalismPlugin plugin;
    private Database database;
    private final Map<UUID, HerbalismPlayer> playerCache = new ConcurrentHashMap<>();
    private final Map<UUID, Crop> cropCache = new ConcurrentHashMap<>();
    private final Set<UUID> pendingPlayerSaves = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingCropSaves = ConcurrentHashMap.newKeySet();
    
    private BukkitTask saveTask;
    private BukkitTask maintenanceTask;
    private boolean initialized = false;
    
    public DatabaseManager(HerbalismPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initializes the database connection
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Determine database type from config
                String databaseType = plugin.getConfigManager().getDatabaseType();
                
                switch (databaseType.toUpperCase()) {
                    case "SQLITE":
                        database = new SQLiteDatabase(plugin);
                        break;
                    case "MYSQL":
                        // TODO: Implement MySQL support
                        MessageUtil.warn("MySQL support not implemented yet, falling back to SQLite");
                        database = new SQLiteDatabase(plugin);
                        break;
                    default:
                        MessageUtil.warn("Unknown database type: " + databaseType + ", using SQLite");
                        database = new SQLiteDatabase(plugin);
                        break;
                }
                
                // Connect to database
                boolean connected = database.connect().get();
                if (!connected) {
                    MessageUtil.error("Failed to connect to database");
                    return false;
                }
                
                // Initialize tables
                boolean tablesCreated = database.initializeTables().get();
                if (!tablesCreated) {
                    MessageUtil.error("Failed to initialize database tables");
                    return false;
                }
                
                // Start scheduled tasks
                startScheduledTasks();
                
                initialized = true;
                MessageUtil.info("Database manager initialized successfully");
                return true;
                
            } catch (Exception e) {
                MessageUtil.error("Failed to initialize database: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Shuts down the database connection
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            if (!initialized) return;
            
            try {
                // Cancel scheduled tasks
                if (saveTask != null) {
                    saveTask.cancel();
                }
                if (maintenanceTask != null) {
                    maintenanceTask.cancel();
                }
                
                // Save all pending data
                saveAllPendingData().get(30, TimeUnit.SECONDS);
                
                // Disconnect from database
                database.disconnect().get(10, TimeUnit.SECONDS);
                
                initialized = false;
                MessageUtil.info("Database manager shut down successfully");
                
            } catch (Exception e) {
                MessageUtil.error("Error during database shutdown: " + e.getMessage());
            }
        });
    }
    
    /**
     * Saves a player to the database
     */
    public CompletableFuture<Boolean> savePlayer(HerbalismPlayer player) {
        if (!initialized) {
            return CompletableFuture.completedFuture(false);
        }
        
        playerCache.put(player.getUUID(), player);
        pendingPlayerSaves.add(player.getUUID());
        
        return database.savePlayer(player).thenApply(success -> {
            if (success) {
                pendingPlayerSaves.remove(player.getUUID());
            }
            return success;
        });
    }
    
    /**
     * Loads a player from the database
     */
    public CompletableFuture<HerbalismPlayer> loadPlayer(UUID uuid, String name) {
        if (!initialized) {
            return CompletableFuture.completedFuture(new HerbalismPlayer(uuid, name));
        }
        
        // Check cache first
        HerbalismPlayer cachedPlayer = playerCache.get(uuid);
        if (cachedPlayer != null) {
            return CompletableFuture.completedFuture(cachedPlayer);
        }
        
        return database.loadPlayer(uuid, name).thenApply(player -> {
            playerCache.put(uuid, player);
            return player;
        });
    }
    
    /**
     * Saves a crop to the database
     */
    public CompletableFuture<Boolean> saveCrop(Crop crop) {
        if (!initialized) {
            return CompletableFuture.completedFuture(false);
        }
        
        cropCache.put(crop.getId(), crop);
        pendingCropSaves.add(crop.getId());
        
        return database.saveCrop(crop).thenApply(success -> {
            if (success) {
                pendingCropSaves.remove(crop.getId());
            }
            return success;
        });
    }
    
    /**
     * Loads a crop from the database
     */
    public CompletableFuture<Crop> loadCrop(UUID cropId) {
        if (!initialized) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Check cache first
        Crop cachedCrop = cropCache.get(cropId);
        if (cachedCrop != null) {
            return CompletableFuture.completedFuture(cachedCrop);
        }
        
        return database.loadCrop(cropId).thenApply(crop -> {
            if (crop != null) {
                cropCache.put(cropId, crop);
            }
            return crop;
        });
    }
    
    /**
     * Loads all crops from the database
     */
    public CompletableFuture<List<Crop>> loadAllCrops() {
        if (!initialized) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        return database.loadAllCrops().thenApply(crops -> {
            // Update cache
            for (Crop crop : crops) {
                cropCache.put(crop.getId(), crop);
            }
            return crops;
        });
    }
    
    /**
     * Loads all crops for a specific player
     */
    public CompletableFuture<List<Crop>> loadPlayerCrops(UUID playerId) {
        if (!initialized) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        return database.loadPlayerCrops(playerId).thenApply(crops -> {
            // Update cache
            for (Crop crop : crops) {
                cropCache.put(crop.getId(), crop);
            }
            return crops;
        });
    }
    
    /**
     * Deletes a crop from the database
     */
    public CompletableFuture<Boolean> deleteCrop(UUID cropId) {
        if (!initialized) {
            return CompletableFuture.completedFuture(false);
        }
        
        cropCache.remove(cropId);
        pendingCropSaves.remove(cropId);
        
        return database.deleteCrop(cropId);
    }
    
    /**
     * Updates crop status
     */
    public CompletableFuture<Boolean> updateCropStatus(UUID cropId, boolean harvested, boolean decayed) {
        if (!initialized) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Update cache
        Crop cachedCrop = cropCache.get(cropId);
        if (cachedCrop != null) {
            cachedCrop.setHarvested(harvested);
            cachedCrop.setDecayed(decayed);
        }
        
        return database.updateCropStatus(cropId, harvested, decayed);
    }
    
    /**
     * Gets database statistics
     */
    public CompletableFuture<Database.DatabaseStats> getStats() {
        if (!initialized) {
            return CompletableFuture.completedFuture(
                new Database.DatabaseStats(0, 0, 0, "None"));
        }
        
        return database.getStats();
    }
    
    /**
     * Creates a backup of the database
     */
    public CompletableFuture<Boolean> createBackup() {
        if (!initialized) {
            return CompletableFuture.completedFuture(false);
        }
        
        String backupPath = plugin.getDataFolder() + "/backups/backup_" + 
                           System.currentTimeMillis() + ".db";
        
        return database.createBackup(backupPath);
    }
    
    /**
     * Performs database maintenance
     */
    public CompletableFuture<Boolean> performMaintenance() {
        if (!initialized) {
            return CompletableFuture.completedFuture(false);
        }
        
        return database.performMaintenance();
    }
    
    /**
     * Saves all pending data immediately
     */
    public CompletableFuture<Boolean> saveAllPendingData() {
        if (!initialized) {
            return CompletableFuture.completedFuture(false);
        }
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        // Save pending players
        for (UUID playerId : new HashSet<>(pendingPlayerSaves)) {
            HerbalismPlayer player = playerCache.get(playerId);
            if (player != null) {
                futures.add(database.savePlayer(player));
            }
        }
        
        // Save pending crops
        for (UUID cropId : new HashSet<>(pendingCropSaves)) {
            Crop crop = cropCache.get(cropId);
            if (crop != null) {
                futures.add(database.saveCrop(crop));
            }
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    pendingPlayerSaves.clear();
                    pendingCropSaves.clear();
                    return true;
                });
    }
    
    /**
     * Starts scheduled tasks for automatic saving and maintenance
     */
    private void startScheduledTasks() {
        int saveInterval = plugin.getConfigManager().getDatabaseSaveInterval();
        
        // Auto-save task
        saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!pendingPlayerSaves.isEmpty() || !pendingCropSaves.isEmpty()) {
                    saveAllPendingData().thenAccept(success -> {
                        if (success) {
                            MessageUtil.debug("Auto-save completed successfully");
                        } else {
                            MessageUtil.warn("Auto-save failed");
                        }
                    });
                }
            }
        }.runTaskTimerAsynchronously(plugin, saveInterval * 20L, saveInterval * 20L);
        
        // Maintenance task (daily)
        maintenanceTask = new BukkitRunnable() {
            @Override
            public void run() {
                performMaintenance().thenAccept(success -> {
                    if (success) {
                        MessageUtil.debug("Database maintenance completed");
                    } else {
                        MessageUtil.warn("Database maintenance failed");
                    }
                });
            }
        }.runTaskTimerAsynchronously(plugin, 24 * 60 * 60 * 20L, 24 * 60 * 60 * 20L); // 24 hours
    }
    
    /**
     * Removes a player from the cache
     */
    public void removePlayerFromCache(UUID playerId) {
        playerCache.remove(playerId);
        pendingPlayerSaves.remove(playerId);
    }
    
    /**
     * Removes a crop from the cache
     */
    public void removeCropFromCache(UUID cropId) {
        cropCache.remove(cropId);
        pendingCropSaves.remove(cropId);
    }
    
    /**
     * Gets the underlying database instance
     */
    public Database getDatabase() {
        return database;
    }
    
    /**
     * Checks if the database is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Gets cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("playerCacheSize", playerCache.size());
        stats.put("cropCacheSize", cropCache.size());
        stats.put("pendingPlayerSaves", pendingPlayerSaves.size());
        stats.put("pendingCropSaves", pendingCropSaves.size());
        return stats;
    }
} 