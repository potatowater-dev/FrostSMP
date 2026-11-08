package com.frostsmp.combat.command;

import com.frostsmp.combat.FrostCombatPlugin;
import com.frostsmp.combat.service.CombatService;
import com.frostsmp.combat.service.PlayerStatsService;
import com.frostsmp.combat.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class FrostCombatCommand implements CommandExecutor, TabCompleter {

    private final FrostCombatPlugin plugin;
    private final CombatService combatService;
    private final PlayerStatsService playerStatsService;

    public FrostCombatCommand(
            FrostCombatPlugin plugin,
            CombatService combatService,
            PlayerStatsService playerStatsService
    ) {
        this.plugin = plugin;
        this.combatService = combatService;
        this.playerStatsService = playerStatsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("frostsmp.combat.admin")) {
            MessageUtil.send(sender, "<red>You do not have permission.</red>");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadPluginConfig();
                MessageUtil.send(sender, "<green>FrostCombat config reloaded.</green>");
            }
            case "untag" -> handleUntag(sender, args);
            case "resetstats", "resetleaderboard", "resetboards" -> {
                int statsCleared = playerStatsService.resetAll();
                int streaksCleared = 0;
                if (plugin.getKillStreakService() != null) {
                    streaksCleared = plugin.getKillStreakService().resetAll();
                }
                MessageUtil.send(sender,
                        "<green>Reset FrostCombat boards.</green> <gray>player-stats:</gray> <white>"
                                + statsCleared + "</white> <gray>· killstreaks:</gray> <white>"
                                + streaksCleared + "</white>");
                if (sender instanceof Player player) {
                    MessageUtil.send(sender,
                            "<dark_gray>Your live PAPI now:</dark_gray> <white>kills="
                                    + playerStatsService.value(PlayerStatsService.Metric.KILLS, player.getUniqueId())
                                    + " deaths="
                                    + playerStatsService.value(PlayerStatsService.Metric.DEATHS, player.getUniqueId())
                                    + " ks="
                                    + (plugin.getKillStreakService() == null
                                    ? 0
                                    : plugin.getKillStreakService().current(player.getUniqueId()))
                                    + "</white>");
                }
                MessageUtil.send(sender,
                        "<gray>If holograms still show old numbers, they are still using</gray> <red>%asleaderboard_*%</red>");
                MessageUtil.send(sender,
                        "<gray>or FancyHolograms has a long update interval. Wallet:</gray> <white>/wallet reset</white>");
            }
            case "checkpapi" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.send(sender, "<red>Players only.</red>");
                    return true;
                }
                MessageUtil.send(sender, "<gold>FrostCombat PAPI values for you:</gold>");
                MessageUtil.send(sender, "<gray>kills=</gray><white>"
                        + playerStatsService.value(PlayerStatsService.Metric.KILLS, player.getUniqueId()) + "</white>");
                MessageUtil.send(sender, "<gray>deaths=</gray><white>"
                        + playerStatsService.value(PlayerStatsService.Metric.DEATHS, player.getUniqueId()) + "</white>");
                MessageUtil.send(sender, "<gray>playtime=</gray><white>"
                        + PlayerStatsService.formatPlaytime(
                        playerStatsService.value(PlayerStatsService.Metric.PLAYTIME, player.getUniqueId()))
                        + "</white>");
                MessageUtil.send(sender, "<gray>killstreak=</gray><white>"
                        + (plugin.getKillStreakService() == null
                        ? 0
                        : plugin.getKillStreakService().current(player.getUniqueId()))
                        + "</white>");
                MessageUtil.send(sender,
                        "<dark_gray>Compare with: /papi parse me %frostcombat_kills% and %asleaderboard_value_kills%</dark_gray>");
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleUntag(CommandSender sender, String[] args) {
        Player target;
        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                MessageUtil.send(sender, "<yellow>Usage: /frostcombat untag <player></yellow>");
                return;
            }
            target = player;
        } else {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtil.send(sender, "<red>Player not found.</red>");
                return;
            }
        }
        combatService.clear(target);
        if (target.equals(sender)) {
            MessageUtil.send(sender, "<green>You were removed from combat.</green>");
        } else {
            MessageUtil.send(sender, "<green>Removed " + target.getName() + " from combat.</green>");
            MessageUtil.send(target, "<green>An operator removed you from combat.</green>");
        }
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.send(sender,
                "<yellow>Usage: /frostcombat <reload|untag [player]|resetstats|checkpapi></yellow>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("frostsmp.combat.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            for (String option : List.of("reload", "untag", "resetstats", "checkpapi")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    options.add(option);
                }
            }
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("untag")) {
            List<String> names = new ArrayList<>();
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    names.add(online.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
