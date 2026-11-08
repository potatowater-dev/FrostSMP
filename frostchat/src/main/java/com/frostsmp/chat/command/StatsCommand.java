package com.frostsmp.chat.command;

import com.frostsmp.chat.FrostChatPlugin;
import com.frostsmp.chat.gui.StatsGui;
import com.frostsmp.chat.service.StatsService;
import com.frostsmp.chat.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StatsCommand implements CommandExecutor, TabCompleter {

    private final FrostChatPlugin plugin;
    private final StatsService statsService;

    public StatsCommand(FrostChatPlugin plugin, StatsService statsService) {
        this.plugin = plugin;
        this.statsService = statsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.prefixed("players-only"));
            return true;
        }
        if (!player.hasPermission("frostsmp.chat.stats")) {
            MessageUtil.send(sender, plugin.prefixed("no-permission"));
            return true;
        }

        OfflinePlayer target;
        if (args.length == 0) {
            target = player;
        } else {
            if (!player.hasPermission("frostsmp.chat.stats.others")) {
                MessageUtil.send(sender, plugin.getConfig().getString("messages.prefix", "")
                        + plugin.getConfig().getString(
                                "messages.stats-others-denied",
                                "<red>You can't view other players' stats.</red>"
                        ));
                return true;
            }
            target = StatsService.resolveTarget(args[0]);
            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                MessageUtil.send(sender, plugin.getConfig().getString("messages.prefix", "")
                        + MessageUtil.format(
                                plugin.getConfig().getString(
                                        "messages.stats-not-found",
                                        "<red>Player <white>{player}</white> not found.</red>"
                                ),
                                "player", args[0]
                        ));
                return true;
            }
        }

        StatsService.PlayerStats stats = statsService.load(target);
        player.openInventory(StatsGui.create(plugin, target, stats));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission("frostsmp.chat.stats.others")) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(online.getName());
            }
        }
        return names;
    }
}
