package com.frost.plots.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Plot {

    private int id;
    private UUID owner;
    private String worldName;
    private double centerX;
    private double centerY;
    private double centerZ;
    private Double spawnX;
    private Double spawnY;
    private Double spawnZ;
    private float spawnYaw;
    private float spawnPitch;
    private int tierIndex;
    private PlotPrivacy privacy;
    private final Map<UUID, PlotRole> members = new ConcurrentHashMap<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getCenterX() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public void setCenterZ(double centerZ) {
        this.centerZ = centerZ;
    }

    public int getBaseY() {
        return (int) Math.floor(centerY);
    }

    public int getCenterBlockX() {
        return (int) Math.floor(centerX);
    }

    public int getCenterBlockZ() {
        return (int) Math.floor(centerZ);
    }

    public World resolveWorld() {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        World exact = Bukkit.getWorld(worldName);
        if (exact != null) {
            return exact;
        }
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().equalsIgnoreCase(worldName)) {
                return world;
            }
        }
        return null;
    }

    public boolean worldMatches(String otherWorldName) {
        return worldName != null && otherWorldName != null && worldName.equalsIgnoreCase(otherWorldName);
    }

    public Location getCenterLocation() {
        World world = resolveWorld();
        if (world == null) {
            return null;
        }
        return new Location(world, centerX, centerY, centerZ);
    }

    public Location getSpawnLocation() {
        World world = resolveWorld();
        if (world == null) {
            return null;
        }
        if (spawnX == null || spawnY == null || spawnZ == null) {
            return new Location(world, centerX + 0.5, centerY + 1.0, centerZ + 0.5, spawnYaw, spawnPitch);
        }
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    public void setSpawn(Location location) {
        this.spawnX = location.getX();
        this.spawnY = location.getY();
        this.spawnZ = location.getZ();
        this.spawnYaw = location.getYaw();
        this.spawnPitch = location.getPitch();
        if (location.getWorld() != null) {
            this.worldName = location.getWorld().getName();
        }
    }

    public Double getSpawnX() {
        return spawnX;
    }

    public void setSpawnX(Double spawnX) {
        this.spawnX = spawnX;
    }

    public Double getSpawnY() {
        return spawnY;
    }

    public void setSpawnY(Double spawnY) {
        this.spawnY = spawnY;
    }

    public Double getSpawnZ() {
        return spawnZ;
    }

    public void setSpawnZ(Double spawnZ) {
        this.spawnZ = spawnZ;
    }

    public float getSpawnYaw() {
        return spawnYaw;
    }

    public void setSpawnYaw(float spawnYaw) {
        this.spawnYaw = spawnYaw;
    }

    public float getSpawnPitch() {
        return spawnPitch;
    }

    public void setSpawnPitch(float spawnPitch) {
        this.spawnPitch = spawnPitch;
    }

    public int getTierIndex() {
        return tierIndex;
    }

    public void setTierIndex(int tierIndex) {
        this.tierIndex = tierIndex;
    }

    public PlotPrivacy getPrivacy() {
        return privacy;
    }

    public void setPrivacy(PlotPrivacy privacy) {
        this.privacy = privacy;
    }

    public Map<UUID, PlotRole> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public void setMembers(Map<UUID, PlotRole> members) {
        this.members.clear();
        if (members != null) {
            this.members.putAll(members);
        }
    }

    public PlotRole getRole(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (uuid.equals(owner)) {
            return null;
        }
        return members.get(uuid);
    }

    public boolean isOwner(UUID uuid) {
        return uuid != null && uuid.equals(owner);
    }

    public boolean isListed(UUID uuid) {
        return uuid != null && members.containsKey(uuid);
    }

    public boolean isMemberOrOwner(UUID uuid) {
        return isOwner(uuid) || isListed(uuid);
    }

    public void putMember(UUID uuid, PlotRole role) {
        members.put(uuid, role);
    }

    public PlotRole removeMember(UUID uuid) {
        return members.remove(uuid);
    }

    public String regionId() {
        return "frostplot_" + id;
    }

    public boolean contains(Location location, int size) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!worldMatches(location.getWorld().getName())) {
            return false;
        }
        int minX = minBlockX(size);
        int maxX = maxBlockX(size);
        int minZ = minBlockZ(size);
        int maxZ = maxBlockZ(size);
        int bx = location.getBlockX();
        int bz = location.getBlockZ();
        return bx >= minX && bx <= maxX && bz >= minZ && bz <= maxZ;
    }

    public int minBlockX(int size) {
        return getCenterBlockX() - Math.max(1, size) / 2;
    }

    public int maxBlockX(int size) {
        return minBlockX(size) + Math.max(1, size) - 1;
    }

    public int minBlockZ(int size) {
        return getCenterBlockZ() - Math.max(1, size) / 2;
    }

    public int maxBlockZ(int size) {
        return minBlockZ(size) + Math.max(1, size) - 1;
    }

    public boolean containsVolume(Location location, int size, int minY, int maxY) {
        if (!contains(location, size)) {
            return false;
        }
        int y = location.getBlockY();
        return y >= minY && y <= maxY;
    }

    public boolean isPlayerInside(Player player, int size) {
        return player != null && contains(player.getLocation(), size);
    }
}
