package com.frostsmp.event.service;

import com.frostsmp.event.FrostEventPlugin;
import com.frostsmp.event.config.PluginConfig;
import com.frostsmp.event.storage.WinnerStorage;
import com.frostsmp.event.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class EventService {

    private final FrostEventPlugin plugin;
    private final PluginConfig config;
    private final WinnerStorage winnerStorage;
    private final LocatorForceService locatorForceService;
    private final SpawnProtectionService spawnProtectionService;

    private final Map<UUID, Integer> kills = new ConcurrentHashMap<>();
    private boolean running;
    private long endsAtMs;
    private BukkitTask endTask;

    public EventService(
            FrostEventPlugin plugin,
            PluginConfig config,
            WinnerStorage winnerStorage,
            LocatorForceService locatorForceService,
            SpawnProtectionService spawnProtectionService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.winnerStorage = winnerStorage;
        this.locatorForceService = locatorForceService;
        this.spawnProtectionService = spawnProtectionService;
    }

    public SpawnProtectionService getSpawnProtection() {
        return spawnProtectionService;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean start(double hours) {
        if (running) {
            return false;
        }
        running = true;
        kills.clear();
        spawnProtectionService.clear();
        long durationMs = Math.round(hours * 60.0 * 60.0 * 1000.0);
        endsAtMs = System.currentTimeMillis() + durationMs;

        setLocatorBar(true);
        locatorForceService.start();
        spawnProtectionService.grantAllOnline(true);

        endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> stop(true), Math.max(1L, durationMs / 50L));

        Bukkit.broadcast(MessageUtil.parse(MessageUtil.format(
                config.getStarted(),
                "hours", formatHours(hours)
        )));
        return true;
    }

    public boolean stop(boolean announce) {
        if (!running) {
            return false;
        }
        running = false;
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
        locatorForceService.stop();
        spawnProtectionService.clear();

        UUID winnerId = null;
        String winnerName = null;
        int best = 0;
        for (Map.Entry<UUID, Integer> entry : kills.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                winnerId = entry.getKey();
            }
        }
        if (winnerId != null) {
            Player online = Bukkit.getPlayer(winnerId);
            winnerName = online != null ? online.getName() : Bukkit.getOfflinePlayer(winnerId).getName();
            if (winnerName == null) {
                winnerName = winnerId.toString();
            }
            winnerStorage.saveWinner(winnerId, winnerName, best);
            if (announce) {
                Bukkit.broadcast(MessageUtil.parse(MessageUtil.format(
                        config.getStopped(),
                        "winner", winnerName,
                        "kills", String.valueOf(best)
                )));
            }
        } else if (announce) {
            Bukkit.broadcast(MessageUtil.parse(config.getStoppedNoKills()));
        }

        kills.clear();
        endsAtMs = 0L;
        return true;
    }

    public void recordKill(Player killer) {
        if (!running || killer == null) {
            return;
        }
        kills.merge(killer.getUniqueId(), 1, Integer::sum);
    }

    public int getKills(UUID playerId) {
        return kills.getOrDefault(playerId, 0);
    }

    public Map<UUID, Integer> getKillsSnapshot() {
        return Collections.unmodifiableMap(kills);
    }

    public long getRemainingMs() {
        if (!running) {
            return 0L;
        }
        return Math.max(0L, endsAtMs - System.currentTimeMillis());
    }

    public String formatRemaining() {
        return formatDuration(getRemainingMs());
    }

    public void shutdown() {
        if (running) {
            stop(false);
        }
        locatorForceService.stop();
    }

    private void setLocatorBar(boolean enabled) {
        for (World world : Bukkit.getWorlds()) {
            try {
                world.setGameRule(GameRule.LOCATOR_BAR, enabled);
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not set locatorBar in " + world.getName() + ": " + exception.getMessage());
            }
        }
    }

    public static String formatHours(double hours) {
        if (Math.abs(hours - Math.rint(hours)) < 0.001) {
            return String.valueOf((long) Math.rint(hours));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", hours);
    }

    public static String formatDuration(long ms) {
        long totalSec = TimeUnit.MILLISECONDS.toSeconds(ms);
        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
