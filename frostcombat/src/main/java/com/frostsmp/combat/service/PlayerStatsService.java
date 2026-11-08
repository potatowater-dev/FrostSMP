package com.frostsmp.combat.service;

import com.frostsmp.combat.storage.PlayerStatsStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lifetime kills / deaths / playtime for hologram leaderboards (asleaderboard replacement).
 * Reset via /frostcombat resetstats. Does not re-import Bukkit stats after a reset.
 */
public final class PlayerStatsService {

    public enum Metric {
        KILLS,
        DEATHS,
        PLAYTIME
    }

    public record Entry(UUID id, String name, long value) {
    }

    private final PlayerStatsStorage storage;
    private final AtomicBoolean seedFromBukkit;
    private final Map<UUID, PlayerStatsStorage.Record> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStartMs = new ConcurrentHashMap<>();
    private final Map<Metric, List<Entry>> leaderboardCache = new EnumMap<>(Metric.class);
    private final Map<Metric, Long> leaderboardBuiltAt = new EnumMap<>(Metric.class);
    private static final long LEADERBOARD_TTL_MS = 2_000L;

    public PlayerStatsService(PlayerStatsStorage storage, boolean seedFromBukkit) {
        this.storage = storage;
        this.seedFromBukkit = new AtomicBoolean(seedFromBukkit);
        for (PlayerStatsStorage.Record record : storage.all()) {
            cache.put(record.id(), record);
        }
    }

    public void save() {
        flushOnlinePlaytime();
        for (PlayerStatsStorage.Record record : cache.values()) {
            storage.put(record);
        }
        storage.save();
    }

    /**
     * Wipes all tracked kills/deaths/playtime. Disables Bukkit re-seed so values stay at zero
     * until new combat/playtime is earned.
     */
    public int resetAll() {
        int before = cache.size();
        seedFromBukkit.set(false);
        sessionStartMs.clear();
        cache.clear();
        storage.clear();
        invalidateLeaderboards();
        long now = System.currentTimeMillis();
        for (Player online : Bukkit.getOnlinePlayers()) {
            sessionStartMs.put(online.getUniqueId(), now);
            // Fresh zero rows so personal placeholders resolve to 0, not stale Bukkit stats.
            put(new PlayerStatsStorage.Record(online.getUniqueId(), online.getName(), 0, 0, 0L));
        }
        storage.save();
        return before;
    }

    public void handleJoin(Player player) {
        UUID id = player.getUniqueId();
        PlayerStatsStorage.Record existing = cache.get(id);
        if (existing == null) {
            if (seedFromBukkit.get()) {
                int kills = safeStat(player, Statistic.PLAYER_KILLS);
                int deaths = safeStat(player, Statistic.DEATHS);
                long playtime = safeStat(player, Statistic.PLAY_ONE_MINUTE);
                put(new PlayerStatsStorage.Record(id, player.getName(), kills, deaths, playtime));
            } else {
                put(new PlayerStatsStorage.Record(id, player.getName(), 0, 0, 0L));
            }
        } else if (!player.getName().equals(existing.name())) {
            put(new PlayerStatsStorage.Record(id, player.getName(), existing.kills(), existing.deaths(), existing.playtimeTicks()));
        }
        sessionStartMs.put(id, System.currentTimeMillis());
    }

    public void handleQuit(Player player) {
        flushPlaytime(player.getUniqueId());
        sessionStartMs.remove(player.getUniqueId());
        save();
    }

    public void recordKill(Player killer) {
        if (killer == null) {
            return;
        }
        PlayerStatsStorage.Record record = ensure(killer.getUniqueId(), killer.getName());
        put(new PlayerStatsStorage.Record(
                record.id(), killer.getName(), record.kills() + 1, record.deaths(), record.playtimeTicks()
        ));
        invalidateLeaderboards();
    }

    public void recordDeath(Player victim) {
        if (victim == null) {
            return;
        }
        PlayerStatsStorage.Record record = ensure(victim.getUniqueId(), victim.getName());
        put(new PlayerStatsStorage.Record(
                record.id(), victim.getName(), record.kills(), record.deaths() + 1, record.playtimeTicks()
        ));
        invalidateLeaderboards();
    }

    public void tickPlaytime() {
        long now = System.currentTimeMillis();
        for (Player online : Bukkit.getOnlinePlayers()) {
            UUID id = online.getUniqueId();
            Long start = sessionStartMs.get(id);
            if (start == null) {
                sessionStartMs.put(id, now);
                ensure(id, online.getName());
                continue;
            }
            long elapsedMs = now - start;
            if (elapsedMs < 5_000L) {
                continue;
            }
            long addTicks = elapsedMs / 50L;
            if (addTicks <= 0L) {
                continue;
            }
            PlayerStatsStorage.Record record = ensure(id, online.getName());
            put(new PlayerStatsStorage.Record(
                    id, online.getName(), record.kills(), record.deaths(), record.playtimeTicks() + addTicks
            ));
            sessionStartMs.put(id, start + addTicks * 50L);
        }
        invalidateLeaderboards();
    }

