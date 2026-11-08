package com.frostsmp.crates.config;

import com.frostsmp.crates.FrostCratesPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class KeyStorage {

    private final FrostCratesPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, Integer>> keys = new HashMap<>();

    public KeyStorage(FrostCratesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "keys.yml");
    }

    public void load() {
        keys.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            Map<String, Integer> playerKeys = new HashMap<>();
            var crateSection = players.getConfigurationSection(uuidStr);
            if (crateSection == null) {
                continue;
            }
            for (String crateId : crateSection.getKeys(false)) {
                playerKeys.put(crateId.toLowerCase(), crateSection.getInt(crateId, 0));
            }
            keys.put(uuid, playerKeys);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Integer>> entry : keys.entrySet()) {
            String path = "players." + entry.getKey();
            for (Map.Entry<String, Integer> keyEntry : entry.getValue().entrySet()) {
                if (keyEntry.getValue() > 0) {
                    yaml.set(path + "." + keyEntry.getKey(), keyEntry.getValue());
                }
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save keys.yml", e);
        }
    }

    public int get(UUID playerId, String crateId) {
        Map<String, Integer> playerKeys = keys.get(playerId);
        if (playerKeys == null) {
            return 0;
        }
        return playerKeys.getOrDefault(crateId.toLowerCase(), 0);
    }

    public void set(UUID playerId, String crateId, int amount) {
        set(playerId, crateId, amount, true);
    }

    public void set(UUID playerId, String crateId, int amount, boolean saveImmediately) {
        keys.computeIfAbsent(playerId, ignored -> new HashMap<>());
        if (amount <= 0) {
            keys.get(playerId).remove(crateId.toLowerCase());
            if (keys.get(playerId).isEmpty()) {
                keys.remove(playerId);
            }
        } else {
            keys.get(playerId).put(crateId.toLowerCase(), amount);
        }
        if (saveImmediately) {
            save();
        }
    }

    public Map<String, Integer> getAll(UUID playerId) {
        Map<String, Integer> playerKeys = keys.get(playerId);
        if (playerKeys == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(playerKeys));
    }
}
