package com.frostsmp.event;

import com.frostsmp.event.command.EventCommand;
import com.frostsmp.event.config.PluginConfig;
import com.frostsmp.event.listener.EventListener;
import com.frostsmp.event.service.EventService;
import com.frostsmp.event.service.LocatorForceService;
import com.frostsmp.event.service.SpawnProtectionService;
import com.frostsmp.event.storage.WinnerStorage;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostEventPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private WinnerStorage winnerStorage;
    private LocatorForceService locatorForceService;
    private EventService eventService;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        winnerStorage = new WinnerStorage(this);
        winnerStorage.load();

        locatorForceService = new LocatorForceService(this, pluginConfig);
        SpawnProtectionService spawnProtectionService = new SpawnProtectionService(pluginConfig);
        eventService = new EventService(this, pluginConfig, winnerStorage, locatorForceService, spawnProtectionService);

        EventCommand command = new EventCommand(this, pluginConfig, eventService, winnerStorage);
        PluginCommand frostevent = getCommand("frostevent");
        if (frostevent != null) {
            frostevent.setExecutor(command);
            frostevent.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'frostevent' missing from plugin.yml!");
        }

        getServer().getPluginManager().registerEvents(new EventListener(this, pluginConfig, eventService), this);
        getLogger().info("FrostEvent enabled.");
    }

    @Override
    public void onDisable() {
        if (eventService != null) {
            eventService.shutdown();
        }
    }

    public void reloadAll() {
        pluginConfig.load();
        winnerStorage.load();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public EventService getEventService() {
        return eventService;
    }
}
