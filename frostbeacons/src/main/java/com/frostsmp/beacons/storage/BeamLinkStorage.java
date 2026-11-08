package com.frostsmp.beacons.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class BeamLinkStorage {

    private final JavaPlugin plugin;
    private final File file;
    /** beacon key -> end location */
    private final Map<String, Location> ends = new HashMap<>();

    public BeamLinkStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "beams.yml");
    }

    public void load() {
        ends.clear();
        if (!file.exists()) {
            save();
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("beams");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String worldName = section.getString(key + ".world");
            World world = worldName == null ? null : Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            ends.put(key, new Location(
                    world,
                    section.getInt(key + ".x"),
                    section.getInt(key + ".y"),
                    section.getInt(key + ".z")
            ));
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Location> entry : ends.entrySet()) {
            Location end = entry.getValue();
            if (end.getWorld() == null) {
                continue;
            }
            String path = "beams." + entry.getKey();
            config.set(path + ".world", end.getWorld().getName());
            config.set(path + ".x", end.getBlockX());
            config.set(path + ".y", end.getBlockY());
            config.set(path + ".z", end.getBlockZ());
        }
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for beams.yml");
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save beams.yml: " + exception.getMessage());
        }
    }

    public void setEnd(Location beacon, Location end) {
        ends.put(key(beacon), end.clone());
        save();
    }

    public Location getEnd(Location beacon) {
        Location end = ends.get(key(beacon));
        return end == null ? null : end.clone();
    }

    public void remove(Location beacon) {
        if (ends.remove(key(beacon)) != null) {
            save();
        }
    }

    public Map<String, Location> snapshot() {
        return Map.copyOf(ends);
    }

    public static String key(Location location) {
        return Objects.requireNonNull(location.getWorld()).getName()
                + ";" + location.getBlockX()
                + ";" + location.getBlockY()
                + ";" + location.getBlockZ();
    }

    public static String format(Location location) {
        return location.getWorld().getName()
                + " " + location.getBlockX()
                + ", " + location.getBlockY()
                + ", " + location.getBlockZ();
    }
}
