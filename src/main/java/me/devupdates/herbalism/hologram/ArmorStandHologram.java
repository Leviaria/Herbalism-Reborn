package me.devupdates.herbalism.hologram;

import me.devupdates.herbalism.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ArmorStandHologram {
    
    private final Location location;
    private final List<String> lines;
    private final List<ArmorStand> armorStands;
    private final Set<UUID> visibleTo;
    private boolean destroyed = false;
    
    public ArmorStandHologram(Location location, List<String> lines) {
        this.location = location.clone();
        this.lines = new ArrayList<>(lines);
        this.armorStands = new ArrayList<>();
        this.visibleTo = new HashSet<>();
        createHologram();
    }
    
    private void createHologram() {
        try {
            MessageUtil.debug("Creating ArmorStand hologram with " + lines.size() + " lines");
            
            // Verify world exists
            if (location.getWorld() == null) {
                MessageUtil.error("World is null for hologram location: " + location);
                return;
            }
            
            // Create armor stands for each line
            for (int i = 0; i < lines.size(); i++) {
                Location armorStandLocation = location.clone().add(0, (lines.size() - i - 1) * 0.3, 0);
                
                MessageUtil.debug("Creating armor stand " + i + " at location: " + armorStandLocation);
                
                ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(armorStandLocation, EntityType.ARMOR_STAND);
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                armorStand.setCanPickupItems(false);
                armorStand.setCustomNameVisible(true);
                armorStand.setCustomName(MessageUtil.colorize(lines.get(i)));
                armorStand.setMarker(true);
                armorStand.setInvulnerable(true);
                armorStand.setCollidable(false);
                armorStand.setSmall(true);
                
                armorStands.add(armorStand);
                MessageUtil.debug("Created armor stand " + i + " with text: " + lines.get(i));
            }
            
            MessageUtil.debug("Successfully created " + armorStands.size() + " armor stands");
            
        } catch (Exception e) {
            MessageUtil.error("Failed to create ArmorStand hologram: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void showToPlayer(Player player) {
        if (destroyed) return;
        
        MessageUtil.debug("Showing hologram to player: " + player.getName());
        visibleTo.add(player.getUniqueId());
        
        // ArmorStands are visible to all players by default
        // We just track which players "should" see it
    }
    
    public void hideFromPlayer(Player player) {
        if (destroyed) return;
        
        visibleTo.remove(player.getUniqueId());
        
        // ArmorStands remain visible to all players
        // We just track which players "should" see it
    }
    
    public void updateLines(List<String> newLines) {
        if (destroyed) return;
        
        // Remove old armor stands
        for (ArmorStand stand : armorStands) {
            stand.remove();
        }
        armorStands.clear();
        
        // Update lines
        lines.clear();
        lines.addAll(newLines);
        
        // Create new armor stands
        createHologram();
    }
    
    public Location getLocation() {
        return location.clone();
    }
    
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }
    
    public boolean isVisibleTo(Player player) {
        return visibleTo.contains(player.getUniqueId());
    }
    
    public boolean isVisibleTo(UUID playerId) {
        return visibleTo.contains(playerId);
    }
    
    public int getViewerCount() {
        return visibleTo.size();
    }
    
    public Set<UUID> getViewers() {
        return new HashSet<>(visibleTo);
    }
    
    public void destroy() {
        if (destroyed) return;
        
        MessageUtil.debug("Destroying hologram with " + armorStands.size() + " armor stands");
        
        // Remove all armor stands
        for (ArmorStand stand : armorStands) {
            stand.remove();
        }
        
        armorStands.clear();
        visibleTo.clear();
        destroyed = true;
        
        MessageUtil.debug("Hologram destroyed successfully");
    }
    
    public boolean isDestroyed() {
        return destroyed;
    }
    
    public List<ArmorStand> getArmorStands() {
        return new ArrayList<>(armorStands);
    }
} 