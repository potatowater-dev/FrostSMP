package com.frostsmp.crates.config;

import com.frostsmp.crates.FrostCratesPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class AutoKeyGrantStorage {

    private final FrostCratesPlugin plugin;
    private final File file;
    private final Map<String, Long> lastGrantMs = new HashMap<>();

    public AutoKeyGrantStorage(FrostCratesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "auto-key-grants.yml");
    }

    public void load() {
        lastGrantMs.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("grants");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            lastGrantMs.put(key, section.getLong(key, 0L));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Long> entry : lastGrantMs.entrySet()) {
            yaml.set("grants." + entry.getKey(), entry.getValue());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save auto-key-grants.yml", e);
        }
    }

    public long getLastGrantMs(UUID playerId, String crateId) {
        return lastGrantMs.getOrDefault(grantKey(playerId, crateId), 0L);
    }

    public void setLastGrantMs(UUID playerId, String crateId, long epochMs) {
        lastGrantMs.put(grantKey(playerId, crateId), epochMs);
    }

    public long getLastBundleGrantMs(UUID playerId, String category, String bundleId) {
        return lastGrantMs.getOrDefault(bundleGrantKey(playerId, category, bundleId), 0L);
    }

    public void setLastBundleGrantMs(UUID playerId, String category, String bundleId, long epochMs) {
        lastGrantMs.put(bundleGrantKey(playerId, category, bundleId), epochMs);
    }

    public void removeCrate(String crateId) {
        String suffix = ":" + crateId.toLowerCase();
        lastGrantMs.keySet().removeIf(key -> key.endsWith(suffix));
        save();
    }

    private String grantKey(UUID playerId, String crateId) {
        return playerId + ":" + crateId.toLowerCase();
    }

    private String bundleGrantKey(UUID playerId, String category, String bundleId) {
        return playerId + ":bundle:" + category.toLowerCase() + ":" + bundleId.toLowerCase();
    }
}
