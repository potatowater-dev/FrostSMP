package com.frostsmp.restart;

import com.frostsmp.restart.command.RestartCommand;
import com.frostsmp.restart.config.PluginConfig;
import com.frostsmp.restart.service.FrostCombatBridge;
import com.frostsmp.restart.service.RestartScheduler;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostRestartPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private RestartScheduler restartScheduler;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        FrostCombatBridge combatBridge = new FrostCombatBridge(this);
        restartScheduler = new RestartScheduler(this, pluginConfig, combatBridge);

        RestartCommand commandHandler = new RestartCommand(this);
        PluginCommand command = getCommand("frostrestart");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        } else {
            getLogger().severe("Command 'frostrestart' missing from plugin.yml!");
        }

        getLogger().info("FrostRestart enabled.");
    }

    @Override
    public void onDisable() {
        if (restartScheduler != null) {
            restartScheduler.shutdown();
        }
    }

    public void reloadPluginConfig() {
        pluginConfig.load();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public RestartScheduler getRestartScheduler() {
        return restartScheduler;
    }
}
