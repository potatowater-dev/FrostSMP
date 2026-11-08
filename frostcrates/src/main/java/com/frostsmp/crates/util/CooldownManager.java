package com.frostsmp.crates.util;

import com.frostsmp.crates.FrostCratesPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {

    private final FrostCratesPlugin plugin;
    private final File file;
    private final Map<String, Long> expiry = new ConcurrentHashMap<>();
    private boolean dirty;

    public CooldownManager(FrostCratesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cooldowns.yml");
        load();
    }

    public void load() {
        expiry.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (config.getConfigurationSection("cooldowns") == null) {
            return;
        }
        for (String key : config.getConfigurationSection("cooldowns").getKeys(false)) {
            expiry.put(key, config.getLong("cooldowns." + key));
        }
    }

    public void save() {
        if (!dirty) {
            return;
        }
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Long> entry : expiry.entrySet()) {
            config.set("cooldowns." + entry.getKey(), entry.getValue());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
            dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save cooldowns.yml: " + exception.getMessage());
        }
    }

    public boolean isOnCooldown(String key) {
        Long until = expiry.get(key);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            expiry.remove(key);
            dirty = true;
            return false;
        }
        return true;
    }

    public void setCooldown(String key, long durationMs) {
        if (durationMs <= 0L) {
            clear(key);
            return;
        }
        expiry.put(key, System.currentTimeMillis() + durationMs);
        dirty = true;
        save();
    }

    public long getRemainingMs(String key) {
        Long until = expiry.get(key);
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            expiry.remove(key);
            dirty = true;
            return 0L;
        }
        return remaining;
    }

    public void clear(String key) {
        if (expiry.remove(key) != null) {
            dirty = true;
            save();
        }
    }

    public void clearAll() {
        expiry.clear();
        dirty = true;
        save();
    }
}
