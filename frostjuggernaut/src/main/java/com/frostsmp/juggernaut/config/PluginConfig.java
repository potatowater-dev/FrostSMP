package com.frostsmp.juggernaut.config;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class PluginConfig {

    private final JavaPlugin plugin;

    private String eventWorldName;
    private int maxJuggernauts;
    private int reconnectGraceSeconds;
    private boolean capOverkillDamage;
    private int winnerAnnounceDelayTicks;
    private int titleFadeInTicks;
    private int titleStayTicks;
    private int titleFadeOutTicks;

    private boolean bossBarEnabled;
    private BossBar.Color bossBarColor;
    private BossBar.Overlay bossBarOverlay;
    private int bossBarUpdateTicks;
    private String bossBarFormat;
    private String bossBarJuggernautFormat;
    private String bossBarEmptyName;

    private String noPermission;
    private String usage;
    private String playersOnly;
    private String reloaded;
    private String noSelection;
    private String juggernautOffline;
    private String alreadyActive;
    private String notRunning;
    private String selected;
    private String deselected;
    private String maxSelected;
    private String eventStarted;
    private String titleStarted;
    private String subtitleStarted;
    private String juggernautEliminated;
    private String juggernautQuit;
    private String juggernautReturned;
    private String juggernautTimedOut;
    private String juggernautDownChat;
    private String titleDown;
    private String subtitleDown;
    private String winnerChat;
    private String titleWinner;
    private String subtitleWinner;
    private String noWinner;
    private String eventStopped;
    private String eventCancelled;
    private String worldMissing;
    private String statusIdle;
    private String statusRunning;
    private String topHeader;
    private String topLine;
    private String topEmpty;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        var config = plugin.getConfig();

        eventWorldName = config.getString("event-world", "world");
        maxJuggernauts = Math.max(1, config.getInt("max-juggernauts", 2));
        reconnectGraceSeconds = Math.max(0, config.getInt("juggernaut-reconnect-grace-seconds", 120));
        capOverkillDamage = config.getBoolean("cap-overkill-damage", true);
        winnerAnnounceDelayTicks = Math.max(0, config.getInt("winner-announce-delay-ticks", 10));
        titleFadeInTicks = Math.max(0, config.getInt("title.fade-in-ticks", 5));
        titleStayTicks = Math.max(1, config.getInt("title.stay-ticks", 40));
        titleFadeOutTicks = Math.max(0, config.getInt("title.fade-out-ticks", 10));

        bossBarEnabled = config.getBoolean("bossbar.enabled", true);
        bossBarColor = parseBarColor(config.getString("bossbar.color", "RED"));
        bossBarOverlay = parseBarOverlay(config.getString("bossbar.style", "SOLID"));
        bossBarUpdateTicks = Math.max(1, config.getInt("bossbar.update-ticks", 10));
        bossBarFormat = config.getString("bossbar.format",
                "<gold>#1</gold> <white>{top}</white> <gray>{top_damage}</gray>"
                        + " <dark_gray>|</dark_gray> <aqua>You</aqua> <white>{your_damage}</white>");
        bossBarJuggernautFormat = config.getString("bossbar.format-juggernaut",
                "<red>JUGGERNAUT</red> <dark_gray>|</dark_gray> <gold>#1</gold>"
                        + " <white>{top}</white> <gray>{top_damage}</gray>");
        bossBarEmptyName = config.getString("bossbar.empty-name", "<gray>nobody</gray>");

        noPermission = config.getString("messages.no-permission", "<red>No permission.</red>");
        usage = config.getString("messages.usage", "<gray>Usage: /juggernaut <manage|top|status|stop|reload></gray>");
        playersOnly = config.getString("messages.players-only", "<red>Players only.</red>");
        reloaded = config.getString("messages.reloaded", "<green>FrostJuggernaut reloaded.</green>");
        noSelection = config.getString("messages.no-selection", "<red>Select a juggernaut first.</red>");
        juggernautOffline = config.getString("messages.juggernaut-offline",
                "<red>{player} is offline.</red>");
        alreadyActive = config.getString("messages.already-active", "<red>Event is already running.</red>");
        notRunning = config.getString("messages.not-running", "<red>No event to stop.</red>");
        selected = config.getString("messages.selected",
                "<green>Selected</green> <white>{player}</white> <green>as juggernaut.</green>"
                        + " <dark_gray>({count}/{max})</dark_gray>");
        deselected = config.getString("messages.deselected",
                "<yellow>Removed</yellow> <white>{player}</white> <yellow>from the juggernauts.</yellow>");
        maxSelected = config.getString("messages.max-selected",
                "<red>Only {max} juggernaut(s) allowed. Click one to remove it first.</red>");
        eventStarted = config.getString("messages.event-started",
                "<gold>Juggernaut</gold> <gray>·</gray> <white>{juggernaut}</white> <gray>is live. PvP on.</gray>");
        titleStarted = config.getString("messages.title-started", "<gold>JUGGERNAUT</gold>");
        subtitleStarted = config.getString("messages.subtitle-started", "<white>{juggernaut}</white>");
        juggernautEliminated = config.getString("messages.juggernaut-eliminated",
                "<red>{juggernaut} is down.</red> <gray>{remaining}/{total} juggernauts left.</gray>");
        juggernautQuit = config.getString("messages.juggernaut-quit",
                "<yellow>{juggernaut} disconnected.</yellow> <gray>{seconds}s to reconnect or they are out.</gray>");
        juggernautReturned = config.getString("messages.juggernaut-returned",
                "<green>{juggernaut} is back.</green> <gray>Event continues.</gray>");
        juggernautTimedOut = config.getString("messages.juggernaut-timed-out",
                "<red>{juggernaut} did not reconnect and is out.</red> <gray>{remaining}/{total} juggernauts left.</gray>");
        juggernautDownChat = config.getString("messages.juggernaut-down-chat",
                "<red>All juggernauts down.</red> <gray>{juggernaut} eliminated.</gray>");
        titleDown = config.getString("messages.title-down", "<red>JUGGERNAUTS DOWN</red>");
        subtitleDown = config.getString("messages.subtitle-down", "<gray>{juggernaut}</gray>");
        winnerChat = config.getString("messages.winner-chat",
                "<gold>Winner</gold> <gray>·</gray> <white>{winner}</white> <dark_gray>({damage} total dmg)</dark_gray>");
        titleWinner = config.getString("messages.title-winner", "<gold>WINNER</gold>");
        subtitleWinner = config.getString("messages.subtitle-winner", "<white>{winner}</white> <gray>{damage} dmg</gray>");
        noWinner = config.getString("messages.no-winner", "<gray>No damage recorded. No winner.</gray>");
        eventStopped = config.getString("messages.event-stopped", "<gray>Juggernaut event ended.</gray>");
        eventCancelled = config.getString("messages.event-cancelled", "<red>Juggernaut event cancelled.</red>");
        worldMissing = config.getString("messages.world-missing", "<red>Event world not found.</red>");
        statusIdle = config.getString("messages.status-idle",
                "<gray>Juggernaut</gray> <dark_gray>·</dark_gray> <white>{state}</white>"
                        + " <dark_gray>·</dark_gray> <gray>selected:</gray> <white>{juggernaut}</white>");
        statusRunning = config.getString("messages.status-running",
                "<gray>Juggernaut</gray> <dark_gray>·</dark_gray> <white>{state}</white>"
                        + " <dark_gray>·</dark_gray> <white>{juggernaut}</white>"
                        + " <dark_gray>·</dark_gray> <gray>alive</gray> <white>{alive}/{total}</white>");
        topHeader = config.getString("messages.top-header", "<gold>Top damage</gold>");
        topLine = config.getString("messages.top-line",
                "<gray>{rank}.</gray> <white>{player}</white> <dark_gray>-</dark_gray> <yellow>{damage}</yellow>");
        topEmpty = config.getString("messages.top-empty", "<gray>No damage recorded yet.</gray>");
    }

    private static BossBar.Color parseBarColor(String raw) {
        if (raw == null) {
            return BossBar.Color.RED;
        }
        try {
            return BossBar.Color.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BossBar.Color.RED;
        }
    }

    private static BossBar.Overlay parseBarOverlay(String raw) {
        if (raw == null) {
            return BossBar.Overlay.PROGRESS;
        }
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "SEGMENTED_6", "NOTCHED_6" -> BossBar.Overlay.NOTCHED_6;
            case "SEGMENTED_10", "NOTCHED_10" -> BossBar.Overlay.NOTCHED_10;
            case "SEGMENTED_12", "NOTCHED_12" -> BossBar.Overlay.NOTCHED_12;
            case "SEGMENTED_20", "NOTCHED_20" -> BossBar.Overlay.NOTCHED_20;
            default -> BossBar.Overlay.PROGRESS;
        };
    }

    public String getEventWorldName() {
        return eventWorldName;
    }

    public int getMaxJuggernauts() {
        return maxJuggernauts;
    }

    public int getReconnectGraceSeconds() {
        return reconnectGraceSeconds;
    }

    public boolean isCapOverkillDamage() {
        return capOverkillDamage;
    }

    public int getWinnerAnnounceDelayTicks() {
        return winnerAnnounceDelayTicks;
    }

    public int getTitleFadeInTicks() {
        return titleFadeInTicks;
    }

    public int getTitleStayTicks() {
        return titleStayTicks;
    }

    public int getTitleFadeOutTicks() {
        return titleFadeOutTicks;
    }

    public boolean isBossBarEnabled() {
        return bossBarEnabled;
    }

    public BossBar.Color getBossBarColor() {
        return bossBarColor;
    }

    public BossBar.Overlay getBossBarOverlay() {
        return bossBarOverlay;
    }

    public int getBossBarUpdateTicks() {
        return bossBarUpdateTicks;
    }

    public String getBossBarFormat() {
        return bossBarFormat;
    }

    public String getBossBarJuggernautFormat() {
        return bossBarJuggernautFormat;
    }

    public String getBossBarEmptyName() {
        return bossBarEmptyName;
    }

    public String getNoPermission() {
        return noPermission;
    }

    public String getUsage() {
        return usage;
    }

    public String getPlayersOnly() {
        return playersOnly;
    }

    public String getReloaded() {
        return reloaded;
    }

    public String getNoSelection() {
        return noSelection;
    }

    public String getJuggernautOffline() {
        return juggernautOffline;
    }

    public String getAlreadyActive() {
        return alreadyActive;
    }

    public String getNotRunning() {
        return notRunning;
    }

    public String getSelected() {
        return selected;
    }

    public String getDeselected() {
        return deselected;
    }

    public String getMaxSelected() {
        return maxSelected;
    }

    public String getEventStarted() {
        return eventStarted;
    }

    public String getTitleStarted() {
        return titleStarted;
    }

    public String getSubtitleStarted() {
        return subtitleStarted;
    }

    public String getJuggernautEliminated() {
        return juggernautEliminated;
    }

    public String getJuggernautQuit() {
        return juggernautQuit;
    }

    public String getJuggernautReturned() {
        return juggernautReturned;
    }

    public String getJuggernautTimedOut() {
        return juggernautTimedOut;
    }

    public String getJuggernautDownChat() {
        return juggernautDownChat;
    }

    public String getTitleDown() {
        return titleDown;
    }

    public String getSubtitleDown() {
        return subtitleDown;
    }

    public String getWinnerChat() {
        return winnerChat;
    }

    public String getTitleWinner() {
        return titleWinner;
    }

    public String getSubtitleWinner() {
        return subtitleWinner;
    }

    public String getNoWinner() {
        return noWinner;
    }

    public String getEventStopped() {
        return eventStopped;
    }

    public String getEventCancelled() {
        return eventCancelled;
    }

    public String getWorldMissing() {
        return worldMissing;
    }

    public String getStatusIdle() {
        return statusIdle;
    }

    public String getStatusRunning() {
        return statusRunning;
    }

    public String getTopHeader() {
        return topHeader;
    }

    public String getTopLine() {
        return topLine;
    }

    public String getTopEmpty() {
        return topEmpty;
    }
}
