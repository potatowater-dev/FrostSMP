package com.frostsmp.bounty.command;

import com.frostsmp.bounty.FrostBountyPlugin;
import com.frostsmp.bounty.config.PluginConfig;
import com.frostsmp.bounty.service.BountyService;
import com.frostsmp.bounty.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class BountyCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_USE = "frostsmp.bounty.use";
    private static final String PERM_ADMIN = "frostsmp.bounty.admin";

    private final FrostBountyPlugin plugin;
    private final PluginConfig config;
    private final BountyService bountyService;

    public BountyCommand(FrostBountyPlugin plugin, PluginConfig config, BountyService bountyService) {
        this.plugin = plugin;
        this.config = config;
        this.bountyService = bountyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(sender, config.getUsage());
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "place" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, config.getPlayersOnly());
                    return true;
                }
                if (!player.hasPermission(PERM_USE)) {
                    MessageUtil.send(player, config.getNoPermission());
                    return true;
                }
                if (args.length < 2) {
                    MessageUtil.send(player, config.getUsage());
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    MessageUtil.send(player, config.getPlayerNotFound().replace("{player}", args[1]));
                    return true;
                }
                bountyService.openPlaceGui(player, target);
            }
            case "list" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, config.getPlayersOnly());
                    return true;
                }
                if (!player.hasPermission(PERM_USE)) {
                    MessageUtil.send(player, config.getNoPermission());
                    return true;
                }
                bountyService.openListGui(player);
            }
            case "reload" -> {
                if (!sender.hasPermission(PERM_ADMIN)) {
                    MessageUtil.send(sender, config.getNoPermission());
                    return true;
                }
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
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String option : List.of("place", "list", "reload")) {
                if (option.startsWith(prefix)) {
                    if (option.equals("reload") && !sender.hasPermission(PERM_ADMIN)) {
                        continue;
                    }
                    out.add(option);
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("place")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return out;
    }
}
