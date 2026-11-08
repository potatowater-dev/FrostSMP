package com.frostsmp.store.command;

import com.frostsmp.store.FrostStorePlugin;
import com.frostsmp.store.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

public final class StoreCommand implements CommandExecutor, TabCompleter {

    private final FrostStorePlugin plugin;

    public StoreCommand(FrostStorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("frostsmp.store.admin")) {
            MessageUtil.send(sender, "<red>No permission.</red>");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            MessageUtil.send(sender, "<green>FrostStore reloaded.</green>");
            return true;
        }
        MessageUtil.send(sender, "<gray>Usage: /froststore reload</gray>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("frostsmp.store.admin") || args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return List.of("reload").stream().filter(o -> o.startsWith(prefix)).toList();
    }
}
