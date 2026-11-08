package com.frostsmp.crates.config;

import com.frostsmp.crates.FrostCratesPlugin;
import com.frostsmp.crates.model.PlacedCrate;
import com.frostsmp.crates.util.LocationKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class PlacedCrateStorage {

    private final FrostCratesPlugin plugin;
    private final File file;
    private final Map<String, PlacedCrate> byLocation = new HashMap<>();
    private final Map<String, PlacedCrate> byId = new HashMap<>();

    public PlacedCrateStorage(FrostCratesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locations.yml");
    }

    public void load() {
        byLocation.clear();
        byId.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("crates");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            String worldName = section.getString(id + ".world");
            int x = section.getInt(id + ".x");
            int y = section.getInt(id + ".y");
            int z = section.getInt(id + ".z");
            String definition = section.getString(id + ".definition");
            World world = Bukkit.getWorld(worldName);
            if (world == null || definition == null) {
                continue;
            }
            Location location = new Location(world, x, y, z);
            PlacedCrate crate = new PlacedCrate(id, location, definition.toLowerCase());
            byLocation.put(LocationKey.from(location), crate);
            byId.put(id, crate);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlacedCrate crate : byId.values()) {
            String path = "crates." + crate.getId();
            yaml.set(path + ".world", crate.getLocation().getWorld().getName());
            yaml.set(path + ".x", crate.getLocation().getBlockX());
            yaml.set(path + ".y", crate.getLocation().getBlockY());
            yaml.set(path + ".z", crate.getLocation().getBlockZ());
            yaml.set(path + ".definition", crate.getDefinitionId());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save locations.yml", e);
        }
    }

    public PlacedCrate getAt(Location location) {
        return byLocation.get(LocationKey.from(location));
    }

    public PlacedCrate getById(String id) {
        return byId.get(id);
    }

    public Collection<PlacedCrate> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int countForDefinition(String definitionId) {
        return (int) byId.values().stream()
                .filter(crate -> crate.getDefinitionId().equalsIgnoreCase(definitionId))
                .count();
    }

    public PlacedCrate register(Location location, String definitionId) {
        String id = "crate_" + UUID.randomUUID().toString().substring(0, 8);
        PlacedCrate crate = new PlacedCrate(id, location, definitionId.toLowerCase());
        byLocation.put(LocationKey.from(location), crate);
        byId.put(id, crate);
        save();
        return crate;
    }

    public boolean unregister(Location location) {
        PlacedCrate crate = byLocation.remove(LocationKey.from(location));
        if (crate == null) {
            return false;
        }
        byId.remove(crate.getId());
        save();
        return true;
    }

    public java.util.List<PlacedCrate> unregisterAllForDefinition(String definitionId) {
        java.util.List<PlacedCrate> removed = new java.util.ArrayList<>();
        for (PlacedCrate crate : new java.util.ArrayList<>(byId.values())) {
            if (!crate.getDefinitionId().equalsIgnoreCase(definitionId)) {
                continue;
            }
            byLocation.remove(LocationKey.from(crate.getLocation()));
            byId.remove(crate.getId());
            removed.add(crate);
        }
        if (!removed.isEmpty()) {
            save();
        }
        return removed;
    }
}
