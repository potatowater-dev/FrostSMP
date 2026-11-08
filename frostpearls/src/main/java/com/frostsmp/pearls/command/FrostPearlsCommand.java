package com.frostsmp.pearls.command;

import com.frostsmp.pearls.FrostPearlsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FrostPearlsCommand implements CommandExecutor, TabCompleter {

    private final FrostPearlsPlugin plugin;

    public FrostPearlsCommand(FrostPearlsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("frostsmp.pearls.admin")) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginConfig();
            sender.sendMessage(Component.text("FrostPearls config reloaded.", NamedTextColor.GREEN));
            return true;
        }
        sender.sendMessage(Component.text("Usage: /frostpearls reload", NamedTextColor.GRAY));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("frostsmp.pearls.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                options.add("reload");
            }
            return options;
        }
        return List.of();
    }
}
