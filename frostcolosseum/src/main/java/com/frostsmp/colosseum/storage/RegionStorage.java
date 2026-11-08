package com.frostsmp.colosseum.storage;

import com.frostsmp.colosseum.FrostColosseumPlugin;
import com.frostsmp.colosseum.model.CuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RegionStorage {

    public static final Set<String> REGION_NAMES = Set.of(
            "platform_red", "platform_blue", "arena", "spectate"
    );
    public static final Set<String> POINT_NAMES = Set.of(
            "spawn_red", "spawn_blue", "exit"
    );

    private final FrostColosseumPlugin plugin;
    private final File file;
    private FileConfiguration yaml;

    private Location selectionPos1;
    private Location selectionPos2;

    private final Map<String, CuboidRegion> regions = new LinkedHashMap<>();
    private final Map<String, Location> points = new LinkedHashMap<>();

    public RegionStorage(FrostColosseumPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Could not create regions.yml: " + exception.getMessage());
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        regions.clear();
        points.clear();

        ConfigurationSection regionSec = yaml.getConfigurationSection("regions");
        if (regionSec != null) {
            for (String name : REGION_NAMES) {
                CuboidRegion region = readRegion(regionSec.getConfigurationSection(name));
                if (region != null) {
                    regions.put(name, region);
                }
            }
        }

        ConfigurationSection pointSec = yaml.getConfigurationSection("points");
        if (pointSec != null) {
            for (String name : POINT_NAMES) {
                Location point = readLocation(pointSec.getConfigurationSection(name));
                if (point != null) {
                    points.put(name, point);
                }
            }
        }
    }

    public void save() {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        yaml.set("regions", null);
        yaml.set("points", null);

        for (Map.Entry<String, CuboidRegion> entry : regions.entrySet()) {
            writeRegion("regions." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Location> entry : points.entrySet()) {
            writeLocation("points." + entry.getKey(), entry.getValue());
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save regions.yml: " + exception.getMessage());
        }
    }

    public void setSelectionPos1(Location location) {
        this.selectionPos1 = location == null ? null : location.clone();
    }

    public void setSelectionPos2(Location location) {
        this.selectionPos2 = location == null ? null : location.clone();
    }

    public Location getSelectionPos1() {
        return selectionPos1 == null ? null : selectionPos1.clone();
    }

    public Location getSelectionPos2() {
        return selectionPos2 == null ? null : selectionPos2.clone();
    }

    public boolean hasSelection() {
        return selectionPos1 != null && selectionPos2 != null
                && selectionPos1.getWorld() != null
                && selectionPos2.getWorld() != null
                && selectionPos1.getWorld().equals(selectionPos2.getWorld());
    }

    public boolean setRegionFromSelection(String name) {
        if (!REGION_NAMES.contains(name) || !hasSelection()) {
            return false;
        }
        regions.put(name, new CuboidRegion(selectionPos1, selectionPos2));
        save();
        return true;
    }

    public void setPoint(String name, Location location) {
        if (!POINT_NAMES.contains(name) || location == null) {
            return;
        }
        points.put(name, location.clone());
        save();
    }

    public CuboidRegion getRegion(String name) {
        return regions.get(name);
    }

    public Location getPoint(String name) {
        Location point = points.get(name);
        return point == null ? null : point.clone();
    }

    public boolean isRegionSet(String name) {
        return regions.containsKey(name);
    }

    public boolean isPointSet(String name) {
        return points.containsKey(name);
    }

    public boolean isFullyConfigured() {
        for (String name : REGION_NAMES) {
            if (!regions.containsKey(name)) {
                return false;
            }
        }
        for (String name : POINT_NAMES) {
            if (!points.containsKey(name)) {
                return false;
            }
        }
        return true;
    }

    public List<String> missingSetup() {
        List<String> missing = new ArrayList<>();
        for (String name : REGION_NAMES) {
            if (!regions.containsKey(name)) {
                missing.add(name);
            }
        }
        for (String name : POINT_NAMES) {
            if (!points.containsKey(name)) {
                missing.add(name);
            }
        }
        return missing;
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

    private CuboidRegion readRegion(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        Location min = readLocation(section.getConfigurationSection("min"));
        Location max = readLocation(section.getConfigurationSection("max"));
        if (min == null || max == null) {
            return null;
        }
        try {
            return new CuboidRegion(min, max);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void writeRegion(String path, CuboidRegion region) {
        writeLocation(path + ".min", region.getMinCorner());
        writeLocation(path + ".max", region.getMaxCorner());
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
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
