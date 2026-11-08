package com.frostsmp.rejoin.command;

import com.frostsmp.rejoin.FrostRejoinPlugin;
import com.frostsmp.rejoin.config.PluginConfig;
import com.frostsmp.rejoin.service.RejoinService;
import com.frostsmp.rejoin.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RejoinCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "frostsmp.rejoin.admin";

    private final FrostRejoinPlugin plugin;
    private final PluginConfig config;
    private final RejoinService rejoinService;

    public RejoinCommand(FrostRejoinPlugin plugin, PluginConfig config, RejoinService rejoinService) {
        this.plugin = plugin;
        this.config = config;
        this.rejoinService = rejoinService;
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
            case "now" -> rejoinService.forceNow();
            case "status" -> {
                MessageUtil.send(sender, MessageUtil.format(
                        config.getStatusLine(),
                        "count", String.valueOf(rejoinService.getPendingCount()),
                        "state", rejoinService.getState().name(),
                        "host", config.getReturnHost(),
                        "port", String.valueOf(config.getReturnPort())
                ));
            }
            case "reload" -> {
                plugin.reloadAll();
                MessageUtil.send(sender, config.getReloaded());
            }
            default -> MessageUtil.send(sender, config.getUsage());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission(PERM)) {
            return out;
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String option : List.of("now", "status", "reload")) {
                if (option.startsWith(prefix)) {
                    out.add(option);
                }
            }
        }
        return out;
    }
}
