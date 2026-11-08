package com.frostsmp.combat.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KillStreakStorage {

    public record Record(UUID id, String name, int current, int best) {
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Record> records = new ConcurrentHashMap<>();

    public KillStreakStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "killstreaks.yml");
    }

    public void load() {
        records.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String name = section.getString(key + ".name", "Unknown");
                int current = Math.max(0, section.getInt(key + ".current", 0));
                int best = Math.max(0, section.getInt(key + ".best", current));
                records.put(id, new Record(id, name, current, best));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Record record : records.values()) {
            String path = "players." + record.id();
            config.set(path + ".name", record.name());
            config.set(path + ".current", record.current());
            config.set(path + ".best", record.best());
        }
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for killstreaks.yml");
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save killstreaks.yml: " + exception.getMessage());
        }
    }

    public void clear() {
        records.clear();
        save();
    }

    public List<Record> all() {
        return List.copyOf(records.values());
    }

    public int best(UUID id) {
        Record record = records.get(id);
        return record == null ? 0 : record.best();
    }

    public String name(UUID id) {
        Record record = records.get(id);
        return record == null ? null : record.name();
    }

    public void update(UUID id, String name, int current, int best) {
        records.put(id, new Record(id, name != null ? name : "Unknown", Math.max(0, current), Math.max(0, best)));
        save();
    }

    public void touchName(UUID id, String name) {
        Record existing = records.get(id);
        if (existing == null) {
            records.put(id, new Record(id, name, 0, 0));
        } else if (!name.equals(existing.name())) {
            records.put(id, new Record(id, name, existing.current(), existing.best()));
        }
    }
}
