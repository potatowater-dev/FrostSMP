package com.frostsmp.beacons.command;

import com.frostsmp.beacons.FrostBeaconsPlugin;
import com.frostsmp.beacons.config.PluginConfig;
import com.frostsmp.beacons.service.WandService;
import com.frostsmp.beacons.storage.BeamLinkStorage;
import com.frostsmp.beacons.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FrostBeaconsCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "frostsmp.beacons.admin";

    private final FrostBeaconsPlugin plugin;
    private final PluginConfig config;
    private final WandService wandService;
    private final BeamLinkStorage beamLinks;

    public FrostBeaconsCommand(
            FrostBeaconsPlugin plugin,
            PluginConfig config,
            WandService wandService,
            BeamLinkStorage beamLinks
    ) {
        this.plugin = plugin;
        this.config = config;
        this.wandService = wandService;
        this.beamLinks = beamLinks;
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
            case "wand" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, config.getPlayersOnly());
                    return true;
                }
                wandService.giveWand(player);
            }
            case "list" -> {
                MessageUtil.send(sender, config.getListHeader());
                Map<String, Location> snapshot = beamLinks.snapshot();
                if (snapshot.isEmpty()) {
                    MessageUtil.send(sender, config.getListEmpty());
                    break;
                }
                for (Map.Entry<String, Location> entry : snapshot.entrySet()) {
                    Location start = parseBeaconKey(entry.getKey());
                    Location end = entry.getValue();
                    if (start == null || end == null) {
                        continue;
                    }
                    MessageUtil.send(sender, "<white>"
                            + BeamLinkStorage.format(start)
                            + "</white> <gray>→</gray> <white>"
                            + BeamLinkStorage.format(end)
                            + "</white>");
                }
            }
            case "reload" -> {
                plugin.reloadAll();
                MessageUtil.send(sender, config.getReloaded());
            }
            default -> MessageUtil.send(sender, config.getUsage());
        }
        return true;
    }

    private static Location parseBeaconKey(String key) {
        String[] parts = key.split(";");
        if (parts.length != 4) {
            return null;
        }
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERM)) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String option : List.of("wand", "list", "reload")) {
                if (option.startsWith(prefix)) {
                    result.add(option);
                }
            }
            return result;
        }
        return List.of();
    }
}
