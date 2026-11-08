package com.frostsmp.afk.command;

import com.frostsmp.afk.FrostAfkPlugin;
import com.frostsmp.afk.config.PluginConfig;
import com.frostsmp.afk.service.WandService;
import com.frostsmp.afk.storage.RegionStorage;
import com.frostsmp.afk.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FrostAfkCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "frostsmp.afk.admin";

    private final FrostAfkPlugin plugin;
    private final PluginConfig config;
    private final RegionStorage regionStorage;
    private final WandService wandService;

    public FrostAfkCommand(
            FrostAfkPlugin plugin,
            PluginConfig config,
            RegionStorage regionStorage,
            WandService wandService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.regionStorage = regionStorage;
        this.wandService = wandService;
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
            case "pos1" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, config.getPlayersOnly());
                    return true;
                }
                regionStorage.setPos1(player.getLocation());
                MessageUtil.send(player, config.getPos1Set().replace("{location}", RegionStorage.format(player.getLocation())));
            }
            case "pos2" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, config.getPlayersOnly());
                    return true;
                }
                regionStorage.setPos2(player.getLocation());
                MessageUtil.send(player, config.getPos2Set().replace("{location}", RegionStorage.format(player.getLocation())));
            }
            case "setspawn" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, config.getPlayersOnly());
                    return true;
                }
                regionStorage.setSpawn(player.getLocation());
                MessageUtil.send(player, config.getSpawnSet().replace("{location}", RegionStorage.format(player.getLocation())));
            }
            case "info" -> {
                MessageUtil.send(sender, config.getInfoHeader());
                String region = regionStorage.formatRegion();
                MessageUtil.send(sender, config.getInfoRegion().replace(
                        "{region}",
                        region == null ? config.getInfoNone() : region
                ));
                Location spawn = regionStorage.getSpawn();
                String spawnText = spawn != null
                        ? RegionStorage.format(spawn)
                        : (regionStorage.isComplete()
                        ? RegionStorage.format(regionStorage.getTeleportTarget()) + " (auto)"
                        : config.getInfoNone());
                MessageUtil.send(sender, config.getInfoSpawn().replace("{spawn}", spawnText));
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
            for (String option : List.of("wand", "pos1", "pos2", "setspawn", "info", "reload")) {
                if (option.startsWith(prefix)) {
                    out.add(option);
                }
            }
        }
        return out;
    }
}
