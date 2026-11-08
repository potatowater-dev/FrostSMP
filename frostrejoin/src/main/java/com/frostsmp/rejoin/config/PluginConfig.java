package com.frostsmp.rejoin.config;

import com.frostsmp.rejoin.FrostRejoinPlugin;

public final class PluginConfig {

    private final FrostRejoinPlugin plugin;

    private String returnHost;
    private int returnPort;
    private int connectTimeoutMs;
    private int startupGraceSeconds;
    private int pollIntervalSeconds;
    private int stableSeconds;
    private int timeoutSeconds;
    private String noPermission;
    private String usage;
    private String reloaded;
    private String waiting;
    private String mainOnline;
    private String transferring;
    private String forced;
    private String statusWaiting;
    private String statusReady;
    private String statusIdle;
    private String statusLine;

    public PluginConfig(FrostRejoinPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var config = plugin.getConfig();

        returnHost = config.getString("return.host", "frostffa.xyz");
        returnPort = Math.max(1, config.getInt("return.port", 25640));
        connectTimeoutMs = Math.max(500, config.getInt("connect-timeout-ms", 2500));
        startupGraceSeconds = Math.max(0, config.getInt("startup-grace-seconds", 5));
        pollIntervalSeconds = Math.max(1, config.getInt("poll-interval-seconds", 5));
        stableSeconds = Math.max(0, config.getInt("stable-seconds", 15));
        timeoutSeconds = Math.max(0, config.getInt("timeout-seconds", 0));

        noPermission = config.getString("messages.no-permission", "<red>No permission.</red>");
        usage = config.getString("messages.usage", "<gray>Usage: /frostrejoin <now|status|reload></gray>");
        reloaded = config.getString("messages.reloaded", "<green>FrostRejoin reloaded.</green>");
        waiting = config.getString("messages.waiting",
                "<gold>Server restarting...</gold> <gray>You will rejoin automatically.</gray>");
        mainOnline = config.getString("messages.main-online",
                "<green>Main server is back!</green> <gray>Rejoining in <white>{seconds}s</white>...</gray>");
        transferring = config.getString("messages.transferring",
                "<gold>Transferring you back to the main server...</gold>");
        forced = config.getString("messages.forced", "<green>Forcing rejoin for all online players.</green>");
        statusWaiting = config.getString("messages.status-waiting",
                "<gray>Waiting for main</gray> <white>{host}:{port}</white><gray>...</gray>");
        statusReady = config.getString("messages.status-ready", "<green>Main is online.</green>");
        statusIdle = config.getString("messages.status-idle", "<gray>Idle — no rejoin pending.</gray>");
        statusLine = config.getString("messages.status-line",
                "<gray>Pending:</gray> <white>{count}</white> <gray>· State:</gray> <white>{state}</white> <gray>· Main:</gray> <white>{host}:{port}</white>");
    }

    public String getReturnHost() {
        return returnHost;
    }

    public int getReturnPort() {
        return returnPort;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getStartupGraceSeconds() {
        return startupGraceSeconds;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public int getStableSeconds() {
        return stableSeconds;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getUsage() {
        return usage;
    }

    public String getReloaded() {
        return reloaded;
    }

    public String getWaiting() {
        return waiting;
    }

    public String getMainOnline() {
        return mainOnline;
    }

    public String getTransferring() {
        return transferring;
    }

    public String getForced() {
        return forced;
    }

    public String getStatusWaiting() {
        return statusWaiting;
    }

    public String getStatusReady() {
        return statusReady;
    }

    public String getStatusIdle() {
        return statusIdle;
    }

    public String getStatusLine() {
        return statusLine;
    }
}
