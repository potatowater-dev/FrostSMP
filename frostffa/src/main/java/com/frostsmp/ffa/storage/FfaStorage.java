package com.frostsmp.ffa.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class FfaStorage {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration data;

    public FfaStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        file.getParentFile().mkdirs();
        data = YamlConfiguration.loadConfiguration(file);
    }

    public List<String> getRecentMutators() {
        return new ArrayList<>(data.getStringList("recent-mutators"));
    }

    public void pushMutator(String id) {
        List<String> recent = getRecentMutators();
        recent.add(0, id);
        while (recent.size() > 2) {
            recent.remove(recent.size() - 1);
        }
        data.set("recent-mutators", recent);
        save();
    }

    public String getLastSlotKey() {
        return data.getString("last-slot-key", "");
    }

    public void setLastSlotKey(String key) {
        data.set("last-slot-key", key);
        save();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save data.yml: " + ex.getMessage());
        }
    }
}
