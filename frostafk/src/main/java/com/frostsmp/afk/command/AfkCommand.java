package com.frostsmp.afk.command;

import com.frostsmp.afk.config.PluginConfig;
import com.frostsmp.afk.service.AfkService;
import com.frostsmp.afk.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AfkCommand implements CommandExecutor {

    private static final String PERM = "frostsmp.afk.use";

    private final PluginConfig config;
    private final AfkService afkService;

    public AfkCommand(PluginConfig config, AfkService afkService) {
        this.config = config;
        this.afkService = afkService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, config.getPlayersOnly());
            return true;
        }
        if (!player.hasPermission(PERM)) {
            MessageUtil.send(player, config.getNoPermission());
            return true;
        }
        afkService.startTeleport(player);
        return true;
    }
}
