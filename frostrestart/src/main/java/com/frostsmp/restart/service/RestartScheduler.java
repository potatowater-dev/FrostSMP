package com.frostsmp.restart.service;

import com.frostsmp.restart.config.PluginConfig;
import com.frostsmp.restart.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class RestartScheduler {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final FrostCombatBridge combatBridge;

    private BukkitTask task;
    private BukkitTask shutdownWaitTask;
    private int secondsRemaining;
    private boolean transferSent;

    public RestartScheduler(JavaPlugin plugin, PluginConfig config, FrostCombatBridge combatBridge) {
        this.plugin = plugin;
        this.config = config;
        this.combatBridge = combatBridge;
    }

    public boolean isScheduled() {
        return task != null && !task.isCancelled();
    }

    public int getSecondsRemaining() {
        return Math.max(0, secondsRemaining);
    }

    public boolean start(int minutes, org.bukkit.command.CommandSender feedback) {
        if (isScheduled()) {
            if (feedback != null) {
                MessageUtil.send(feedback, config.getAlreadyScheduled());
            }
            return false;
        }

        secondsRemaining = Math.max(1, minutes) * 60;
        transferSent = false;
        MessageUtil.broadcast(MessageUtil.format(config.getScheduled(), "minutes", String.valueOf(minutes)));
        announceForRemaining(secondsRemaining);

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (secondsRemaining <= 0) {
                finishRestart();
                return;
            }
            secondsRemaining--;
            if (secondsRemaining <= 0) {
                finishRestart();
                return;
            }
            // Send transfer packets before the hard shutdown so clients can leave first
            if (config.isTransferEnabled()
                    && !transferSent
                    && secondsRemaining <= config.getTransferAtSeconds()) {
                transferPlayers();
            }
            announceForRemaining(secondsRemaining);
        }, 20L, 20L);
        return true;
    }

    public void startNow(org.bukkit.command.CommandSender feedback) {
        stopTask();
        cancelShutdownWait();
        secondsRemaining = 0;
        combatBridge.disableCombatLogGrace();
        finishRestart();
    }

    public boolean cancel(org.bukkit.command.CommandSender feedback) {
        if (!isScheduled() && shutdownWaitTask == null) {
            if (feedback != null) {
                MessageUtil.send(feedback, config.getNotScheduled());
            }
            return false;
        }
        stopTask();
        cancelShutdownWait();
        transferSent = false;
        combatBridge.disableCombatLogGrace();
        secondsRemaining = 0;
        if (feedback != null) {
            MessageUtil.send(feedback, config.getCancelled());
        }
        MessageUtil.broadcast(config.getCancelled());
        return true;
    }

    public void shutdown() {
        stopTask();
        cancelShutdownWait();
        combatBridge.disableCombatLogGrace();
        secondsRemaining = 0;
        transferSent = false;
    }

    private void announceForRemaining(int remaining) {
        if (remaining == 300) {
            MessageUtil.broadcast(config.getAnnounce5m());
            return;
        }
        if (remaining == 60) {
            MessageUtil.broadcast(config.getAnnounce1m());
            return;
        }
        if (remaining == 30) {
            MessageUtil.broadcast(config.getAnnounce30s());
            return;
        }
        if (remaining <= 10 && remaining >= 1) {
            if (remaining == 10) {
                combatBridge.enableCombatLogGrace(config.getCombatLogGraceSeconds());
            }
            MessageUtil.broadcast(MessageUtil.format(config.getAnnounceCountdown(), "seconds", String.valueOf(remaining)));
        }
    }

    private void finishRestart() {
        stopTask();
        MessageUtil.broadcast(config.getRestarting());

        if (config.isTransferEnabled()) {
            if (!transferSent) {
                transferPlayers();
            }
            waitThenShutdown();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getServer().shutdown());
    }

    private void transferPlayers() {
        transferSent = true;
        String host = config.getTransferHost();
        int port = config.getTransferPort();
        MessageUtil.broadcast(config.getTransferring());
        plugin.getLogger().info("Transferring players to " + host + ":" + port);

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (Player player : players) {
            try {
                // Paper transfer packet (1.20.5+) — do NOT rely on a /transfer command
                player.transfer(host, port);
                plugin.getLogger().info("Sent transfer packet to " + player.getName());
            } catch (Exception exception) {
                plugin.getLogger().warning("Failed to transfer " + player.getName() + ": " + exception.getMessage());
            }
        }
    }

    private void waitThenShutdown() {
        cancelShutdownWait();
        final int maxWaitTicks = Math.max(20, config.getTransferDelaySeconds() * 20);
        final int[] waited = {0};

        shutdownWaitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            waited[0] += 10;
            int online = Bukkit.getOnlinePlayers().size();
            if (online == 0) {
                plugin.getLogger().info("All players transferred — shutting down.");
                cancelShutdownWait();
                Bukkit.getServer().shutdown();
                return;
            }
            if (waited[0] >= maxWaitTicks) {
                plugin.getLogger().warning(online + " player(s) still online after transfer wait — shutting down anyway.");
                cancelShutdownWait();
                Bukkit.getServer().shutdown();
            }
        }, 10L, 10L);
    }

    private void cancelShutdownWait() {
        if (shutdownWaitTask != null) {
            shutdownWaitTask.cancel();
            shutdownWaitTask = null;
        }
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0 && seconds > 0) {
            return minutes + "m " + seconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return seconds + "s";
    }
}