    public long value(Metric metric, UUID playerId) {
        if (playerId == null) {
            return 0L;
        }
        if (metric == Metric.PLAYTIME) {
            flushPlaytime(playerId);
        }
        PlayerStatsStorage.Record record = cache.get(playerId);
        if (record == null) {
            return 0L;
        }
        return switch (metric) {
            case KILLS -> record.kills();
            case DEATHS -> record.deaths();
            case PLAYTIME -> record.playtimeTicks();
        };
    }

    public Entry top(Metric metric, int rank) {
        if (rank < 1) {
            return null;
        }
        List<Entry> board = leaderboard(metric);
        if (rank > board.size()) {
            return null;
        }
        return board.get(rank - 1);
    }

    public int place(Metric metric, UUID playerId) {
        if (playerId == null || value(metric, playerId) <= 0L) {
            return 0;
        }
        List<Entry> board = leaderboard(metric);
        for (int i = 0; i < board.size(); i++) {
            if (board.get(i).id().equals(playerId)) {
                return i + 1;
            }
        }
        return 0;
    }

    public List<Entry> leaderboard(Metric metric) {
        long now = System.currentTimeMillis();
        Long builtAt = leaderboardBuiltAt.get(metric);
        List<Entry> cached = leaderboardCache.get(metric);
        if (cached != null && builtAt != null && now - builtAt < LEADERBOARD_TTL_MS) {
            return cached;
        }
        if (metric == Metric.PLAYTIME) {
            flushOnlinePlaytime();
        }
        List<Entry> entries = new ArrayList<>();
        for (PlayerStatsStorage.Record record : cache.values()) {
            long value = switch (metric) {
                case KILLS -> record.kills();
                case DEATHS -> record.deaths();
                case PLAYTIME -> record.playtimeTicks();
            };
            if (value <= 0L) {
                continue;
            }
            entries.add(new Entry(record.id(), record.name(), value));
        }
        entries.sort(Comparator
                .comparingLong(Entry::value).reversed()
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        List<Entry> frozen = List.copyOf(entries);
        leaderboardCache.put(metric, frozen);
        leaderboardBuiltAt.put(metric, now);
        return frozen;
    }

    public static String formatPlaytime(long ticks) {
        long totalSeconds = Math.max(0L, ticks) / 20L;
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        if (days > 0L) {
            return days + "d " + hours + "h " + minutes + "m";
        }
        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public static String formatValue(Metric metric, long value) {
        if (metric == Metric.PLAYTIME) {
            return formatPlaytime(value);
        }
        return String.format(Locale.US, "%,d", value);
    }

    private void flushOnlinePlaytime() {
        for (UUID id : List.copyOf(sessionStartMs.keySet())) {
            flushPlaytime(id);
        }
    }

    private void flushPlaytime(UUID id) {
        Long start = sessionStartMs.get(id);
        if (start == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsedMs = now - start;
        if (elapsedMs < 50L) {
            return;
        }
        long addTicks = elapsedMs / 50L;
        Player online = Bukkit.getPlayer(id);
        String name = online != null ? online.getName() : storage.name(id);
        PlayerStatsStorage.Record record = ensure(id, name != null ? name : "Unknown");
        put(new PlayerStatsStorage.Record(
                id, record.name(), record.kills(), record.deaths(), record.playtimeTicks() + addTicks
        ));
        sessionStartMs.put(id, now);
    }

    private PlayerStatsStorage.Record ensure(UUID id, String name) {
        PlayerStatsStorage.Record existing = cache.get(id);
        if (existing != null) {
            return existing;
        }
        PlayerStatsStorage.Record created = new PlayerStatsStorage.Record(id, name != null ? name : "Unknown", 0, 0, 0L);
        put(created);
        return created;
    }

    private void put(PlayerStatsStorage.Record record) {
        cache.put(record.id(), record);
        storage.put(record);
    }

    private void invalidateLeaderboards() {
        leaderboardCache.clear();
        leaderboardBuiltAt.clear();
    }

    private static int safeStat(OfflinePlayer player, Statistic statistic) {
        try {
            return player.getStatistic(statistic);
        } catch (IllegalArgumentException | UnsupportedOperationException exception) {
            return 0;
        }
    }
}
