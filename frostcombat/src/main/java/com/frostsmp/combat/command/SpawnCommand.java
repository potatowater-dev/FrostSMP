package com.frostsmp.combat.command;

import com.frostsmp.combat.service.SpawnService;
import com.frostsmp.combat.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpawnCommand implements CommandExecutor {

    private final SpawnService spawnService;

    public SpawnCommand(SpawnService spawnService) {
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /spawn.");
            return true;
        }
        if (!player.hasPermission("frostsmp.spawn.use")) {
            MessageUtil.send(player, "<red>You do not have permission.</red>");
            return true;
        }
        spawnService.startTeleport(player);
        return true;
    }
}
