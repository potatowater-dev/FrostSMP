package com.frostsmp.clear.command;

import com.frostsmp.clear.FrostClearPlugin;
import com.frostsmp.clear.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

public final class ClearCommand implements CommandExecutor, TabCompleter {

    private final FrostClearPlugin plugin;

    public ClearCommand(FrostClearPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("frostsmp.clear.admin")) {
            MessageUtil.send(sender, plugin.getPluginConfig().getNoPermission());
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            MessageUtil.send(sender, plugin.getPluginConfig().getReloaded());
            return true;
        }
        MessageUtil.send(sender, plugin.getPluginConfig().getUsage());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("frostsmp.clear.admin")) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(prefix)) {
                return List.of("reload");
            }
        }
        return List.of();
    }
}
