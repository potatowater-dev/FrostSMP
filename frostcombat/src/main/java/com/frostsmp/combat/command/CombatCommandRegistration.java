package com.frostsmp.combat.command;

import com.frostsmp.combat.FrostCombatPlugin;
import com.frostsmp.combat.service.CombatService;
import com.frostsmp.combat.service.SpawnService;
import com.frostsmp.combat.service.PlayerStatsService;
import com.frostsmp.combat.util.MessageUtil;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

public final class CombatCommandRegistration {

    private CombatCommandRegistration() {
    }

    public static void register(
            FrostCombatPlugin plugin,
            CombatService combatService,
            SpawnService spawnService,
            PlayerStatsService playerStatsService,
            boolean licensed
    ) {
        CommandExecutor licenseFailure = (sender, command, label, args) -> {
            MessageUtil.send(sender,
                    "<red>FrostCombat license invalid. Set license.key in config.yml and restart.</red>");
            return true;
        };

        if (!licensed) {
            bind(plugin, "frostcombat", licenseFailure, null);
            bind(plugin, "spawn", licenseFailure, null);
            return;
        }

        FrostCombatCommand frostCombatCommand = new FrostCombatCommand(plugin, combatService, playerStatsService);
        bind(plugin, "frostcombat",
                new SafeCommandExecutor(plugin, frostCombatCommand),
                frostCombatCommand);
        bind(plugin, "spawn",
                new SafeCommandExecutor(plugin, new SpawnCommand(spawnService)),
                null);
    }

    private static void bind(
            FrostCombatPlugin plugin,
            String name,
            CommandExecutor executor,
            TabCompleter tabCompleter
    ) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().severe("Command '" + name + "' missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }
    }
}
