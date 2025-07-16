package me.devupdates.herbalism.manager;

import me.devupdates.herbalism.core.HerbalismPlugin;
import me.devupdates.herbalism.player.HerbalismPlayer;
import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager implements Listener {
    
    private final HerbalismPlugin plugin;
    private final Map<UUID, HerbalismPlayer> players = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSaveTime = new HashMap<>();
    
    public PlayerManager(HerbalismPlugin plugin) {
        this.plugin = plugin;
        
        // Register this manager as a listener for player events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Load online players (in case of plugin reload)
        loadOnlinePlayers();
        
        MessageUtil.info("PlayerManager initialized!");
    }
    
    /**
     * Loads all currently online players into the manager
     */
    private void loadOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            loadPlayer(player.getUniqueId(), player.getName());
        }
    }
    
    /**
     * Gets a HerbalismPlayer by UUID, loading from database if needed
     */
    public HerbalismPlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }
    
    /**
     * Gets a HerbalismPlayer by Player object
     */
    public HerbalismPlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }
    
    /**
     * Gets a HerbalismPlayer by name (only for online players)
     */
    public HerbalismPlayer getPlayer(String name) {
        return players.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Loads a player from the database or creates a new one
     */
    public HerbalismPlayer loadPlayer(UUID uuid, String name) {
        HerbalismPlayer herbalismPlayer = players.get(uuid);
        
        if (herbalismPlayer == null) {
            // Try to load from database
            herbalismPlayer = loadPlayerFromDatabase(uuid, name);
            
            if (herbalismPlayer == null) {
                // Create new player
                herbalismPlayer = new HerbalismPlayer(uuid, name);
                MessageUtil.debug("Created new HerbalismPlayer for " + name);
            } else {
                MessageUtil.debug("Loaded HerbalismPlayer from database for " + name);
            }
            
            players.put(uuid, herbalismPlayer);
        }
        
        return herbalismPlayer;
    }
    
    /**
     * Loads player data from database
     */
    private HerbalismPlayer loadPlayerFromDatabase(UUID uuid, String name) {
        // TODO: Implement database loading
        // For now, return null to create new player
        return null;
    }
    
    /**
     * Saves a player to the database
     */
    public void savePlayer(HerbalismPlayer herbalismPlayer) {
        if (herbalismPlayer == null) return;
        
        // TODO: Implement database saving
        // For now, just update last save time
        lastSaveTime.put(herbalismPlayer.getUUID(), System.currentTimeMillis());
        
        MessageUtil.debug("Saved player data for " + herbalismPlayer.getName());
    }
    
    /**
     * Saves a player by UUID
     */
    public void savePlayer(UUID uuid) {
        HerbalismPlayer herbalismPlayer = players.get(uuid);
        if (herbalismPlayer != null) {
            savePlayer(herbalismPlayer);
        }
    }
    
    /**
     * Saves all currently loaded players
     */
    public void saveAllPlayers() {
        int saved = 0;
        for (HerbalismPlayer herbalismPlayer : players.values()) {
            savePlayer(herbalismPlayer);
            saved++;
        }
        MessageUtil.info("Saved " + saved + " player(s) to database");
    }
    
    /**
     * Unloads a player from memory (saves first)
     */
    public void unloadPlayer(UUID uuid) {
        HerbalismPlayer herbalismPlayer = players.get(uuid);
        if (herbalismPlayer != null) {
            herbalismPlayer.updateLastSeen();
            savePlayer(herbalismPlayer);
            players.remove(uuid);
            lastSaveTime.remove(uuid);
            MessageUtil.debug("Unloaded player " + herbalismPlayer.getName());
        }
    }
    
    /**
     * Gets all currently loaded players
     */
    public Collection<HerbalismPlayer> getAllPlayers() {
        return players.values();
    }
    
    /**
     * Gets all online HerbalismPlayers
     */
    public Collection<HerbalismPlayer> getOnlinePlayers() {
        return players.values().stream()
                .filter(HerbalismPlayer::isOnline)
                .toList();
    }
    
    /**
     * Gets the number of loaded players
     */
    public int getLoadedPlayerCount() {
        return players.size();
    }
    
    /**
     * Checks if a player is loaded
     */
    public boolean isPlayerLoaded(UUID uuid) {
        return players.containsKey(uuid);
    }
    
    /**
     * Adds experience to a player
     */
    public void addExperience(UUID uuid, long amount) {
        HerbalismPlayer herbalismPlayer = players.get(uuid);
        if (herbalismPlayer != null) {
            herbalismPlayer.addExperience(amount);
            
            // Save player data if significant amount of experience was gained
            if (amount >= 100) {
                savePlayer(herbalismPlayer);
            }
        }
    }
    
    /**
     * Adds experience to a player
     */
    public void addExperience(Player player, long amount) {
        addExperience(player.getUniqueId(), amount);
    }
    
    /**
     * Sets a player's level
     */
    public void setLevel(UUID uuid, int level) {
        HerbalismPlayer herbalismPlayer = players.get(uuid);
        if (herbalismPlayer != null) {
            herbalismPlayer.setLevel(level);
            savePlayer(herbalismPlayer);
        }
    }
    
    /**
     * Gets the top players by level
     */
    public Collection<HerbalismPlayer> getTopPlayersByLevel(int limit) {
        return players.values().stream()
                .sorted((a, b) -> Integer.compare(b.getLevel(), a.getLevel()))
                .limit(limit)
                .toList();
    }
    
    /**
     * Gets the top players by experience
     */
    public Collection<HerbalismPlayer> getTopPlayersByExperience(int limit) {
        return players.values().stream()
                .sorted((a, b) -> Long.compare(b.getExperience(), a.getExperience()))
                .limit(limit)
                .toList();
    }
    
    /**
     * Gets the top players by harvests
     */
    public Collection<HerbalismPlayer> getTopPlayersByHarvests(int limit) {
        return players.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotalHarvests(), a.getTotalHarvests()))
                .limit(limit)
                .toList();
    }
    
    /**
     * Performs periodic save for all players
     */
    public void performPeriodicSave() {
        long currentTime = System.currentTimeMillis();
        int saveInterval = plugin.getConfigManager().getDatabaseSaveInterval() * 1000; // Convert to milliseconds
        
        for (HerbalismPlayer herbalismPlayer : players.values()) {
            UUID uuid = herbalismPlayer.getUUID();
            Long lastSave = lastSaveTime.get(uuid);
            
            if (lastSave == null || (currentTime - lastSave) >= saveInterval) {
                savePlayer(herbalismPlayer);
            }
        }
    }
    
    /**
     * Shutdowns the PlayerManager and saves all data
     */
    public void shutdown() {
        MessageUtil.info("Shutting down PlayerManager...");
        saveAllPlayers();
        players.clear();
        lastSaveTime.clear();
        MessageUtil.info("PlayerManager shutdown complete!");
    }
    
    // Event handlers
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        // Load player data
        HerbalismPlayer herbalismPlayer = loadPlayer(uuid, name);
        herbalismPlayer.updateLastSeen();
        
        MessageUtil.debug("Player " + name + " joined - data loaded");
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Update play time before saving
        HerbalismPlayer herbalismPlayer = players.get(uuid);
        if (herbalismPlayer != null) {
            // Calculate session play time (this is a simplified version)
            // In a real implementation, you'd track login time
            herbalismPlayer.addPlayTime(1000); // Add 1 second for now
        }
        
        // Delay unloading to allow other plugins to finish their work
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            unloadPlayer(uuid);
        }, 20L); // 1 second delay
        
        MessageUtil.debug("Player " + player.getName() + " quit - data saved and unloaded");
    }
} 