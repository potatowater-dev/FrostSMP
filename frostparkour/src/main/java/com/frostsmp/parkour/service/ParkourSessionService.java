package com.frostsmp.parkour.service;

import com.frostsmp.parkour.FrostParkourPlugin;
import com.frostsmp.parkour.config.PluginConfig;
import com.frostsmp.parkour.model.Course;
import com.frostsmp.parkour.session.ParkourSession;
import com.frostsmp.parkour.util.MessageUtil;
import com.frostsmp.parkour.util.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ParkourSessionService {

    public enum ActionItemType {
        RESTART,
        CHECKPOINT,
        QUIT
    }

    private final FrostParkourPlugin plugin;
    private final PluginConfig config;
    private final CourseService courseService;
    private final LeaderboardService leaderboardService;

    private final Map<UUID, ParkourSession> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> startCooldown = new ConcurrentHashMap<>();
    private final NamespacedKey itemKey;

    public ParkourSessionService(FrostParkourPlugin plugin, PluginConfig config,
                                 CourseService courseService, LeaderboardService leaderboardService) {
        this.plugin = plugin;
        this.config = config;
        this.courseService = courseService;
        this.leaderboardService = leaderboardService;
        this.itemKey = new NamespacedKey(plugin, "parkour_action");
    }

    public boolean isInParkour(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public ParkourSession getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public boolean isOnStartCooldown(UUID playerId) {
        long cd = startCooldown.getOrDefault(playerId, 0L);
        return System.currentTimeMillis() < cd;
    }

    public boolean startCourse(Player player, Course course) {
        if (player == null || course == null || !course.isConfigured()) {
            return false;
        }
        if (isInParkour(player.getUniqueId())) {
            MessageUtil.send(player, config.msg("already-in-parkour"));
            return false;
        }
        if (isOnStartCooldown(player.getUniqueId())) {
            return false;
        }

        ParkourSession session = new ParkourSession(player, course.getId(), course.getStartLocation());
        sessions.put(player.getUniqueId(), session);

        leaderboardService.recordAttempt(course.getId(), player.getName());

        // Prepare player
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        clearInventoryKeepLastHotbar(player);
        player.teleport(course.getStartLocation());

        giveHotbarItems(player);
        playSound(player, "start");

        MessageUtil.send(player, config.msg("course-started"));

        if (config.isActionbarTimerEnabled()) {
            startTimerTask(player, session, course);
        }

        return true;
    }

    public void restartCourse(Player player) {
        ParkourSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        Course course = courseService.getCourse(session.getCourseId());
        if (course == null || course.getStartLocation() == null) {
            quitParkour(player, true);
            return;
        }

        session.resetTimer();
        session.setCurrentCheckpointIndex(-1);
        session.setLastCheckpointLocation(course.getStartLocation());
        player.teleport(course.getStartLocation());

        playSound(player, "restart");
        MessageUtil.send(player, config.msg("restarted"));
    }

    public void teleportLastCheckpoint(Player player) {
        ParkourSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        Location target = session.getLastCheckpointLocation();
        if (target != null) {
            player.teleport(target);
            playSound(player, "teleport-checkpoint");
            int cp = session.getCurrentCheckpointIndex() + 1;
            MessageUtil.send(player, MessageUtil.format(config.msg("checkpoint-teleport"), "checkpoint", String.valueOf(cp)));
        }
    }

    public void quitParkour(Player player, boolean sendMsg) {
        startCooldown.put(player.getUniqueId(), System.currentTimeMillis() + 2500L);
        ParkourSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            if (sendMsg) {
                MessageUtil.send(player, config.msg("not-in-parkour"));
            }
            return;
        }

        session.restorePlayer(player);
        if (sendMsg) {
            MessageUtil.send(player, config.msg("quit"));
            MessageUtil.send(player, config.msg("inventory-restored"));
        }
    }

    public void handleCheckpointReached(Player player, int index, Location location) {
        ParkourSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        // Only advance forward
        if (index > session.getCurrentCheckpointIndex()) {
            session.setCurrentCheckpointIndex(index);
            session.setLastCheckpointLocation(location);

            long elapsed = session.getElapsedTimeMs();
            playSound(player, "checkpoint");
            MessageUtil.send(player, MessageUtil.format(
                    config.msg("checkpoint-reached"),
                    "checkpoint", String.valueOf(index + 1),
                    "time", TimeUtil.formatTime(elapsed)
            ));
        }
    }

    public void handleFinish(Player player) {
        startCooldown.put(player.getUniqueId(), System.currentTimeMillis() + 3000L);
        ParkourSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        Course course = courseService.getCourse(session.getCourseId());
        long finalTime = session.getElapsedTimeMs();
        String formattedTime = TimeUtil.formatTime(finalTime);

        boolean isNewBest = leaderboardService.recordCompletion(
                session.getCourseId(),
                player.getName(),
                finalTime
        );

        session.restorePlayer(player);

        if (isNewBest) {
            playSound(player, "new-record");
            MessageUtil.broadcast(MessageUtil.format(
                    config.msg("new-record-broadcast"),
                    "player", player.getName(),
                    "course", course != null ? course.getDisplayName() : session.getCourseId(),
                    "time", formattedTime
            ));
        } else {
            playSound(player, "finish");
            MessageUtil.send(player, MessageUtil.format(
                    config.msg("finished"),
                    "course", course != null ? course.getDisplayName() : session.getCourseId(),
                    "time", formattedTime
            ));
        }

        // Show finish title
        String titleStr = config.getFinishTitle();
        String subTitleStr = MessageUtil.format(
                config.getFinishSubtitle(),
                "time", formattedTime,
                "new_best", isNewBest ? config.getNewBestTag() : ""
        );
        Title title = Title.title(
                MessageUtil.parse(titleStr),
                MessageUtil.parse(subTitleStr),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(500))
        );
        player.showTitle(title);
    }

    public void handleFall(Player player) {
        ParkourSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        Location cpLoc = session.getLastCheckpointLocation();
        if (cpLoc != null) {
            player.teleport(cpLoc);
            playSound(player, "fall");
            int cp = session.getCurrentCheckpointIndex() + 1;
            MessageUtil.send(player, MessageUtil.format(config.msg("fell"), "checkpoint", String.valueOf(cp)));
        }
    }

    private void startTimerTask(Player player, ParkourSession session, Course course) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !sessions.containsKey(player.getUniqueId())) {
                return;
            }
            long elapsed = session.getElapsedTimeMs();
            int cp = Math.max(0, session.getCurrentCheckpointIndex() + 1);
            String bar = MessageUtil.format(
                    config.getActionbarTimerFormat(),
                    "course", course.getDisplayName(),
                    "time", TimeUtil.formatTime(elapsed),
                    "checkpoint", String.valueOf(cp),
                    "total_checkpoints", String.valueOf(course.getCheckpoints().size())
            );
            player.sendActionBar(MessageUtil.parse(bar));
        }, 1L, 1L);
        session.setTimerTask(task);
    }

    public void giveHotbarItems(Player player) {
        clearInventoryKeepLastHotbar(player);
        placeHotbarItem(player, config.getRestartItem(), ActionItemType.RESTART);
        placeHotbarItem(player, config.getCheckpointItem(), ActionItemType.CHECKPOINT);
        placeHotbarItem(player, config.getQuitItem(), ActionItemType.QUIT);
    }

    private void placeHotbarItem(Player player, PluginConfig.HotbarItemConfig itemConfig, ActionItemType type) {
        // Never overwrite the last hotbar slot (index 8)
        if (itemConfig.slot() == 8) {
            return;
        }
        player.getInventory().setItem(itemConfig.slot(), createActionItem(itemConfig, type));
    }

    /** Clears inventory but leaves the last hotbar slot (index 8) untouched. */
    private static void clearInventoryKeepLastHotbar(Player player) {
        ItemStack lastHotbar = player.getInventory().getItem(8);
        player.getInventory().clear();
        if (lastHotbar != null && !lastHotbar.getType().isAir()) {
            player.getInventory().setItem(8, lastHotbar.clone());
        }
    }

    private ItemStack createActionItem(PluginConfig.HotbarItemConfig itemConfig, ActionItemType type) {
        ItemStack stack = new ItemStack(itemConfig.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse(itemConfig.name()));
            List<Component> lore = new ArrayList<>();
            for (String line : itemConfig.lore()) {
                lore.add(MessageUtil.parse(line));
            }
            meta.lore(lore);
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, type.name());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ActionItemType getActionItemType(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return null;
        }
        String typeName = stack.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (typeName == null) {
            return null;
        }
        try {
            return ActionItemType.valueOf(typeName);
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean isActionItem(ItemStack stack) {
        return getActionItemType(stack) != null;
    }

    public void playSound(Player player, String key) {
        Sound sound = config.getSound(key);
        if (sound != null && player != null) {
            player.playSound(player.getLocation(), sound, config.getSoundVolume(key), config.getSoundPitch(key));
        }
    }

    public void shutdown() {
        for (UUID uuid : new ArrayList<>(sessions.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                quitParkour(player, false);
            }
        }
        sessions.clear();
    }
}
