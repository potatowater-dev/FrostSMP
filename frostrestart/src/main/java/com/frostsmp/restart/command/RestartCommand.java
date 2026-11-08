package com.frostsmp.restart.command;

import com.frostsmp.restart.FrostRestartPlugin;
import com.frostsmp.restart.config.PluginConfig;
import com.frostsmp.restart.service.RestartScheduler;
import com.frostsmp.restart.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RestartCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "frostsmp.restart.admin";

    private final PluginConfig config;
    private final RestartScheduler scheduler;

    public RestartCommand(FrostRestartPlugin plugin) {
        this.config = plugin.getPluginConfig();
        this.scheduler = plugin.getRestartScheduler();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            MessageUtil.send(sender, config.getNoPermission());
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(sender, config.getUsage());
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "now" -> scheduler.startNow(sender);
            case "start" -> {
                int minutes = config.getDefaultMinutes();
                if (args.length >= 2) {
                    try {
                        minutes = Integer.parseInt(args[1]);
                    } catch (NumberFormatException exception) {
                        MessageUtil.send(sender, config.getUsage());
                        return true;
                    }
                }
                if (minutes < 1) {
                    MessageUtil.send(sender, config.getUsage());
                    return true;
                }
                scheduler.start(minutes, sender);
            }
            case "cancel" -> scheduler.cancel(sender);
            case "status" -> {
                if (!scheduler.isScheduled()) {
                    MessageUtil.send(sender, config.getNotScheduled());
                    return true;
                }
                MessageUtil.send(sender, MessageUtil.format(
                        config.getStatus(),
                        "time",
                        RestartScheduler.formatDuration(scheduler.getSecondsRemaining())
                ));
            }
            default -> MessageUtil.send(sender, config.getUsage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERM)) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> options = List.of("start", "now", "cancel", "status");
            List<String> result = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(prefix)) {
                    result.add(option);
                }
            }
            return result;
        }
        if (args.length == 2 && "start".equalsIgnoreCase(args[0])) {
            return List.of("1", "5", "10", "15");
        }
        return List.of();
    }
}
