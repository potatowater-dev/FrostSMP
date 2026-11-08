package com.frostsmp.colosseum.command;

import com.frostsmp.colosseum.FrostColosseumPlugin;
import com.frostsmp.colosseum.config.PluginConfig;
import com.frostsmp.colosseum.gui.AdminGui;
import com.frostsmp.colosseum.gui.RewardGui;
import com.frostsmp.colosseum.service.MatchScheduler;
import com.frostsmp.colosseum.service.MatchService;
import com.frostsmp.colosseum.service.QueueService;
import com.frostsmp.colosseum.service.WandService;
import com.frostsmp.colosseum.storage.RegionStorage;
import com.frostsmp.colosseum.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ColoCommand implements CommandExecutor, TabCompleter {

    private final FrostColosseumPlugin plugin;
    private final PluginConfig config;
    private final RegionStorage regionStorage;
    private final WandService wandService;
    private final MatchService matchService;
    private final MatchScheduler scheduler;
    private final QueueService queueService;
    private final AdminGui adminGui;
    private final RewardGui rewardGui;

    public ColoCommand(
            FrostColosseumPlugin plugin,
            PluginConfig config,
            RegionStorage regionStorage,
            WandService wandService,
            MatchService matchService,
            MatchScheduler scheduler,
            QueueService queueService,
            AdminGui adminGui,
            RewardGui rewardGui
    ) {
        this.plugin = plugin;
        this.config = config;
        this.regionStorage = regionStorage;
        this.wandService = wandService;
        this.matchService = matchService;
        this.scheduler = scheduler;
        this.queueService = queueService;
        this.adminGui = adminGui;
        this.rewardGui = rewardGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        if (cmd.equals("setreward")) {
            return handleSetReward(sender);
        }

        if (args.length == 0) {
            MessageUtil.send(sender, config.rawMsg("usage"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "status" -> handleStatus(sender);
            case "wand" -> handleWand(sender);
            case "setregion" -> handleSetRegion(sender, args);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "setreward" -> handleSetReward(sender);
            case "admin" -> handleAdmin(sender);
            case "forcestart" -> handleForceStart(sender);
            case "forceend" -> handleForceEnd(sender);
            case "reload" -> handleReload(sender);
            default -> {
                MessageUtil.send(sender, config.rawMsg("usage"));
                yield true;
            }
        };
    }

    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("frostsmp.colo.use") && !sender.hasPermission("frostsmp.colo.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return true;
        }
        MessageUtil.send(sender, MessageUtil.format(
                config.msg("status"),
                "state", matchService.getState().name(),
                "next", scheduler.formatCountdown(),
                "red", String.valueOf(queueService.getRedCount()),
                "blue", String.valueOf(queueService.getBlueCount())
        ));
        return true;
    }

    private boolean handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return true;
        }
        if (!player.hasPermission("frostsmp.colo.admin")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return true;
        }
        wandService.giveWand(player);
        return true;
    }

    private boolean handleSetRegion(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return true;
        }
        if (!player.hasPermission("frostsmp.colo.admin")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(player, config.msg("unknown-region"));
            return true;
        }
        String name = args[1].toLowerCase(Locale.ROOT);
        if (!RegionStorage.REGION_NAMES.contains(name)) {
            MessageUtil.send(player, config.msg("unknown-region"));
            return true;
        }
        if (!regionStorage.hasSelection()) {
            MessageUtil.send(player, config.msg("need-selection"));
            return true;
        }
        regionStorage.setRegionFromSelection(name);
        MessageUtil.send(player, MessageUtil.format(config.msg("region-set"), "region", name));
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return true;
        }
        if (!player.hasPermission("frostsmp.colo.admin")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(player, config.msg("unknown-spawn"));
            return true;
        }
        String key = switch (args[1].toLowerCase(Locale.ROOT)) {
            case "red" -> "spawn_red";
            case "blue" -> "spawn_blue";
            case "exit" -> "exit";
            default -> null;
        };
        if (key == null) {
            MessageUtil.send(player, config.msg("unknown-spawn"));
            return true;
        }
        regionStorage.setPoint(key, player.getLocation());
        MessageUtil.send(player, MessageUtil.format(config.msg("spawn-set"), "spawn", args[1].toLowerCase(Locale.ROOT)));
        return true;
    }

    private boolean handleSetReward(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return true;
        }
        if (!player.hasPermission("frostsmp.colo.admin")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return true;
        }
        rewardGui.open(player);
        return true;
    }

    private boolean handleAdmin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.msg("players-only"));
            return true;
        }
        if (!player.hasPermission("frostsmp.colo.admin")) {
            MessageUtil.send(player, config.msg("no-permission"));
            return true;
        }
        adminGui.open(player);
        return true;
    }

    private boolean handleForceStart(CommandSender sender) {
        if (!sender.hasPermission("frostsmp.colo.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return true;
        }
        if (!regionStorage.isFullyConfigured()) {
            MessageUtil.send(sender, MessageUtil.format(
                    config.msg("incomplete-setup"),
                    "missing", String.join(", ", regionStorage.missingSetup())
            ));
            return true;
        }
        if (matchService.tryStart(true)) {
            MessageUtil.send(sender, config.msg("force-start"));
        } else {
            MessageUtil.send(sender, config.msg("skip-round"));
        }
        return true;
    }

    private boolean handleForceEnd(CommandSender sender) {
        if (!sender.hasPermission("frostsmp.colo.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return true;
        }
        matchService.forceEnd(true);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("frostsmp.colo.admin")) {
            MessageUtil.send(sender, config.msg("no-permission"));
            return true;
        }
        plugin.reloadPlugin();
        MessageUtil.send(sender, config.msg("reloaded"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("setreward")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("status");
            if (sender.hasPermission("frostsmp.colo.admin")) {
                options.addAll(Arrays.asList(
                        "wand", "setregion", "setspawn", "setreward", "admin", "forcestart", "forceend", "reload"
                ));
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && sender.hasPermission("frostsmp.colo.admin")) {
            if (args[0].equalsIgnoreCase("setregion")) {
                return filter(new ArrayList<>(RegionStorage.REGION_NAMES), args[1]);
            }
            if (args[0].equalsIgnoreCase("setspawn")) {
                return filter(Arrays.asList("red", "blue", "exit"), args[1]);
            }
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).collect(Collectors.toList());
    }
}
