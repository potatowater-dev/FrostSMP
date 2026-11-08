package com.frostsmp.afk.storage;

import com.frostsmp.afk.FrostAfkPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public final class RegionStorage {

    private final FrostAfkPlugin plugin;
    private final File file;
    private FileConfiguration yaml;

    private Location pos1;
    private Location pos2;
    private Location spawn;

    public RegionStorage(FrostAfkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "region.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Could not create region.yml: " + exception.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        pos1 = readLocation("pos1");
        pos2 = readLocation("pos2");
        spawn = readLocation("spawn");
    }

    public void save() {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        writeLocation("pos1", pos1);
        writeLocation("pos2", pos2);
        writeLocation("spawn", spawn);
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save region.yml: " + exception.getMessage());
        }
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null
                && pos1.getWorld() != null
                && pos2.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }

    public boolean contains(Location location) {
        if (!isComplete() || location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().equals(pos1.getWorld())) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean contains(Player player) {
        return contains(player.getLocation());
    }

    public Location getPos1() {
        return pos1 == null ? null : pos1.clone();
    }

    public Location getPos2() {
        return pos2 == null ? null : pos2.clone();
    }

    public Location getSpawn() {
        return spawn == null ? null : spawn.clone();
    }

    public Location getTeleportTarget() {
        if (spawn != null) {
            return spawn.clone();
        }
        if (!isComplete()) {
            return null;
        }
        double x = (pos1.getX() + pos2.getX()) / 2.0;
        double y = Math.min(pos1.getY(), pos2.getY()) + 1.0;
        double z = (pos1.getZ() + pos2.getZ()) / 2.0;
        return new Location(pos1.getWorld(), x, y, z);
    }

    public void setPos1(Location location) {
        this.pos1 = location == null ? null : location.clone();
        save();
    }

    public void setPos2(Location location) {
        this.pos2 = location == null ? null : location.clone();
        save();
    }

    public void setSpawn(Location location) {
        this.spawn = location == null ? null : location.clone();
        save();
    }

    public String formatRegion() {
        if (!isComplete()) {
            return null;
        }
        return format(pos1) + " → " + format(pos2);
    }

    public static String format(Location location) {
        if (location == null || location.getWorld() == null) {
            return "?";
        }
        return location.getWorld().getName()
                + " " + location.getBlockX()
                + "," + location.getBlockY()
                + "," + location.getBlockZ();
    }

    private Location readLocation(String path) {
        String worldName = yaml.getString(path + ".world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = yaml.getDouble(path + ".x");
        double y = yaml.getDouble(path + ".y");
        double z = yaml.getDouble(path + ".z");
        float yaw = (float) yaml.getDouble(path + ".yaw", 0);
        float pitch = (float) yaml.getDouble(path + ".pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void writeLocation(String path, Location location) {
        if (location == null || location.getWorld() == null) {
            yaml.set(path, null);
            return;
        }
        yaml.set(path + ".world", location.getWorld().getName());
        yaml.set(path + ".x", location.getX());
        yaml.set(path + ".y", location.getY());
        yaml.set(path + ".z", location.getZ());
        yaml.set(path + ".yaw", location.getYaw());
        yaml.set(path + ".pitch", location.getPitch());
    }
}
