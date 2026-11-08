package com.frostsmp.event.command;

import com.frostsmp.event.FrostEventPlugin;
import com.frostsmp.event.config.PluginConfig;
import com.frostsmp.event.service.EventService;
import com.frostsmp.event.storage.WinnerStorage;
import com.frostsmp.event.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EventCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_ADMIN = "frostsmp.event.admin";
    private static final String PERM_WINNER = "frostsmp.event.winner";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final FrostEventPlugin plugin;
    private final PluginConfig config;
    private final EventService eventService;
    private final WinnerStorage winnerStorage;

    public EventCommand(
            FrostEventPlugin plugin,
            PluginConfig config,
            EventService eventService,
            WinnerStorage winnerStorage
    ) {
        this.plugin = plugin;
        this.config = config;
        this.eventService = eventService;
        this.winnerStorage = winnerStorage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, config.getUsage());
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> {
                if (!sender.hasPermission(PERM_ADMIN)) {
                    MessageUtil.send(sender, config.getNoPermission());
                    return true;
                }
                if (eventService.isRunning()) {
                    MessageUtil.send(sender, config.getAlreadyRunning());
                    return true;
                }
                double hours = config.getDefaultHours();
                if (args.length >= 2) {
                    try {
                        hours = Double.parseDouble(args[1]);
                    } catch (NumberFormatException exception) {
                        MessageUtil.send(sender, config.getUsage());
                        return true;
                    }
                    if (hours <= 0) {
                        MessageUtil.send(sender, "<red>Hours must be positive.</red>");
                        return true;
                    }
                }
                eventService.start(hours);
            }
            case "stop" -> {
                if (!sender.hasPermission(PERM_ADMIN)) {
                    MessageUtil.send(sender, config.getNoPermission());
                    return true;
                }
                if (!eventService.stop(true)) {
                    MessageUtil.send(sender, config.getNotRunning());
                }
            }
            case "status" -> {
                if (!eventService.isRunning()) {
                    MessageUtil.send(sender, config.getStatusIdle());
                    return true;
                }
                MessageUtil.send(sender, MessageUtil.format(
                        config.getStatusRunning(),
                        "remaining", eventService.formatRemaining(),
                        "players", String.valueOf(eventService.getKillsSnapshot().size())
                ));
            }
            case "winner" -> {
                if (!sender.hasPermission(PERM_WINNER)) {
                    MessageUtil.send(sender, config.getNoPermission());
                    return true;
                }
                WinnerStorage.Winner winner = winnerStorage.getLastWinner();
                if (winner == null) {
                    MessageUtil.send(sender, config.getWinnerNone());
                    return true;
                }
                MessageUtil.send(sender, MessageUtil.format(
                        config.getWinner(),
                        "winner", winner.name(),
                        "kills", String.valueOf(winner.kills()),
                        "when", WHEN.format(Instant.ofEpochMilli(winner.endedAtEpochMs()))
                ));
            }
            case "kills" -> {
                if (!sender.hasPermission(PERM_WINNER)) {
                    MessageUtil.send(sender, config.getNoPermission());
                    return true;
                }
                if (!eventService.isRunning()) {
                    MessageUtil.send(sender, config.getNotRunning());
                    return true;
                }
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        MessageUtil.send(sender, "<red>Player not found.</red>");
                        return true;
                    }
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    MessageUtil.send(sender, config.getUsage());
                    return true;
                }
                int kills = eventService.getKills(target.getUniqueId());
                if (kills <= 0) {
                    MessageUtil.send(sender, config.getKillsNone());
                    return true;
                }
                if (sender instanceof Player self && self.getUniqueId().equals(target.getUniqueId())) {
                    MessageUtil.send(sender, MessageUtil.format(config.getKillsSelf(), "kills", String.valueOf(kills)));
                } else {
                    MessageUtil.send(sender, MessageUtil.format(
                            config.getKillsOther(),
                            "player", target.getName(),
                            "kills", String.valueOf(kills)
                    ));
                }
            }
            case "reload" -> {
                if (!sender.hasPermission(PERM_ADMIN)) {
                    MessageUtil.send(sender, config.getNoPermission());
                    return true;
                }
                plugin.reloadAll();
                MessageUtil.send(sender, "<green>FrostEvent reloaded.</green>");
            }
            default -> MessageUtil.send(sender, config.getUsage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>(List.of("status", "winner", "kills"));
            if (sender.hasPermission(PERM_ADMIN)) {
                options.addAll(List.of("start", "stop", "reload"));
            }
            for (String option : options) {
                if (option.startsWith(prefix)) {
                    out.add(option);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start") && sender.hasPermission(PERM_ADMIN)) {
            return filter(args[1], List.of("1", "3", "6", "12"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("kills")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    out.add(player.getName());
                }
            }
        }
        return out;
    }

    private static List<String> filter(String input, List<String> options) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(option);
            }
        }
        return out;
    }
}
