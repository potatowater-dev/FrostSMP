package com.frostsmp.alt.storage;

import com.frostsmp.alt.model.PlayerStatus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerStateStorage {

    public static final class PlayerRecord {
        private PlayerStatus status = PlayerStatus.NORMAL;
        private String lastKnownName = "";
        private int lastKnownPlaytimeMinutes;
        private long sessionStartMs;
        private Integer openCaseId;

        public PlayerStatus getStatus() {
            return status;
        }

        public void setStatus(PlayerStatus status) {
            this.status = status;
        }

        public String getLastKnownName() {
            return lastKnownName;
        }

        public void setLastKnownName(String lastKnownName) {
            this.lastKnownName = lastKnownName;
        }

        public int getLastKnownPlaytimeMinutes() {
            return lastKnownPlaytimeMinutes;
        }

        public void setLastKnownPlaytimeMinutes(int lastKnownPlaytimeMinutes) {
            this.lastKnownPlaytimeMinutes = lastKnownPlaytimeMinutes;
        }

        public long getSessionStartMs() {
            return sessionStartMs;
        }

        public void setSessionStartMs(long sessionStartMs) {
            this.sessionStartMs = sessionStartMs;
        }

        public Integer getOpenCaseId() {
            return openCaseId;
        }

        public void setOpenCaseId(Integer openCaseId) {
            this.openCaseId = openCaseId;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerRecord> records = new HashMap<>();

    public PlayerStateStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
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
                UUID uuid = UUID.fromString(key);
                PlayerRecord record = new PlayerRecord();
                record.setStatus(PlayerStatus.valueOf(section.getString(key + ".status", "NORMAL")));
                record.setLastKnownName(section.getString(key + ".name", ""));
                record.setLastKnownPlaytimeMinutes(section.getInt(key + ".playtime-minutes", 0));
                if (section.contains(key + ".open-case-id")) {
                    record.setOpenCaseId(section.getInt(key + ".open-case-id"));
                }
                records.put(uuid, record);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerRecord> entry : records.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerRecord record = entry.getValue();
            config.set(path + ".status", record.getStatus().name());
            config.set(path + ".name", record.getLastKnownName());
            config.set(path + ".playtime-minutes", record.getLastKnownPlaytimeMinutes());
            if (record.getOpenCaseId() != null) {
                config.set(path + ".open-case-id", record.getOpenCaseId());
            }
        }
        write(config);
    }

    public PlayerRecord getOrCreate(UUID playerId) {
        return records.computeIfAbsent(playerId, ignored -> new PlayerRecord());
    }

    public PlayerRecord get(UUID playerId) {
        return records.get(playerId);
    }

    private void write(FileConfiguration config) {
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                plugin.getLogger().warning("Could not create FrostAlt data folder.");
            }
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save players.yml: " + exception.getMessage());
        }
    }
}
