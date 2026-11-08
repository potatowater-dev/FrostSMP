package com.frostsmp.afk;

import com.frostsmp.afk.command.AfkCommand;
import com.frostsmp.afk.command.FrostAfkCommand;
import com.frostsmp.afk.config.PluginConfig;
import com.frostsmp.afk.listener.AfkListener;
import com.frostsmp.afk.listener.WandListener;
import com.frostsmp.afk.service.AfkService;
import com.frostsmp.afk.service.FrostCratesBridge;
import com.frostsmp.afk.service.WandService;
import com.frostsmp.afk.storage.RegionStorage;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostAfkPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private RegionStorage regionStorage;
    private AfkService afkService;
    private WandService wandService;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        regionStorage = new RegionStorage(this);
        regionStorage.load();

        FrostCratesBridge cratesBridge = new FrostCratesBridge(this);
        afkService = new AfkService(this, pluginConfig, regionStorage, cratesBridge);
        wandService = new WandService(this, pluginConfig);

        AfkCommand afkCommand = new AfkCommand(pluginConfig, afkService);
        PluginCommand afk = getCommand("afk");
        if (afk != null) {
            afk.setExecutor(afkCommand);
        } else {
            getLogger().severe("Command 'afk' missing from plugin.yml!");
        }

        FrostAfkCommand adminCommand = new FrostAfkCommand(this, pluginConfig, regionStorage, wandService);
        PluginCommand frostafk = getCommand("frostafk");
        if (frostafk != null) {
            frostafk.setExecutor(adminCommand);
            frostafk.setTabCompleter(adminCommand);
        } else {
            getLogger().severe("Command 'frostafk' missing from plugin.yml!");
        }

        getServer().getPluginManager().registerEvents(new AfkListener(afkService), this);
        getServer().getPluginManager().registerEvents(new WandListener(pluginConfig, regionStorage, wandService), this);

        afkService.start();
        getLogger().info("FrostAFK enabled.");
    }

    @Override
    public void onDisable() {
        if (afkService != null) {
            afkService.shutdown();
        }
    }

    public void reloadAll() {
        pluginConfig.load();
        regionStorage.load();
        afkService.reload();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public RegionStorage getRegionStorage() {
        return regionStorage;
    }

    public AfkService getAfkService() {
        return afkService;
    }
}
