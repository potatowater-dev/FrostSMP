package com.frostsmp.restart.config;

import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfig {

    private final JavaPlugin plugin;

    private int defaultMinutes;
    private int combatLogGraceSeconds;
    private boolean transferEnabled;
    private String transferHost;
    private int transferPort;
    private int transferAtSeconds;
    private int transferDelaySeconds;
    private String noPermission;
    private String usage;
    private String alreadyScheduled;
    private String notScheduled;
    private String cancelled;
    private String scheduled;
    private String announce5m;
    private String announce1m;
    private String announce30s;
    private String announceCountdown;
    private String status;
    private String restarting;
    private String transferring;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var config = plugin.getConfig();

        defaultMinutes = Math.max(1, config.getInt("default-minutes", 5));
        combatLogGraceSeconds = Math.max(1, config.getInt("combat-log-grace-seconds", 20));

        // Transfer/rejoin holding server settings
        transferEnabled = config.getBoolean("transfer.enabled", true);
        transferHost = config.getString("transfer.host", "85.11.102.61");
        transferPort = Math.max(1, config.getInt("transfer.port", 25795));
        // Send transfer packets this many seconds before countdown hits 0
        transferAtSeconds = Math.max(1, config.getInt("transfer.at-seconds", 8));
        // Max seconds to wait for players to leave after transfer before force-shutdown
        transferDelaySeconds = Math.max(5, config.getInt("transfer.delay-seconds", 15));

        noPermission = config.getString("messages.no-permission", "<red>No permission.</red>");
        usage = config.getString("messages.usage",
                "<gray>Usage: /frostrestart <start [minutes]|now|cancel|status></gray>");
        alreadyScheduled = config.getString("messages.already-scheduled", "<red>A restart is already scheduled.</red>");
        notScheduled = config.getString("messages.not-scheduled", "<gray>No restart is scheduled.</gray>");
        cancelled = config.getString("messages.cancelled", "<yellow>Scheduled restart cancelled.</yellow>");
        scheduled = config.getString("messages.scheduled",
                "<gold>Restart</gold> <gray>·</gray> <white>{minutes}m</white> <gray>until shutdown.</gray>");
        announce5m = config.getString("messages.announce-5m",
                "<gold>Restart</gold> <gray>in</gray> <white>5 minutes</white><gray>.</gray>");
        announce1m = config.getString("messages.announce-1m",
                "<gold>Restart</gold> <gray>in</gray> <white>1 minute</white><gray>.</gray>");
        announce30s = config.getString("messages.announce-30s",
                "<gold>Restart</gold> <gray>in</gray> <white>30 seconds</white><gray>.</gray>");
        announceCountdown = config.getString("messages.announce-countdown",
                "<gold>Restart</gold> <gray>in</gray> <white>{seconds}s</white><gray>.</gray>");
        status = config.getString("messages.status", "<gray>Restart in</gray> <white>{time}</white><gray>.</gray>");
        restarting = config.getString("messages.restarting", "<red>Restarting now...</red>");
        transferring = config.getString("messages.transferring",
                "<gold>Transferring you to the holding server...</gold>");
    }

    public int getDefaultMinutes() {
        return defaultMinutes;
    }

    public int getCombatLogGraceSeconds() {
        return combatLogGraceSeconds;
    }

    public boolean isTransferEnabled() {
        return transferEnabled;
    }

    public String getTransferHost() {
        return transferHost;
    }

    public int getTransferPort() {
        return transferPort;
    }

    public int getTransferAtSeconds() {
        return transferAtSeconds;
    }

    public int getTransferDelaySeconds() {
        return transferDelaySeconds;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getUsage() {
        return usage;
    }

    public String getAlreadyScheduled() {
        return alreadyScheduled;
    }

    public String getNotScheduled() {
        return notScheduled;
    }

    public String getCancelled() {
        return cancelled;
    }

    public String getScheduled() {
        return scheduled;
    }

    public String getAnnounce5m() {
        return announce5m;
    }

    public String getAnnounce1m() {
        return announce1m;
    }

    public String getAnnounce30s() {
        return announce30s;
    }

    public String getAnnounceCountdown() {
        return announceCountdown;
    }

    public String getStatus() {
        return status;
    }

    public String getRestarting() {
        return restarting;
    }

    public String getTransferring() {
        return transferring;
    }
}
