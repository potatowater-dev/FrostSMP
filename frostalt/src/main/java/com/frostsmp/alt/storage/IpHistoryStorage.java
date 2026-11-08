package com.frostsmp.alt.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class IpHistoryStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, Set<UUID>> ipToPlayers = new HashMap<>();

    public IpHistoryStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ip-history.yml");
    }

    public void load() {
        ipToPlayers.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("ips");
        if (section == null) {
            return;
        }
        for (String ip : section.getKeys(false)) {
            Set<UUID> ids = new HashSet<>();
            for (String raw : section.getStringList(ip)) {
                try {
                    ids.add(UUID.fromString(raw));
                } catch (IllegalArgumentException ignored) {
                }
            }
            ipToPlayers.put(ip, ids);
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Set<UUID>> entry : ipToPlayers.entrySet()) {
            List<String> ids = new ArrayList<>();
            for (UUID id : entry.getValue()) {
                ids.add(id.toString());
            }
            config.set("ips." + entry.getKey(), ids);
        }
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                plugin.getLogger().warning("Could not create FrostAlt data folder.");
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save ip-history.yml: " + exception.getMessage());
        }
    }

    public void record(String ip, UUID playerId) {
        if ("unknown".equals(ip)) {
            return;
        }
        ipToPlayers.computeIfAbsent(ip, ignored -> new HashSet<>()).add(playerId);
        save();
    }
}
