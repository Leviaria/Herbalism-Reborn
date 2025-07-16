package me.devupdates.herbalism.database;

import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.crop.Crop;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database {
    
    /**
     * Establishes connection to the database
     */
    CompletableFuture<Boolean> connect();
    
    /**
     * Closes the database connection
     */
    CompletableFuture<Void> disconnect();
    
    /**
     * Checks if the database is connected
     */
    boolean isConnected();
    
    /**
     * Gets a connection from the pool
     */
    Connection getConnection() throws SQLException;
    
    /**
     * Initializes database tables
     */
    CompletableFuture<Boolean> initializeTables();
    
    // Player-related methods
    
    /**
     * Saves a player to the database
     */
    CompletableFuture<Boolean> savePlayer(HerbalismPlayer player);
    
    /**
     * Loads a player from the database
     */
    CompletableFuture<HerbalismPlayer> loadPlayer(UUID uuid, String name);
    
    /**
     * Checks if a player exists in the database
     */
    CompletableFuture<Boolean> playerExists(UUID uuid);
    
    /**
     * Deletes a player from the database
     */
    CompletableFuture<Boolean> deletePlayer(UUID uuid);
    
    /**
     * Gets all players from the database
     */
    CompletableFuture<List<HerbalismPlayer>> getAllPlayers();
    
    /**
     * Updates player statistics
     */
    CompletableFuture<Boolean> updatePlayerStats(UUID uuid, long experience, int level, 
                                                int harvests, int fertilizersUsed);
    
    // Crop-related methods
    
    /**
     * Saves a crop to the database
     */
    CompletableFuture<Boolean> saveCrop(Crop crop);
    
    /**
     * Loads a crop from the database
     */
    CompletableFuture<Crop> loadCrop(UUID cropId);
    
    /**
     * Loads all crops for a player
     */
    CompletableFuture<List<Crop>> loadPlayerCrops(UUID playerId);
    
    /**
     * Loads all crops from the database
     */
    CompletableFuture<List<Crop>> loadAllCrops();
    
    /**
     * Deletes a crop from the database
     */
    CompletableFuture<Boolean> deleteCrop(UUID cropId);
    
    /**
     * Deletes all crops for a player
     */
    CompletableFuture<Boolean> deletePlayerCrops(UUID playerId);
    
    /**
     * Updates crop status
     */
    CompletableFuture<Boolean> updateCropStatus(UUID cropId, boolean harvested, boolean decayed);
    
    // Utility methods
    
    /**
     * Executes a batch of operations
     */
    CompletableFuture<Boolean> executeBatch(List<DatabaseOperation> operations);
    
    /**
     * Performs database maintenance (cleanup, optimization)
     */
    CompletableFuture<Boolean> performMaintenance();
    
    /**
     * Gets database statistics
     */
    CompletableFuture<DatabaseStats> getStats();
    
    /**
     * Creates a backup of the database
     */
    CompletableFuture<Boolean> createBackup(String backupPath);
    
    /**
     * Functional interface for database operations
     */
    @FunctionalInterface
    interface DatabaseOperation {
        void execute(Connection connection) throws SQLException;
    }
    
    /**
     * Database statistics class
     */
    class DatabaseStats {
        private final long totalPlayers;
        private final long totalCrops;
        private final long databaseSize;
        private final String databaseType;
        
        public DatabaseStats(long totalPlayers, long totalCrops, long databaseSize, String databaseType) {
            this.totalPlayers = totalPlayers;
            this.totalCrops = totalCrops;
            this.databaseSize = databaseSize;
            this.databaseType = databaseType;
        }
        
        public long getTotalPlayers() { return totalPlayers; }
        public long getTotalCrops() { return totalCrops; }
        public long getDatabaseSize() { return databaseSize; }
        public String getDatabaseType() { return databaseType; }
    }
} 