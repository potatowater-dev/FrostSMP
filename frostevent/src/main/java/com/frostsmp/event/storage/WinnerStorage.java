package com.frostsmp.event.storage;

import com.frostsmp.event.FrostEventPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

public final class WinnerStorage {

    public record Winner(UUID uuid, String name, int kills, long endedAtEpochMs) {
    }

    private final FrostEventPlugin plugin;
    private final File file;
    private Winner lastWinner;

    public WinnerStorage(FrostEventPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "winners.yml");
    }

    public void load() {
        lastWinner = null;
        if (!file.exists()) {
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String uuidRaw = yaml.getString("last.uuid");
        if (uuidRaw == null) {
            return;
        }
        try {
            lastWinner = new Winner(
                    UUID.fromString(uuidRaw),
                    yaml.getString("last.name", "Unknown"),
                    yaml.getInt("last.kills", 0),
                    yaml.getLong("last.ended-at", Instant.now().toEpochMilli())
            );
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid winner UUID in winners.yml");
        }
    }

    public void saveWinner(UUID uuid, String name, int kills) {
        lastWinner = new Winner(uuid, name, kills, Instant.now().toEpochMilli());
        FileConfiguration yaml = new YamlConfiguration();
        yaml.set("last.uuid", uuid.toString());
        yaml.set("last.name", name);
        yaml.set("last.kills", kills);
        yaml.set("last.ended-at", lastWinner.endedAtEpochMs());
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save winners.yml: " + exception.getMessage());
        }
    }

    public Winner getLastWinner() {
        return lastWinner;
    }
}
