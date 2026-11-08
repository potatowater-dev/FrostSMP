package com.frostsmp.event.config;

import com.frostsmp.event.FrostEventPlugin;

public final class PluginConfig {

    private final FrostEventPlugin plugin;

    private double defaultHours;
    private int locatorTickInterval;
    private int spawnProtectionSeconds;
    private String noPermission;
    private String usage;
    private String alreadyRunning;
    private String notRunning;
    private String started;
    private String stopped;
    private String stoppedNoKills;
    private String statusRunning;
    private String statusIdle;
    private String winner;
    private String winnerNone;
    private String killsSelf;
    private String killsOther;
    private String killsNone;
    private String nakedBlockedAttacker;
    private String nakedBlockedVictim;
    private String spawnProtectionGranted;
    private String spawnProtectionEnded;
    private String spawnProtectionBlockedAttacker;

    public PluginConfig(FrostEventPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var config = plugin.getConfig();

        defaultHours = Math.max(0.1, config.getDouble("default-hours", 6.0));
        locatorTickInterval = Math.max(1, config.getInt("locator-tick-interval", 10));
        spawnProtectionSeconds = Math.max(0, config.getInt("spawn-protection-seconds", 300));

        noPermission = config.getString("messages.no-permission", "<red>No permission.</red>");
        usage = config.getString("messages.usage",
                "<gray>Usage: /frostevent <start [hours]|stop|status|winner|kills [player]></gray>");
        alreadyRunning = config.getString("messages.already-running", "<red>An event is already running.</red>");
        notRunning = config.getString("messages.not-running", "<gray>No event is running.</gray>");
        started = config.getString("messages.started",
                "<gold>Event started</gold> <gray>for</gray> <white>{hours}h</white><gray>.</gray>");
        stopped = config.getString("messages.stopped",
                "<gold>Event ended.</gold> <gray>Winner:</gray> <white>{winner}</white> <gray>(</gray><white>{kills}</white> <gray>kills)</gray>");
        stoppedNoKills = config.getString("messages.stopped-no-kills",
                "<gold>Event ended.</gold> <gray>No kills were recorded.</gray>");
        statusRunning = config.getString("messages.status-running",
                "<gray>Event active ·</gray> <white>{remaining}</white> <gray>left · kills tracked:</gray> <white>{players}</white>");
        statusIdle = config.getString("messages.status-idle", "<gray>No event is running.</gray>");
        winner = config.getString("messages.winner",
                "<gold>Last winner:</gold> <white>{winner}</white> <gray>·</gray> <white>{kills}</white> <gray>kills</gray>");
        winnerNone = config.getString("messages.winner-none", "<gray>No winner saved yet.</gray>");
        killsSelf = config.getString("messages.kills-self", "<gray>Your kills:</gray> <white>{kills}</white>");
        killsOther = config.getString("messages.kills-other",
                "<gray>Kills for</gray> <white>{player}</white><gray>:</gray> <white>{kills}</white>");
        killsNone = config.getString("messages.kills-none", "<gray>No kills recorded for that player.</gray>");
        nakedBlockedAttacker = config.getString("messages.naked-blocked-attacker",
                "<red>You can't kill a naked player during the event.</red>");
        nakedBlockedVictim = config.getString("messages.naked-blocked-victim",
                "<yellow>A player tried to kill you while you were naked (protected).</yellow>");
        spawnProtectionGranted = config.getString("messages.spawn-protection-granted",
                "<green>Spawn protection:</green> <white>{seconds}s</white> <gray>(ends if you hit someone)</gray>");
        spawnProtectionEnded = config.getString("messages.spawn-protection-ended",
                "<yellow>Spawn protection ended.</yellow>");
        spawnProtectionBlockedAttacker = config.getString("messages.spawn-protection-blocked-attacker",
                "<red>That player has spawn protection.</red>");
    }

    public double getDefaultHours() {
        return defaultHours;
    }

    public int getLocatorTickInterval() {
        return locatorTickInterval;
    }

    public int getSpawnProtectionSeconds() {
        return spawnProtectionSeconds;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getUsage() {
        return usage;
    }

    public String getAlreadyRunning() {
        return alreadyRunning;
    }

    public String getNotRunning() {
        return notRunning;
    }

    public String getStarted() {
        return started;
    }

    public String getStopped() {
        return stopped;
    }

    public String getStoppedNoKills() {
        return stoppedNoKills;
    }

    public String getStatusRunning() {
        return statusRunning;
    }

    public String getStatusIdle() {
        return statusIdle;
    }

    public String getWinner() {
        return winner;
    }

    public String getWinnerNone() {
        return winnerNone;
    }

    public String getKillsSelf() {
        return killsSelf;
    }

    public String getKillsOther() {
        return killsOther;
    }

    public String getKillsNone() {
        return killsNone;
    }

    public String getNakedBlockedAttacker() {
        return nakedBlockedAttacker;
    }

    public String getNakedBlockedVictim() {
        return nakedBlockedVictim;
    }

    public String getSpawnProtectionGranted() {
        return spawnProtectionGranted;
    }

    public String getSpawnProtectionEnded() {
        return spawnProtectionEnded;
    }

    public String getSpawnProtectionBlockedAttacker() {
        return spawnProtectionBlockedAttacker;
    }
}
