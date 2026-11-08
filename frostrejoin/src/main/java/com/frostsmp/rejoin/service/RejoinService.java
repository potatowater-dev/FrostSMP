package com.frostsmp.rejoin.service;

import com.frostsmp.rejoin.FrostRejoinPlugin;
import com.frostsmp.rejoin.config.PluginConfig;
import com.frostsmp.rejoin.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RejoinService {

    public enum State {
        IDLE,
        WAITING_DOWN,
        WAITING_UP,
        STABLE,
        TRANSFERRING
    }

    private final FrostRejoinPlugin plugin;
    private final PluginConfig config;

    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private BukkitTask pollTask;
    private BukkitTask stableTask;
    private State state = State.IDLE;
    private long startedAtMs;
    private boolean transferLocked;

    public RejoinService(FrostRejoinPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void stop() {
        cancelTasks();
        state = State.IDLE;
        transferLocked = false;
    }

    public void clearAll() {
        stop();
        pending.clear();
    }

    public void forceNow() {
        cancelTasks();
        transferLocked = false;
        MessageUtil.broadcast(config.getForced());
        // Force: transfer everyone currently online (and any pending)
        for (Player player : Bukkit.getOnlinePlayers()) {
            pending.add(player.getUniqueId());
        }
        transferPending();
    }

    public State getState() {
        return state;
    }

    public int getPendingCount() {
        // Only count players who are still online
        int count = 0;
        for (UUID id : pending) {
            if (Bukkit.getPlayer(id) != null) {
                count++;
            }
        }
        return count;
    }

    public void onPlayerJoin(Player player) {
        if (state == State.WAITING_UP) {
            pending.add(player.getUniqueId());
            MessageUtil.send(player, config.getWaiting());
            plugin.getLogger().info("Queued " + player.getName() + " for rejoin (main is currently offline).");
        } else if (state == State.STABLE) {
            pending.add(player.getUniqueId());
            MessageUtil.send(player, MessageUtil.format(config.getMainOnline(), "seconds", String.valueOf(config.getStableSeconds())));
            plugin.getLogger().info("Queued " + player.getName() + " for rejoin (rejoin countdown in progress).");
        } else {
            // Main server state unknown or idle — start check to see if it goes down or is down
            ensureMonitoring();
        }
    }

    public void onPlayerQuit(Player player) {
        // Don't drop pending during an active send-back — transfer already snapshotted
        if (state == State.TRANSFERRING || state == State.STABLE) {
            return;
        }
        pending.remove(player.getUniqueId());
        if (getPendingCount() == 0 && state == State.WAITING_UP) {
            plugin.getLogger().info("No pending players left — stopping rejoin wait.");
            stop();
        }
    }

    private void ensureMonitoring() {
        if (state == State.WAITING_DOWN || state == State.WAITING_UP || state == State.STABLE || state == State.TRANSFERRING) {
            return;
        }
        startMonitoring();
    }

    private void startMonitoring() {
        cancelTasks();
        transferLocked = false;
        state = State.WAITING_DOWN;
        startedAtMs = System.currentTimeMillis();
        plugin.getLogger().info("Monitoring main server "
                + config.getReturnHost() + ":" + config.getReturnPort()
                + " for restart downtime.");

        long pollTicks = config.getPollIntervalSeconds() * 20L;
        // Check quickly on first tick
        long firstDelay = 10L;

        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::pollTick, firstDelay, pollTicks);
    }

    private void pollTick() {
        if (state != State.WAITING_DOWN && state != State.WAITING_UP) {
            return;
        }
        if (config.getTimeoutSeconds() > 0 && state == State.WAITING_UP) {
            long elapsedSec = (System.currentTimeMillis() - startedAtMs) / 1000L;
            if (elapsedSec >= config.getTimeoutSeconds()) {
                plugin.getLogger().warning("Rejoin poll timed out.");
                stop();
                return;
            }
        }

        String host = config.getReturnHost();
        int port = config.getReturnPort();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean online = isReachable(host, port);
            Bukkit.getScheduler().runTask(plugin, () -> handleReachability(online));
        });
    }

    private void handleReachability(boolean online) {
        if (state == State.WAITING_DOWN) {
            if (!online) {
                // Main server went down! Transition to WAITING_UP and queue all online players
                state = State.WAITING_UP;
                startedAtMs = System.currentTimeMillis();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    pending.add(player.getUniqueId());
                }
                MessageUtil.broadcast(config.getWaiting());
                plugin.getLogger().info("Main server detected offline — queued " + getPendingCount() + " player(s) for auto-rejoin.");
            } else {
                // Main server is online. If it stays online without going down for a check cycle (15s), no restart occurred
                long elapsedSec = (System.currentTimeMillis() - startedAtMs) / 1000L;
                int monitorDuration = Math.max(10, config.getStartupGraceSeconds() + 5);
                if (elapsedSec >= monitorDuration) {
                    plugin.getLogger().info("Main server remained online for " + elapsedSec + "s (no downtime detected). Returning to idle.");
                    stop();
                }
            }
            return;
        }
        if (state == State.WAITING_UP) {
            if (online) {
                // Main server was confirmed down and is now back online!
                onMainReachable();
            }
        }
    }

    private void onMainReachable() {
        if (state != State.WAITING_UP) {
            return;
        }
        if (transferLocked) {
            return;
        }
        transferLocked = true;
        state = State.STABLE;
        cancelPollOnly();

        int stable = config.getStableSeconds();
        MessageUtil.broadcast(MessageUtil.format(config.getMainOnline(), "seconds", String.valueOf(stable)));
        plugin.getLogger().info("Main server is back online — rejoining in " + stable + "s (" + getPendingCount() + " pending).");
        if (stable <= 0) {
            transferPending();
            return;
        }
        stableTask = Bukkit.getScheduler().runTaskLater(plugin, this::transferPending, stable * 20L);
    }

    private void transferPending() {
        state = State.TRANSFERRING;
        String host = config.getReturnHost();
        int port = config.getReturnPort();
        MessageUtil.broadcast(config.getTransferring());

        Set<UUID> snapshot = new HashSet<>(pending);
        for (UUID id : snapshot) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) {
                pending.remove(id);
                continue;
            }
            try {
                player.transfer(host, port);
                plugin.getLogger().info("Sent rejoin transfer to " + player.getName());
            } catch (Exception exception) {
                plugin.getLogger().warning("Failed to rejoin transfer " + player.getName() + ": " + exception.getMessage());
            }
        }

        pending.clear();
        cancelTasks();
        state = State.IDLE;
        transferLocked = false;
        plugin.getLogger().info("Rejoin cycle finished.");
    }

    private void cancelPollOnly() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
    }

    private void cancelTasks() {
        cancelPollOnly();
        if (stableTask != null) {
            stableTask.cancel();
            stableTask = null;
        }
    }

    private boolean isReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), config.getConnectTimeoutMs());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
