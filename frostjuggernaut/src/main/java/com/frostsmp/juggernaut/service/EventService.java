package com.frostsmp.juggernaut.service;

import com.frostsmp.juggernaut.config.PluginConfig;
import com.frostsmp.juggernaut.storage.JuggernautStorage;
import com.frostsmp.juggernaut.util.MessageUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EventService {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final DamageTracker damageTracker;
    private final BossBarService bossBarService;
    private final JuggernautStorage storage;

    private final Set<UUID> juggernauts = new LinkedHashSet<>();
    private final Map<UUID, String> juggernautNames = new LinkedHashMap<>();
    private final Set<UUID> downJuggernauts = new LinkedHashSet<>();
    private final Map<UUID, BukkitTask> disconnectTasks = new HashMap<>();

    private EventState state = EventState.IDLE;
    private Boolean previousPvp;
    private String pvpWorldName;
    private BukkitTask winnerTask;

    public EventService(JavaPlugin plugin,
                        PluginConfig config,
                        DamageTracker damageTracker,
                        BossBarService bossBarService,
                        JuggernautStorage storage) {
        this.plugin = plugin;
        this.config = config;
        this.damageTracker = damageTracker;
        this.bossBarService = bossBarService;
        this.storage = storage;
    }

    public EventState getState() {
        return state;
    }

    public boolean isActive() {
        return state == EventState.ACTIVE;
    }

    public boolean isRunning() {
        return state == EventState.ACTIVE || state == EventState.FINISHED;
    }

    public DamageTracker getDamageTracker() {
        return damageTracker;
    }

    public Set<UUID> getJuggernauts() {
        return Collections.unmodifiableSet(juggernauts);
    }

    public boolean isJuggernaut(UUID playerId) {
        return playerId != null && juggernauts.contains(playerId);
    }

    public boolean isAliveJuggernaut(UUID playerId) {
        return isJuggernaut(playerId) && !downJuggernauts.contains(playerId);
    }

    public int getJuggernautCount() {
        return juggernauts.size();
    }

    public int getAliveJuggernautCount() {
        return juggernauts.size() - downJuggernauts.size();
    }

    public String getJuggernautName(UUID playerId) {
        return juggernautNames.get(playerId);
    }

    public String getJuggernautNameList() {
        if (juggernautNames.isEmpty()) {
            return "-";
        }
        return String.join(", ", juggernautNames.values());
    }

    // --- selection ---------------------------------------------------------

    /**
     * Adds the player as a juggernaut, or removes them if already selected.
     */
    public void toggleJuggernaut(Player player, CommandSender feedback) {
        if (isRunning()) {
            MessageUtil.send(feedback, config.getAlreadyActive());
            return;
        }
        UUID id = player.getUniqueId();
        if (juggernauts.remove(id)) {
            juggernautNames.remove(id);
            state = juggernauts.isEmpty() ? EventState.IDLE : EventState.SELECTED;
            MessageUtil.send(feedback, MessageUtil.format(config.getDeselected(), "player", player.getName()));
            return;
        }
        if (juggernauts.size() >= config.getMaxJuggernauts()) {
            MessageUtil.send(feedback, MessageUtil.format(config.getMaxSelected(),
                    "max", String.valueOf(config.getMaxJuggernauts())));
            return;
        }
        juggernauts.add(id);
        juggernautNames.put(id, player.getName());
        state = EventState.SELECTED;
        MessageUtil.send(feedback, MessageUtil.format(config.getSelected(),
                "player", player.getName(),
                "count", String.valueOf(juggernauts.size()),
                "max", String.valueOf(config.getMaxJuggernauts())));
    }

    // --- lifecycle ---------------------------------------------------------

    public boolean start(CommandSender feedback) {
        if (isRunning()) {
            MessageUtil.send(feedback, config.getAlreadyActive());
            return false;
        }
        if (juggernauts.isEmpty()) {
            MessageUtil.send(feedback, config.getNoSelection());
            return false;
        }
        for (UUID id : juggernauts) {
            Player juggernaut = Bukkit.getPlayer(id);
            if (juggernaut == null || !juggernaut.isOnline()) {
                MessageUtil.send(feedback, MessageUtil.format(config.getJuggernautOffline(),
                        "player", juggernautNames.getOrDefault(id, "Unknown")));
                return false;
            }
            juggernautNames.put(id, juggernaut.getName());
        }

        World world = Bukkit.getWorld(config.getEventWorldName());
        if (world == null) {
            MessageUtil.send(feedback, config.getWorldMissing());
            return false;
        }

        previousPvp = readPvp(world);
        pvpWorldName = world.getName();
        storage.setPvpRestore(pvpWorldName, previousPvp);
        writePvp(world, true);

        downJuggernauts.clear();
        cancelDisconnectTasks();
        damageTracker.clear();
        state = EventState.ACTIVE;
        bossBarService.start();

        String names = getJuggernautNameList();
        broadcastChat(MessageUtil.format(config.getEventStarted(),
                "juggernaut", names, "count", String.valueOf(juggernauts.size())));
        broadcastTitle(
                MessageUtil.format(config.getTitleStarted(), "juggernaut", names),
                MessageUtil.format(config.getSubtitleStarted(), "juggernaut", names)
        );
        return true;
    }

    public boolean stop(CommandSender feedback, boolean cancelled) {
        if (!isRunning()) {
            if (feedback != null) {
                MessageUtil.send(feedback, config.getNotRunning());
            }
            return false;
        }
        reset();
        broadcastChat(cancelled ? config.getEventCancelled() : config.getEventStopped());
        return true;
    }

    public void shutdown() {
        if (isRunning()) {
            restorePvp();
        }
        cancelDisconnectTasks();
        cancelWinnerTask();
        bossBarService.stop();
        damageTracker.clear();
        juggernauts.clear();
        juggernautNames.clear();
        downJuggernauts.clear();
        state = EventState.IDLE;
    }

    private void reset() {
        restorePvp();
        cancelDisconnectTasks();
        cancelWinnerTask();
        bossBarService.stop();
        damageTracker.clear();
        juggernauts.clear();
        juggernautNames.clear();
        downJuggernauts.clear();
        state = EventState.IDLE;
    }

    // --- juggernaut elimination -------------------------------------------

    public void handleJuggernautDeath(Player juggernaut) {
        markDown(juggernaut.getUniqueId(), config.getJuggernautEliminated());
    }

    private void markDown(UUID id, String eliminationMessage) {
        if (state != EventState.ACTIVE || !isAliveJuggernaut(id)) {
            return;
        }
        downJuggernauts.add(id);
        cancelDisconnectTask(id);

        String name = juggernautNames.getOrDefault(id, "Unknown");
        if (getAliveJuggernautCount() > 0) {
            broadcastChat(MessageUtil.format(eliminationMessage,
                    "juggernaut", name,
                    "remaining", String.valueOf(getAliveJuggernautCount()),
                    "total", String.valueOf(juggernauts.size())));
            return;
        }

        state = EventState.FINISHED;
        String names = getJuggernautNameList();
        broadcastChat(MessageUtil.format(config.getJuggernautDownChat(), "juggernaut", names, "last", name));
        broadcastTitle(
                MessageUtil.format(config.getTitleDown(), "juggernaut", names, "last", name),
                MessageUtil.format(config.getSubtitleDown(), "juggernaut", names, "last", name)
        );
        cancelWinnerTask();
        winnerTask = Bukkit.getScheduler().runTaskLater(plugin, this::announceWinner,
                config.getWinnerAnnounceDelayTicks());
    }

    private void announceWinner() {
        winnerTask = null;
        if (state != EventState.FINISHED) {
            return;
        }

        DamageTracker.Entry top = damageTracker.getTop();
        if (top == null || damageTracker.isEmpty()) {
            broadcastChat(config.getNoWinner());
            reset();
            return;
        }

        String winnerName = top.name() != null ? top.name() : "Unknown";
        String damageText = MessageUtil.formatDamage(top.damage());

        broadcastChat(MessageUtil.format(config.getWinnerChat(), "winner", winnerName, "damage", damageText));
        broadcastTitle(
                MessageUtil.format(config.getTitleWinner(), "winner", winnerName, "damage", damageText),
                MessageUtil.format(config.getSubtitleWinner(), "winner", winnerName, "damage", damageText)
        );
        reset();
    }

    // --- connection handling ----------------------------------------------

    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        if (state != EventState.ACTIVE || !isAliveJuggernaut(id)) {
            return;
        }
        int grace = config.getReconnectGraceSeconds();
        String name = juggernautNames.getOrDefault(id, player.getName());
        if (grace <= 0) {
            markDown(id, config.getJuggernautEliminated());
            return;
        }
        broadcastChat(MessageUtil.format(config.getJuggernautQuit(),
                "juggernaut", name, "seconds", String.valueOf(grace)));
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            disconnectTasks.remove(id);
            Player back = Bukkit.getPlayer(id);
            if (back == null || !back.isOnline()) {
                markDown(id, config.getJuggernautTimedOut());
            }
        }, grace * 20L);
        disconnectTasks.put(id, task);
    }

    public void handleJoin(Player player) {
        UUID id = player.getUniqueId();
        if (state != EventState.ACTIVE) {
            return;
        }
        juggernautNames.computeIfPresent(id, (key, old) -> player.getName());
        if (disconnectTasks.containsKey(id)) {
            cancelDisconnectTask(id);
            broadcastChat(MessageUtil.format(config.getJuggernautReturned(),
                    "juggernaut", player.getName()));
        }
    }

    private void cancelDisconnectTask(UUID id) {
        BukkitTask task = disconnectTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
    }

    private void cancelDisconnectTasks() {
        for (BukkitTask task : disconnectTasks.values()) {
            task.cancel();
        }
        disconnectTasks.clear();
    }

    private void cancelWinnerTask() {
        if (winnerTask != null) {
            winnerTask.cancel();
            winnerTask = null;
        }
    }

    // --- pvp ---------------------------------------------------------------

    private static boolean readPvp(World world) {
        Boolean value = world.getGameRuleValue(GameRules.PVP);
        if (value != null) {
            return value;
        }
        return Boolean.TRUE.equals(world.getGameRuleDefault(GameRules.PVP));
    }

    private static void writePvp(World world, boolean value) {
        world.setGameRule(GameRules.PVP, value);
    }

    private void restorePvp() {
        if (previousPvp != null && pvpWorldName != null) {
            World world = Bukkit.getWorld(pvpWorldName);
            if (world != null) {
                writePvp(world, previousPvp);
            }
        }
        previousPvp = null;
        pvpWorldName = null;
        storage.clearPvpRestore();
    }

    /**
     * Undoes a forced PvP flag left behind by a crash mid-event.
     */
    public void restorePvpFromDisk() {
        String worldName = storage.getPvpRestoreWorld();
        if (worldName == null) {
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            writePvp(world, storage.getPvpRestoreValue());
            plugin.getLogger().info("Restored PvP=" + storage.getPvpRestoreValue()
                    + " in '" + worldName + "' after an unclean shutdown.");
        }
        storage.clearPvpRestore();
    }

    // --- broadcast helpers -------------------------------------------------

    private void broadcastChat(String message) {
        var component = MessageUtil.parse(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    private void broadcastTitle(String title, String subtitle) {
        Title.Times times = Title.Times.times(
                ticksToDuration(config.getTitleFadeInTicks()),
                ticksToDuration(config.getTitleStayTicks()),
                ticksToDuration(config.getTitleFadeOutTicks())
        );
        Title adventureTitle = Title.title(
                MessageUtil.parse(title),
                MessageUtil.parse(subtitle),
                times
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(adventureTitle);
        }
    }

    private static Duration ticksToDuration(int ticks) {
        return Duration.ofMillis(ticks * 50L);
    }
}
