package com.frostsmp.chat.command;

import com.frostsmp.chat.FrostChatPlugin;
import com.frostsmp.chat.listener.ChatTagListener;
import com.frostsmp.chat.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FrostChatCommand implements CommandExecutor, TabCompleter {

    private final FrostChatPlugin plugin;

    public FrostChatCommand(FrostChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("viewec")) {
            if (!(sender instanceof Player player)) {
                MessageUtil.send(sender, plugin.prefixed("players-only"));
                return true;
            }
            Inventory inventory = plugin.getSnapshots().getView(args[1]);
            if (inventory == null) {
                MessageUtil.send(sender, plugin.prefixed("invalid-snapshot"));
                return true;
            }
            player.openInventory(inventory);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("viewinv")) {
            if (!(sender instanceof Player player)) {
                MessageUtil.send(sender, plugin.prefixed("players-only"));
                return true;
            }
            Inventory inventory = plugin.getSnapshots().getView(args[1]);
            if (inventory == null) {
                MessageUtil.send(sender, plugin.prefixed("invalid-snapshot"));
                return true;
            }
            player.openInventory(inventory);
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("viewwallet")) {
            if (!(sender instanceof Player player)) {
                MessageUtil.send(sender, plugin.prefixed("players-only"));
                return true;
            }
            UUID targetId;
            try {
                targetId = UUID.fromString(args[1]);
            } catch (IllegalArgumentException exception) {
                MessageUtil.send(sender, plugin.prefixed("invalid-snapshot"));
                return true;
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(targetId);
            String name = offline.getName() == null ? "Unknown" : offline.getName();
            player.openInventory(plugin.getWalletPreviews().createWalletView(targetId, name));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("test")) {
            if (!(sender instanceof Player player)) {
                MessageUtil.send(sender, plugin.prefixed("players-only"));
                return true;
            }
            if (!sender.hasPermission("frostsmp.chat.admin") && !sender.isOp()) {
                MessageUtil.send(sender, plugin.prefixed("no-permission"));
                return true;
            }
            ChatTagListener tags = plugin.getChatTagListener();
            if (tags == null) {
                MessageUtil.send(sender, "<red>Chat tag listener is not loaded.</red>");
                return true;
            }
            Component line = tags.buildTaggedLine(player, "[wallet] [item] frostchat-test");
            // Send directly to everyone — proves rendering without depending on chat events.
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(line);
            }
            MessageUtil.send(sender, plugin.getConfig().getString("messages.prefix", "")
                    + "<green>Sent tagged test line to all online players.</green>");
            return true;
        }

        if (!sender.hasPermission("frostsmp.chat.admin") && !sender.isOp()) {
            MessageUtil.send(sender, plugin.prefixed("no-permission"));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            MessageUtil.send(sender, plugin.prefixed("reloaded"));
            return true;
        }
        MessageUtil.send(sender, plugin.getConfig().getString("messages.prefix", "")
                + "<gray>Usage: /frostchat <reload|test></gray>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("frostsmp.chat.admin") && !sender.isOp()) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("reload", "test").stream().filter(option -> option.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
