package com.frostsmp.juggernaut.command;

import com.frostsmp.juggernaut.FrostJuggernautPlugin;
import com.frostsmp.juggernaut.config.PluginConfig;
import com.frostsmp.juggernaut.gui.ManageGui;
import com.frostsmp.juggernaut.service.DamageTracker;
import com.frostsmp.juggernaut.service.EventService;
import com.frostsmp.juggernaut.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JuggernautCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "frostsmp.juggernaut.admin";
    private static final String PERM_TOP = "frostsmp.juggernaut.top";
    private static final int TOP_LIMIT = 10;

    private final FrostJuggernautPlugin plugin;
    private final PluginConfig config;
    private final EventService eventService;

    public JuggernautCommand(FrostJuggernautPlugin plugin, PluginConfig config, EventService eventService) {
        this.plugin = plugin;
        this.config = config;
        this.eventService = eventService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("top")) {
            if (!sender.hasPermission(PERM_TOP) && !sender.hasPermission(PERM)) {
                MessageUtil.send(sender, config.getNoPermission());
                return true;
            }
            sendTop(sender);
            return true;
        }

        if (!sender.hasPermission(PERM)) {
            MessageUtil.send(sender, config.getNoPermission());
            return true;
        }
        if (sub.isEmpty()) {
            MessageUtil.send(sender, config.getUsage());
            return true;
        }

        switch (sub) {
            case "manage" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, config.getPlayersOnly());
                    return true;
                }
                player.openInventory(new ManageGui(eventService, config).getInventory());
            }
            case "status" -> sendStatus(sender);
            case "stop" -> eventService.stop(sender, false);
            case "reload" -> {
                plugin.reloadPluginConfig();
                MessageUtil.send(sender, config.getReloaded());
            }
            default -> MessageUtil.send(sender, config.getUsage());
        }
        return true;
    }

    private void sendStatus(CommandSender sender) {
        String template = eventService.isRunning() ? config.getStatusRunning() : config.getStatusIdle();
        MessageUtil.send(sender, MessageUtil.format(template,
                "state", eventService.getState().name(),
                "juggernaut", eventService.getJuggernautNameList(),
                "alive", String.valueOf(eventService.getAliveJuggernautCount()),
                "total", String.valueOf(eventService.getJuggernautCount())));
    }

    private void sendTop(CommandSender sender) {
        List<DamageTracker.Entry> leaderboard = eventService.getDamageTracker().getLeaderboard(TOP_LIMIT);
        if (leaderboard.isEmpty()) {
            MessageUtil.send(sender, config.getTopEmpty());
            return;
        }
        MessageUtil.send(sender, config.getTopHeader());
        int rank = 1;
        for (DamageTracker.Entry entry : leaderboard) {
            MessageUtil.send(sender, MessageUtil.format(config.getTopLine(),
                    "rank", String.valueOf(rank),
                    "player", entry.name() != null ? entry.name() : "Unknown",
                    "damage", MessageUtil.formatDamage(entry.damage())));
            rank++;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (sender.hasPermission(PERM)) {
            options.addAll(List.of("manage", "status", "stop", "reload"));
        }
        if (sender.hasPermission(PERM_TOP) || sender.hasPermission(PERM)) {
            options.add("top");
        }
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix)) {
                result.add(option);
            }
        }
        return result;
    }
}
