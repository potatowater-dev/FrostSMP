package com.frostsmp.clear.service;

import com.frostsmp.clear.config.PluginConfig;
import com.frostsmp.clear.config.PluginConfig.TaskSettings;
import com.frostsmp.clear.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class ClearScheduler {

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private BukkitTask task;
    private int itemsSecondsRemaining;
    private int blocksSecondsRemaining;

    public ClearScheduler(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void start() {
        stop();
        itemsSecondsRemaining = config.getItems().getIntervalSeconds();
        blocksSecondsRemaining = config.getBlocks().getIntervalSeconds();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        itemsSecondsRemaining = tickTask(config.getItems(), itemsSecondsRemaining);
        blocksSecondsRemaining = tickTask(config.getBlocks(), blocksSecondsRemaining);
    }

    private int tickTask(TaskSettings settings, int remaining) {
        if (!settings.isEnabled()) {
            return remaining;
        }
        remaining--;
        if (settings.getWarnings().contains(remaining)) {
            warn(settings, remaining);
        }
        if (remaining <= 0) {
            dispatch(settings.getCommand());
            return settings.getIntervalSeconds();
        }
        return remaining;
    }

    private void warn(TaskSettings settings, int seconds) {
        String text = MessageUtil.format(
                config.getWarningMessage(),
                "label", settings.getLabel(),
                "time", seconds + "s"
        );
        Component message = MessageUtil.parse(text);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(message);
            if (settings.isSoundEnabled()) {
                player.playSound(player.getLocation(), settings.getSound(), settings.getVolume(), settings.getPitch());
            }
        }
    }

    private void dispatch(String command) {
        if (command == null || command.isBlank()) {
            plugin.getLogger().warning("Skipped empty clear command.");
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        plugin.getLogger().info("Ran /" + command);
    }
}
