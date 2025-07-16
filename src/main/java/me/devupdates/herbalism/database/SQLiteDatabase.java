package me.devupdates.herbalism.database;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.crop.Crop;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.MessageUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SQLiteDatabase implements Database {
    
    private final HerbalismPlugin plugin;
    private final File databaseFile;
    private HikariDataSource dataSource;
    private boolean connected = false;
    
    public SQLiteDatabase(HerbalismPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "herbalism.db");
    }
    
    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Create data folder if it doesn't exist
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                
                // Configure HikariCP for SQLite
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
                config.setDriverClassName("org.sqlite.JDBC");
                config.setMaximumPoolSize(10);
                config.setMinimumIdle(2);
                config.setConnectionTimeout(30000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);
                config.setLeakDetectionThreshold(60000);
                
                // SQLite-specific settings
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                
                dataSource = new HikariDataSource(config);
                connected = true;
                
                MessageUtil.info("Successfully connected to SQLite database: " + databaseFile.getName());
                return true;
                
            } catch (Exception e) {
                MessageUtil.error("Failed to connect to SQLite database: " + e.getMessage());
                connected = false;
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                connected = false;
                MessageUtil.info("Disconnected from SQLite database");
            }
        });
    }
    
    @Override
    public boolean isConnected() {
        return connected && dataSource != null && !dataSource.isClosed();
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("Database is not connected");
        }
        return dataSource.getConnection();
    }
    
    @Override
    public CompletableFuture<Boolean> initializeTables() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                // Create players table
                String playersTable = """
                    CREATE TABLE IF NOT EXISTS herbalism_players (
                        uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        level INTEGER NOT NULL DEFAULT 1,
                        experience BIGINT NOT NULL DEFAULT 0,
                        total_plants INTEGER NOT NULL DEFAULT 0,
                        max_plants INTEGER NOT NULL DEFAULT 0,
                        total_harvests INTEGER NOT NULL DEFAULT 0,
                        total_fertilizers_used INTEGER NOT NULL DEFAULT 0,
                        total_crops_planted INTEGER NOT NULL DEFAULT 0,
                        total_play_time BIGINT NOT NULL DEFAULT 0,
                        first_joined BIGINT NOT NULL DEFAULT 0,
                        last_seen BIGINT NOT NULL DEFAULT 0,
                        active_crops INTEGER NOT NULL DEFAULT 0,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                    """;
                
                // Create crops table
                String cropsTable = """
                    CREATE TABLE IF NOT EXISTS herbalism_crops (
                        id TEXT PRIMARY KEY,
                        crop_type TEXT NOT NULL,
                        owner_id TEXT NOT NULL,
                        owner_name TEXT NOT NULL,
                        world TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        z REAL NOT NULL,
                        plant_time BIGINT NOT NULL,
                        original_duration BIGINT NOT NULL,
                        harvest_time BIGINT NOT NULL,
                        harvested BOOLEAN NOT NULL DEFAULT FALSE,
                        decayed BOOLEAN NOT NULL DEFAULT FALSE,
                        fertilized_by TEXT,
                        growth_modifier REAL NOT NULL DEFAULT 1.0,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (owner_id) REFERENCES herbalism_players(uuid)
                    )
                    """;
                
                // Create indexes
                String[] indexes = {
                    "CREATE INDEX IF NOT EXISTS idx_players_name ON herbalism_players(name)",
                    "CREATE INDEX IF NOT EXISTS idx_players_level ON herbalism_players(level)",
                    "CREATE INDEX IF NOT EXISTS idx_crops_owner ON herbalism_crops(owner_id)",
                    "CREATE INDEX IF NOT EXISTS idx_crops_type ON herbalism_crops(crop_type)",
                    "CREATE INDEX IF NOT EXISTS idx_crops_harvested ON herbalism_crops(harvested)",
                    "CREATE INDEX IF NOT EXISTS idx_crops_location ON herbalism_crops(world, x, y, z)"
                };
                
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(playersTable);
                    stmt.execute(cropsTable);
                    
                    for (String index : indexes) {
                        stmt.execute(index);
                    }
                }
                
                MessageUtil.info("Database tables initialized successfully");
                return true;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to initialize database tables: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> savePlayer(HerbalismPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO herbalism_players 
                (uuid, name, level, experience, total_plants, max_plants, total_harvests, 
                 total_fertilizers_used, total_crops_planted, total_play_time, first_joined, 
                 last_seen, active_crops, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, player.getUUID().toString());
                stmt.setString(2, player.getName());
                stmt.setInt(3, player.getLevel());
                stmt.setLong(4, player.getExperience());
                stmt.setInt(5, player.getTotalPlants());
                stmt.setInt(6, player.getMaxPlants());
                stmt.setInt(7, player.getTotalHarvests());
                stmt.setInt(8, player.getTotalFertilizersUsed());
                stmt.setInt(9, player.getTotalCropsPlanted());
                stmt.setLong(10, player.getTotalPlayTime());
                stmt.setLong(11, player.getFirstJoined());
                stmt.setLong(12, player.getLastSeen());
                stmt.setInt(13, player.getActiveCrops());
                
                stmt.executeUpdate();
                return true;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to save player " + player.getName() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<HerbalismPlayer> loadPlayer(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM herbalism_players WHERE uuid = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    HerbalismPlayer player = new HerbalismPlayer(uuid, name);
                    
                    // Load data from database
                    player.setLevel(rs.getInt("level"));
                    player.setExperience(rs.getLong("experience"));
                    player.setTotalPlants(rs.getInt("total_plants"));
                    player.setMaxPlants(rs.getInt("max_plants"));
                    player.setTotalHarvests(rs.getInt("total_harvests"));
                    player.setTotalFertilizersUsed(rs.getInt("total_fertilizers_used"));
                    player.setTotalCropsPlanted(rs.getInt("total_crops_planted"));
                    player.setTotalPlayTime(rs.getLong("total_play_time"));
                    player.setFirstJoined(rs.getLong("first_joined"));
                    player.setLastSeen(rs.getLong("last_seen"));
                    player.setActiveCrops(rs.getInt("active_crops"));
                    
                    return player;
                } else {
                    // Player doesn't exist, create new one
                    return new HerbalismPlayer(uuid, name);
                }
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to load player " + name + ": " + e.getMessage());
                return new HerbalismPlayer(uuid, name);
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> playerExists(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM herbalism_players WHERE uuid = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                return rs.next();
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to check player existence: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> deletePlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM herbalism_players WHERE uuid = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                int affected = stmt.executeUpdate();
                return affected > 0;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to delete player: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<List<HerbalismPlayer>> getAllPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM herbalism_players ORDER BY level DESC, experience DESC";
            List<HerbalismPlayer> players = new ArrayList<>();
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("name");
                    
                    HerbalismPlayer player = new HerbalismPlayer(uuid, name);
                    player.setLevel(rs.getInt("level"));
                    player.setExperience(rs.getLong("experience"));
                    player.setTotalPlants(rs.getInt("total_plants"));
                    player.setMaxPlants(rs.getInt("max_plants"));
                    player.setTotalHarvests(rs.getInt("total_harvests"));
                    player.setTotalFertilizersUsed(rs.getInt("total_fertilizers_used"));
                    player.setTotalCropsPlanted(rs.getInt("total_crops_planted"));
                    player.setTotalPlayTime(rs.getLong("total_play_time"));
                    player.setFirstJoined(rs.getLong("first_joined"));
                    player.setLastSeen(rs.getLong("last_seen"));
                    player.setActiveCrops(rs.getInt("active_crops"));
                    
                    players.add(player);
                }
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to load all players: " + e.getMessage());
            }
            
            return players;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> updatePlayerStats(UUID uuid, long experience, int level, 
                                                       int harvests, int fertilizersUsed) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                UPDATE herbalism_players 
                SET experience = ?, level = ?, total_harvests = ?, total_fertilizers_used = ?, 
                    updated_at = CURRENT_TIMESTAMP
                WHERE uuid = ?
                """;
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(1, experience);
                stmt.setInt(2, level);
                stmt.setInt(3, harvests);
                stmt.setInt(4, fertilizersUsed);
                stmt.setString(5, uuid.toString());
                
                int affected = stmt.executeUpdate();
                return affected > 0;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to update player stats: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> saveCrop(Crop crop) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO herbalism_crops 
                (id, crop_type, owner_id, owner_name, world, x, y, z, plant_time, original_duration, 
                 harvest_time, harvested, decayed, fertilized_by, growth_modifier, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, crop.getId().toString());
                stmt.setString(2, crop.getCropType());
                stmt.setString(3, crop.getOwnerId().toString());
                stmt.setString(4, crop.getOwnerName());
                stmt.setString(5, crop.getLocation().getWorld().getName());
                stmt.setDouble(6, crop.getLocation().getX());
                stmt.setDouble(7, crop.getLocation().getY());
                stmt.setDouble(8, crop.getLocation().getZ());
                stmt.setLong(9, crop.getPlantTime());
                stmt.setLong(10, crop.getOriginalDuration());
                stmt.setLong(11, crop.getHarvestTime());
                stmt.setBoolean(12, crop.isHarvested());
                stmt.setBoolean(13, crop.isDecayed());
                stmt.setString(14, crop.getFertilizedBy().isEmpty() ? null : 
                    String.join(",", crop.getFertilizedBy().stream().map(UUID::toString).toArray(String[]::new)));
                stmt.setDouble(15, crop.getGrowthModifier());
                
                stmt.executeUpdate();
                return true;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to save crop: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Crop> loadCrop(UUID cropId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM herbalism_crops WHERE id = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, cropId.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return createCropFromResultSet(rs);
                }
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to load crop: " + e.getMessage());
            }
            
            return null;
        });
    }
    
    @Override
    public CompletableFuture<List<Crop>> loadPlayerCrops(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM herbalism_crops WHERE owner_id = ? AND harvested = FALSE";
            List<Crop> crops = new ArrayList<>();
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerId.toString());
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    Crop crop = createCropFromResultSet(rs);
                    if (crop != null) {
                        crops.add(crop);
                    }
                }
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to load player crops: " + e.getMessage());
            }
            
            return crops;
        });
    }
    
    @Override
    public CompletableFuture<List<Crop>> loadAllCrops() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM herbalism_crops WHERE harvested = FALSE";
            List<Crop> crops = new ArrayList<>();
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Crop crop = createCropFromResultSet(rs);
                    if (crop != null) {
                        crops.add(crop);
                    }
                }
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to load all crops: " + e.getMessage());
            }
            
            return crops;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> deleteCrop(UUID cropId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM herbalism_crops WHERE id = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, cropId.toString());
                int affected = stmt.executeUpdate();
                return affected > 0;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to delete crop: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> deletePlayerCrops(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM herbalism_crops WHERE owner_id = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerId.toString());
                int affected = stmt.executeUpdate();
                return affected > 0;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to delete player crops: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> updateCropStatus(UUID cropId, boolean harvested, boolean decayed) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE herbalism_crops SET harvested = ?, decayed = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setBoolean(1, harvested);
                stmt.setBoolean(2, decayed);
                stmt.setString(3, cropId.toString());
                
                int affected = stmt.executeUpdate();
                return affected > 0;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to update crop status: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> executeBatch(List<DatabaseOperation> operations) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                
                for (DatabaseOperation operation : operations) {
                    operation.execute(conn);
                }
                
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to execute batch operations: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> performMaintenance() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Clean up old harvested crops (older than 30 days)
                stmt.execute("DELETE FROM herbalism_crops WHERE harvested = TRUE AND updated_at < datetime('now', '-30 days')");
                
                // Vacuum database to reclaim space
                stmt.execute("VACUUM");
                
                // Analyze tables for query optimization
                stmt.execute("ANALYZE");
                
                MessageUtil.info("Database maintenance completed successfully");
                return true;
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to perform database maintenance: " + e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<DatabaseStats> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                
                long totalPlayers = 0;
                long totalCrops = 0;
                long databaseSize = databaseFile.length();
                
                // Count players
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM herbalism_players")) {
                    if (rs.next()) {
                        totalPlayers = rs.getLong(1);
                    }
                }
                
                // Count crops
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM herbalism_crops")) {
                    if (rs.next()) {
                        totalCrops = rs.getLong(1);
                    }
                }
                
                return new DatabaseStats(totalPlayers, totalCrops, databaseSize, "SQLite");
                
            } catch (SQLException e) {
                MessageUtil.error("Failed to get database stats: " + e.getMessage());
                return new DatabaseStats(0, 0, 0, "SQLite");
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> createBackup(String backupPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File backupFile = new File(backupPath);
                if (backupFile.getParentFile() != null) {
                    backupFile.getParentFile().mkdirs();
                }
                
                // SQLite backup using file copy
                java.nio.file.Files.copy(databaseFile.toPath(), backupFile.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                MessageUtil.info("Database backup created: " + backupPath);
                return true;
                
            } catch (IOException e) {
                MessageUtil.error("Failed to create database backup: " + e.getMessage());
                return false;
            }
        });
    }
    
    private Crop createCropFromResultSet(ResultSet rs) throws SQLException {
        UUID cropId = UUID.fromString(rs.getString("id"));
        String cropType = rs.getString("crop_type");
        UUID ownerId = UUID.fromString(rs.getString("owner_id"));
        String ownerName = rs.getString("owner_name");
        String worldName = rs.getString("world");
        
        // Get world
        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            MessageUtil.warn("World " + worldName + " not found for crop " + cropId);
            return null;
        }
        
        org.bukkit.Location location = new org.bukkit.Location(world, 
            rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"));
        
        long plantTime = rs.getLong("plant_time");
        long originalDuration = rs.getLong("original_duration");
        long harvestTime = rs.getLong("harvest_time");
        boolean harvested = rs.getBoolean("harvested");
        boolean decayed = rs.getBoolean("decayed");
        double growthModifier = rs.getDouble("growth_modifier");
        
        // Create crop using reflection or deserialization
        Map<String, Object> data = new HashMap<>();
        data.put("id", cropId.toString());
        data.put("crop_type", cropType);
        data.put("owner_id", ownerId.toString());
        data.put("owner_name", ownerName);
        data.put("world", worldName);
        data.put("x", location.getX());
        data.put("y", location.getY());
        data.put("z", location.getZ());
        data.put("plant_time", plantTime);
        data.put("original_duration", originalDuration);
        data.put("harvest_time", harvestTime);
        data.put("harvested", harvested);
        data.put("decayed", decayed);
        data.put("growth_modifier", growthModifier);
        
        String fertilizedByStr = rs.getString("fertilized_by");
        if (fertilizedByStr != null && !fertilizedByStr.isEmpty()) {
            List<String> fertilizedBy = Arrays.asList(fertilizedByStr.split(","));
            data.put("fertilized_by", fertilizedBy);
        }
        
        return Crop.deserialize(data);
    }
} 