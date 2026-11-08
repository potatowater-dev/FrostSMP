package com.frostsmp.afk.service;

import com.frostsmp.afk.FrostAfkPlugin;
import com.frostsmp.afk.config.PluginConfig;
import com.frostsmp.afk.storage.RegionStorage;
import com.frostsmp.afk.util.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class AfkService {

    private final FrostAfkPlugin plugin;
    private final PluginConfig config;
    private final RegionStorage regionStorage;
    private final FrostCratesBridge cratesBridge;

    private final Map<UUID, Long> progressMs = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, PendingTeleport> pendingTeleports = new HashMap<>();
    private long lastCratesMissingMs;

    private BukkitTask tickTask;
    private long lastTickMs;

    public AfkService(
            FrostAfkPlugin plugin,
            PluginConfig config,
            RegionStorage regionStorage,
            FrostCratesBridge cratesBridge
    ) {
        this.plugin = plugin;
        this.config = config;
        this.regionStorage = regionStorage;
        this.cratesBridge = cratesBridge;
    }

    public void start() {
        lastTickMs = System.currentTimeMillis();
        scheduleTick();
    }

    public void reload() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bossBars.clear();
        scheduleTick();
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (PendingTeleport pending : pendingTeleports.values()) {
            if (pending.task() != null) {
                pending.task().cancel();
            }
        }
        pendingTeleports.clear();
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bossBars.clear();
        progressMs.clear();
    }

    private void scheduleTick() {
        tickTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::tick,
                config.getTickIntervalTicks(),
                config.getTickIntervalTicks()
        );
    }

    private void tick() {
        long now = System.currentTimeMillis();
        long delta = Math.max(0L, now - lastTickMs);
        lastTickMs = now;

        if (!regionStorage.isComplete()) {
            clearAllBars();
            return;
        }

        long interval = config.getRewardIntervalMs();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (!regionStorage.contains(player)) {
                hideBar(player);
                continue;
            }

            long progress = progressMs.getOrDefault(id, 0L) + delta;
            if (progress >= interval) {
                if (grantReward(player)) {
                    progress = 0L;
                } else {
                    // Stay ready to retry when crates become available
                    progress = interval;
                }
            }
            progressMs.put(id, progress);
            updateBossBar(player, progress, interval);
        }

        Iterator<Map.Entry<UUID, BossBar>> it = bossBars.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BossBar> entry = it.next();
            if (Bukkit.getPlayer(entry.getKey()) == null) {
                it.remove();
                progressMs.remove(entry.getKey());
            }
        }
    }

    private boolean grantReward(Player player) {
        String crateId = config.getCrateId();
        int amount = config.getKeysPerReward();
        if (!cratesBridge.giveKey(player, crateId, amount)) {
            long now = System.currentTimeMillis();
            if (now - lastCratesMissingMs >= 10_000L) {
                lastCratesMissingMs = now;
                MessageUtil.send(player, config.getCratesMissing());
            }
            return false;
        }
        MessageUtil.send(player, config.getReward()
                .replace("{amount}", String.valueOf(amount))
                .replace("{crate}", crateId));
        return true;
    }

    private void updateBossBar(Player player, long progress, long interval) {
        UUID id = player.getUniqueId();
        float ratio = interval <= 0 ? 0f : (float) Math.min(1.0, (double) progress / (double) interval);
        long remainingMs = Math.max(0L, interval - progress);
        var title = MessageUtil.parse(config.getBossBarTitle().replace("{remaining}", formatDuration(remainingMs)));

        BossBar bar = bossBars.get(id);
        if (bar == null) {
            bar = BossBar.bossBar(
                    title,
                    ratio,
                    toAdventureColor(config.getBossBarColor()),
                    toAdventureOverlay(config.getBossBarStyle())
            );
            bossBars.put(id, bar);
            player.showBossBar(bar);
        } else {
            bar.name(title);
            bar.progress(Math.max(0f, Math.min(1f, ratio)));
        }
    }

    private static BossBar.Color toAdventureColor(org.bukkit.boss.BarColor color) {
        return switch (color) {
            case PINK -> BossBar.Color.PINK;
            case BLUE -> BossBar.Color.BLUE;
            case RED -> BossBar.Color.RED;
            case GREEN -> BossBar.Color.GREEN;
            case YELLOW -> BossBar.Color.YELLOW;
            case PURPLE -> BossBar.Color.PURPLE;
            case WHITE -> BossBar.Color.WHITE;
        };
    }

    private static BossBar.Overlay toAdventureOverlay(org.bukkit.boss.BarStyle style) {
        return switch (style) {
            case SOLID -> BossBar.Overlay.PROGRESS;
            case SEGMENTED_6 -> BossBar.Overlay.NOTCHED_6;
            case SEGMENTED_10 -> BossBar.Overlay.NOTCHED_10;
            case SEGMENTED_12 -> BossBar.Overlay.NOTCHED_12;
            case SEGMENTED_20 -> BossBar.Overlay.NOTCHED_20;
        };
    }

    public static String formatDuration(long ms) {
        long totalSec = TimeUnit.MILLISECONDS.toSeconds(ms);
        long minutes = totalSec / 60;
        long seconds = totalSec % 60;
        if (minutes >= 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return hours + "h " + minutes + "m";
        }
        return minutes + "m " + seconds + "s";
    }

    private void hideBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private void clearAllBars() {
        for (Map.Entry<UUID, BossBar> entry : bossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        bossBars.clear();
    }

    public boolean startTeleport(Player player) {
        if (!regionStorage.isComplete()) {
            MessageUtil.send(player, config.getRegionIncomplete());
            return false;
        }
        Location spawn = regionStorage.getTeleportTarget();
        if (spawn == null) {
            MessageUtil.send(player, config.getRegionIncomplete());
            return false;
        }
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            MessageUtil.send(player, config.getAfkAlready());
            return false;
        }

        int delaySec = config.getTeleportDelaySeconds();
        Location from = player.getLocation().clone();
        MessageUtil.send(player, config.getAfkStart().replace("{seconds}", String.valueOf(delaySec)));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingTeleports.remove(player.getUniqueId());
            if (!player.isOnline()) {
                return;
            }
            player.teleport(spawn);
            MessageUtil.send(player, config.getAfkTeleported());
        }, delaySec * 20L);

        pendingTeleports.put(player.getUniqueId(), new PendingTeleport(from, task));
        return true;
    }

    public void cancelTeleport(Player player, boolean moved) {
        PendingTeleport pending = pendingTeleports.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (pending.task() != null) {
            pending.task().cancel();
        }
        MessageUtil.send(player, moved ? config.getAfkCancelledMove() : config.getAfkCancelled());
    }

    public void onMove(Player player, Location to) {
        PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        Location from = pending.from();
        if (to.getWorld() == null || from.getWorld() == null || !to.getWorld().equals(from.getWorld())) {
            cancelTeleport(player, true);
            return;
        }
        if (to.getBlockX() != from.getBlockX()
                || to.getBlockY() != from.getBlockY()
                || to.getBlockZ() != from.getBlockZ()) {
            cancelTeleport(player, true);
        }
    }

    public void onQuit(Player player) {
        cancelTeleportQuiet(player);
        hideBar(player);
        progressMs.remove(player.getUniqueId());
    }

    private void cancelTeleportQuiet(Player player) {
        PendingTeleport pending = pendingTeleports.remove(player.getUniqueId());
        if (pending != null && pending.task() != null) {
            pending.task().cancel();
        }
    }

    private record PendingTeleport(Location from, BukkitTask task) {
    }
}
