package com.frostsmp.speed.command;

import com.frostsmp.speed.FrostSpeedPlugin;
import com.frostsmp.speed.config.SpeedPotionConfig;
import com.frostsmp.speed.item.SpeedPotionItem;
import com.frostsmp.speed.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpeedCommand implements CommandExecutor, TabCompleter {

    private final FrostSpeedPlugin plugin;
    private final SpeedPotionConfig config;

    public SpeedCommand(FrostSpeedPlugin plugin, SpeedPotionConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("frostsmp.speed.admin")) {
            MessageUtil.send(sender, config.prefixed("no-permission"));
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(sender, config.prefixed("usage"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            plugin.reloadAll();
            MessageUtil.send(sender, config.prefixed("reloaded"));
            return true;
        }
        if (!sub.equals("give")) {
            MessageUtil.send(sender, config.prefixed("usage"));
            return true;
        }
        Player target;
        int amount = 1;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtil.send(sender, config.prefixed("player-offline"));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            MessageUtil.send(sender, config.prefixed("players-only"));
            return true;
        }
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException exception) {
                MessageUtil.send(sender, config.prefixed("usage"));
                return true;
            }
        }
        ItemStack potion = SpeedPotionItem.create(amount);
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(potion);
        leftover.values().forEach(stack -> target.getWorld().dropItemNaturally(target.getLocation(), stack));
        MessageUtil.send(sender, config.prefixed("given",
                "amount", String.valueOf(amount),
                "player", target.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("frostsmp.speed.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("give", "reload").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
