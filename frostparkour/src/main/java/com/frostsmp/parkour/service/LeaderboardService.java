package com.frostsmp.parkour.service;

import com.frostsmp.parkour.FrostParkourPlugin;
import com.frostsmp.parkour.model.ParkourRecord;
import com.frostsmp.parkour.model.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LeaderboardService {

    private final FrostParkourPlugin plugin;
    private final File file;

    // courseId -> sorted list of records
    private final Map<String, List<ParkourRecord>> leaderboardMap = new ConcurrentHashMap<>();
    // courseId -> (lowercase username -> stats)
    private final Map<String, Map<String, PlayerStats>> statsMap = new ConcurrentHashMap<>();
    // courseId -> (lowercase username -> display name as last seen)
    private final Map<String, Map<String, String>> nameMap = new ConcurrentHashMap<>();

    public LeaderboardService(FrostParkourPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "leaderboards.yml");
    }

    public void load() {
        leaderboardMap.clear();
        statsMap.clear();
        nameMap.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection coursesSec = config.getConfigurationSection("courses");
        if (coursesSec == null) {
            return;
        }

        for (String courseId : coursesSec.getKeys(false)) {
            ConfigurationSection sec = coursesSec.getConfigurationSection(courseId);
            if (sec == null) {
                continue;
            }

            Map<String, PlayerStats> playerStats = new ConcurrentHashMap<>();
            Map<String, String> names = new ConcurrentHashMap<>();
            List<ParkourRecord> records = new ArrayList<>();

            ConfigurationSection statsSec = sec.getConfigurationSection("stats");
            if (statsSec != null) {
                for (String key : statsSec.getKeys(false)) {
                    try {
                        long bestTime = statsSec.getLong(key + ".best-time", -1L);
                        int completions = statsSec.getInt(key + ".completions", 0);
                        int attempts = statsSec.getInt(key + ".attempts", 0);
                        long timestamp = statsSec.getLong(key + ".timestamp", System.currentTimeMillis());
                        String displayName = statsSec.getString(key + ".name");

                        // Migrate old UUID-keyed entries to username keys.
                        String nameKey = resolveNameKey(key, displayName);
                        if (nameKey == null || nameKey.isBlank() || "unknown".equals(nameKey)) {
                            continue;
                        }
                        if (displayName == null || displayName.isBlank() || "Unknown".equalsIgnoreCase(displayName)) {
                            displayName = resolveDisplayName(key, nameKey);
                        }

                        // Prefer keeping the better entry if both UUID and name keys collide after migration.
                        PlayerStats existing = playerStats.get(nameKey);
                        PlayerStats incoming = new PlayerStats(bestTime, completions, attempts);
                        if (existing == null) {
                            playerStats.put(nameKey, incoming);
                            names.put(nameKey, displayName);
                            if (bestTime > 0) {
                                records.add(new ParkourRecord(displayName, bestTime, timestamp));
                            }
                        } else {
                            existing.mergeFrom(incoming);
                            names.put(nameKey, displayName);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // Rebuild sorted records from merged stats
            records.clear();
            for (Map.Entry<String, PlayerStats> entry : playerStats.entrySet()) {
                PlayerStats ps = entry.getValue();
                if (ps.getBestTimeMs() > 0) {
                    String display = names.getOrDefault(entry.getKey(), entry.getKey());
                    records.add(new ParkourRecord(display, ps.getBestTimeMs(), System.currentTimeMillis()));
                }
            }
            Collections.sort(records);
            String courseKey = courseId.toLowerCase(Locale.ROOT);
            leaderboardMap.put(courseKey, records);
            statsMap.put(courseKey, playerStats);
            nameMap.put(courseKey, names);
        }

        // Rewrite file in username format so future loads stay clean.
        save();
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection coursesSec = config.createSection("courses");

        for (String courseId : statsMap.keySet()) {
            ConfigurationSection sec = coursesSec.createSection(courseId);
            ConfigurationSection statsSec = sec.createSection("stats");
            Map<String, PlayerStats> map = statsMap.get(courseId);
            Map<String, String> names = nameMap.getOrDefault(courseId, Map.of());
            Map<String, Long> timestamps = timestampsByName(courseId);
            if (map != null) {
                for (Map.Entry<String, PlayerStats> entry : map.entrySet()) {
                    String nameKey = entry.getKey();
                    PlayerStats ps = entry.getValue();
                    ConfigurationSection pSec = statsSec.createSection(nameKey);
                    pSec.set("name", names.getOrDefault(nameKey, nameKey));
                    pSec.set("best-time", ps.getBestTimeMs());
                    pSec.set("completions", ps.getCompletions());
                    pSec.set("attempts", ps.getAttempts());
                    pSec.set("timestamp", timestamps.getOrDefault(nameKey, System.currentTimeMillis()));
                }
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save leaderboards.yml: " + e.getMessage());
        }
    }

    public PlayerStats getStats(String courseId, String playerName) {
        String courseKey = courseId.toLowerCase(Locale.ROOT);
        String nameKey = normalizeName(playerName);
        Map<String, PlayerStats> map = statsMap.computeIfAbsent(courseKey, k -> new ConcurrentHashMap<>());
        nameMap.computeIfAbsent(courseKey, k -> new ConcurrentHashMap<>())
                .putIfAbsent(nameKey, playerName == null ? nameKey : playerName);
        return map.computeIfAbsent(nameKey, k -> new PlayerStats());
    }

    public void recordAttempt(String courseId, String playerName) {
        rememberName(courseId, playerName);
        getStats(courseId, playerName).incrementAttempts();
    }

    public boolean recordCompletion(String courseId, String playerName, long timeMs) {
        String courseKey = courseId.toLowerCase(Locale.ROOT);
        String nameKey = normalizeName(playerName);
        rememberName(courseId, playerName);

        PlayerStats stats = getStats(courseKey, playerName);
        stats.incrementCompletions();

        boolean isNewBest = false;
        if (stats.getBestTimeMs() <= 0 || timeMs < stats.getBestTimeMs()) {
            stats.setBestTimeMs(timeMs);
            isNewBest = true;

            List<ParkourRecord> records = leaderboardMap.computeIfAbsent(courseKey, k -> new ArrayList<>());
            synchronized (records) {
                records.removeIf(r -> r.nameKey().equals(nameKey));
                records.add(new ParkourRecord(playerName, timeMs, System.currentTimeMillis()));
                Collections.sort(records);
            }
        }

        save();
        return isNewBest;
    }

    public List<ParkourRecord> getTopRecords(String courseId) {
        String key = courseId.toLowerCase(Locale.ROOT);
        List<ParkourRecord> list = leaderboardMap.get(key);
        if (list == null) {
            return Collections.emptyList();
        }
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    public int getRank(String courseId, String playerName) {
        String nameKey = normalizeName(playerName);
        List<ParkourRecord> list = getTopRecords(courseId);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).nameKey().equals(nameKey)) {
                return i + 1;
            }
        }
        return -1;
    }

    public void resetLeaderboard(String courseId) {
        String key = courseId.toLowerCase(Locale.ROOT);
        leaderboardMap.remove(key);
        statsMap.remove(key);
        nameMap.remove(key);
        save();
    }

    private void rememberName(String courseId, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        String courseKey = courseId.toLowerCase(Locale.ROOT);
        nameMap.computeIfAbsent(courseKey, k -> new ConcurrentHashMap<>())
                .put(normalizeName(playerName), playerName);
    }

    private Map<String, Long> timestampsByName(String courseId) {
        Map<String, Long> out = new HashMap<>();
        List<ParkourRecord> records = leaderboardMap.get(courseId);
        if (records == null) {
            return out;
        }
        synchronized (records) {
            for (ParkourRecord record : records) {
                out.put(record.nameKey(), record.timestamp());
            }
        }
        return out;
    }

    private static String normalizeName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "unknown";
        }
        return playerName.toLowerCase(Locale.ROOT);
    }

    private static String resolveNameKey(String rawKey, String storedName) {
        if (storedName != null && !storedName.isBlank() && !"Unknown".equalsIgnoreCase(storedName)) {
            return storedName.toLowerCase(Locale.ROOT);
        }
        try {
            UUID uuid = UUID.fromString(rawKey);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName();
            if (name != null && !name.isBlank()) {
                return name.toLowerCase(Locale.ROOT);
            }
        } catch (IllegalArgumentException ignored) {
            // Already a username key
            return rawKey.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static String resolveDisplayName(String rawKey, String nameKey) {
        try {
            UUID uuid = UUID.fromString(rawKey);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return nameKey;
    }
}
