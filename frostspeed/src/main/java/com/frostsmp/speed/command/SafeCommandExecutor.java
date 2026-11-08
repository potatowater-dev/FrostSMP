package com.frostsmp.speed.command;

import com.frostsmp.speed.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class SafeCommandExecutor implements CommandExecutor {

    private final JavaPlugin plugin;
    private final CommandExecutor delegate;

    public SafeCommandExecutor(JavaPlugin plugin, CommandExecutor delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            return delegate.onCommand(sender, command, label, args);
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Command /" + label + " failed", throwable);
            MessageUtil.send(sender, "<red>Command failed. Check console for details.</red>");
            return true;
        }
    }
}
