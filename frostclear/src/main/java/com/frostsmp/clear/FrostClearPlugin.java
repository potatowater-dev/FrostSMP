package com.frostsmp.clear;

import com.frostsmp.clear.command.ClearCommand;
import com.frostsmp.clear.command.SafeCommandExecutor;
import com.frostsmp.clear.config.PluginConfig;
import com.frostsmp.clear.service.ClearScheduler;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostClearPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private ClearScheduler clearScheduler;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        clearScheduler = new ClearScheduler(this, pluginConfig);
        clearScheduler.start();

        ClearCommand command = new ClearCommand(this);
        PluginCommand frostclear = getCommand("frostclear");
        if (frostclear != null) {
            frostclear.setExecutor(new SafeCommandExecutor(this, command));
            frostclear.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'frostclear' missing from plugin.yml!");
        }

        getLogger().info("FrostClear enabled.");
    }

    @Override
    public void onDisable() {
        if (clearScheduler != null) {
            clearScheduler.stop();
        }
    }

    public void reloadAll() {
        pluginConfig.load();
        clearScheduler.start();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
}
