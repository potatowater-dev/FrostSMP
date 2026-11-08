package com.frostsmp.rejoin;

import com.frostsmp.license.LicenseGate;
import com.frostsmp.rejoin.command.RejoinCommand;
import com.frostsmp.rejoin.config.PluginConfig;
import com.frostsmp.rejoin.listener.RejoinListener;
import com.frostsmp.rejoin.service.RejoinService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostRejoinPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private RejoinService rejoinService;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        rejoinService = new RejoinService(this, pluginConfig);
        // Do NOT start waiting on enable — wait until players actually arrive from transfer

        RejoinCommand command = new RejoinCommand(this, pluginConfig, rejoinService);
        PluginCommand frostrejoin = getCommand("frostrejoin");
        if (frostrejoin != null) {
            frostrejoin.setExecutor(command);
            frostrejoin.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'frostrejoin' missing from plugin.yml!");
        }

        getServer().getPluginManager().registerEvents(new RejoinListener(rejoinService), this);

        // Queue anyone already online (e.g. plugin reload mid-restart)
        for (Player player : Bukkit.getOnlinePlayers()) {
            rejoinService.onPlayerJoin(player);
        }

        getLogger().info("FrostRejoin enabled — returning players to "
                + pluginConfig.getReturnHost() + ":" + pluginConfig.getReturnPort());
    }

    @Override
    public void onDisable() {
        if (rejoinService != null) {
            rejoinService.stop();
        }
    }

    public void reloadAll() {
        pluginConfig.load();
        rejoinService.clearAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            rejoinService.onPlayerJoin(player);
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public RejoinService getRejoinService() {
        return rejoinService;
    }
}
