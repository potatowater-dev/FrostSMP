package com.frostsmp.alt.command;

import com.frostsmp.alt.service.AppealService;
import com.frostsmp.alt.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class AppealCommand implements CommandExecutor, TabCompleter {

    private final AppealService appealService;

    public AppealCommand(AppealService appealService) {
        this.appealService = appealService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "<red>Only players can submit appeals.</red>");
            return true;
        }
        if (args.length == 0) {
            MessageUtil.send(player, "<yellow>Usage: /appeal <reason></yellow>");
            return true;
        }
        String reason = String.join(" ", args).trim();
        if (reason.isEmpty()) {
            MessageUtil.send(player, "<yellow>Usage: /appeal <reason></yellow>");
            return true;
        }
        appealService.submitAppeal(player, reason);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
