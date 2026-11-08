package com.frost.plots.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class SchedulerUtil {

    private final Plugin plugin;

    public SchedulerUtil(Plugin plugin) {
        this.plugin = plugin;
    }

    public void sync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public void async(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
}
