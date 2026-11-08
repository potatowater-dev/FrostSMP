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

public final class PlayerStatsStorage {

    public record Record(UUID id, String name, int kills, int deaths, long playtimeTicks) {
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Record> records = new ConcurrentHashMap<>();

    public PlayerStatsStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-stats.yml");
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
                int kills = Math.max(0, section.getInt(key + ".kills", 0));
                int deaths = Math.max(0, section.getInt(key + ".deaths", 0));
                long playtime = Math.max(0L, section.getLong(key + ".playtime-ticks", 0L));
                records.put(id, new Record(id, name, kills, deaths, playtime));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Record record : records.values()) {
            String path = "players." + record.id();
            config.set(path + ".name", record.name());
            config.set(path + ".kills", record.kills());
            config.set(path + ".deaths", record.deaths());
            config.set(path + ".playtime-ticks", record.playtimeTicks());
        }
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for player-stats.yml");
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save player-stats.yml: " + exception.getMessage());
        }
    }

    public List<Record> all() {
        return List.copyOf(records.values());
    }

    public Record get(UUID id) {
        return records.get(id);
    }

    public void put(Record record) {
        records.put(record.id(), record);
    }

    public void clear() {
        records.clear();
        save();
    }

    public String name(UUID id) {
        Record record = records.get(id);
        return record == null ? null : record.name();
    }
}
