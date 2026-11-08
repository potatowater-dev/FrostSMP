package com.frostsmp.combat.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CombatLogStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Set<UUID> pending = new HashSet<>();

    public CombatLogStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "combat-loggers.yml");
    }

    public void load() {
        pending.clear();
        if (!file.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> ids = config.getStringList("combat-loggers");
        for (String id : ids) {
            try {
                pending.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        config.set("combat-loggers", pending.stream().map(UUID::toString).toList());
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                plugin.getLogger().warning("Could not create data folder for combat-loggers.yml");
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save combat-loggers.yml: " + exception.getMessage());
        }
    }

    public void mark(UUID playerId) {
        if (pending.add(playerId)) {
            save();
        }
    }

    public boolean consume(UUID playerId) {
        if (!pending.remove(playerId)) {
            return false;
        }
        save();
        return true;
    }
}
