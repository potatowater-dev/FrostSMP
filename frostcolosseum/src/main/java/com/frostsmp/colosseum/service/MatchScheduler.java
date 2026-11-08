package com.frostsmp.colosseum.service;

import com.frostsmp.colosseum.FrostColosseumPlugin;
import com.frostsmp.colosseum.config.PluginConfig;
import org.bukkit.scheduler.BukkitTask;

public final class MatchScheduler {

    private final FrostColosseumPlugin plugin;
    private final PluginConfig config;
    private final MatchService matchService;
    private final QueueService queueService;

    private BukkitTask intervalTask;
    private BukkitTask queueScanTask;
    private long nextMatchAtMs;

    public MatchScheduler(
            FrostColosseumPlugin plugin,
            PluginConfig config,
            MatchService matchService,
            QueueService queueService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.matchService = matchService;
        this.queueService = queueService;
    }

    public void start() {
        stop();
        resetInterval();
        intervalTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (System.currentTimeMillis() < nextMatchAtMs) {
                return;
            }
            matchService.tryStart(false);
            resetInterval();
        }, 20L, 20L);

        queueScanTask = plugin.getServer().getScheduler().runTaskTimer(plugin, queueService::scanStandingPlayers, 40L, 40L);
    }

    public void stop() {
        if (intervalTask != null) {
            intervalTask.cancel();
            intervalTask = null;
        }
        if (queueScanTask != null) {
            queueScanTask.cancel();
            queueScanTask = null;
        }
    }

    public void resetInterval() {
        nextMatchAtMs = System.currentTimeMillis() + config.getIntervalMinutes() * 60_000L;
    }

    public long getSecondsUntilNext() {
        return Math.max(0, (nextMatchAtMs - System.currentTimeMillis() + 999) / 1000);
    }

    public String formatCountdown() {
        long total = getSecondsUntilNext();
        long minutes = total / 60;
        long seconds = total % 60;
        return minutes + "m " + seconds + "s";
    }
}
