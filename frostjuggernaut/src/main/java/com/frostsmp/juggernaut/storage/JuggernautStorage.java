package com.frostsmp.juggernaut.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Disk state that must outlive a restart: the PvP flag we overrode.
 */
public final class JuggernautStorage {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public JuggernautStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        data = YamlConfiguration.loadConfiguration(file);
        // Clear any leftover mace queue from older versions
        if (data.contains("pending-rewards")) {
            data.set("pending-rewards", null);
            save();
        }
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save data.yml: " + ex.getMessage());
        }
    }

    public String getPvpRestoreWorld() {
        return data.getString("pvp-restore.world");
    }

    public boolean getPvpRestoreValue() {
        return data.getBoolean("pvp-restore.value", false);
    }

    public void setPvpRestore(String worldName, boolean previousValue) {
        data.set("pvp-restore.world", worldName);
        data.set("pvp-restore.value", previousValue);
        save();
    }

    public void clearPvpRestore() {
        data.set("pvp-restore", null);
        save();
    }
}
