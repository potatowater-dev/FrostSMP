package com.frostsmp.juggernaut;

import com.frostsmp.juggernaut.command.JuggernautCommand;
import com.frostsmp.juggernaut.config.PluginConfig;
import com.frostsmp.juggernaut.listener.JuggernautListener;
import com.frostsmp.juggernaut.listener.ManageGuiListener;
import com.frostsmp.juggernaut.service.BossBarService;
import com.frostsmp.juggernaut.service.DamageTracker;
import com.frostsmp.juggernaut.service.EventService;
import com.frostsmp.juggernaut.storage.JuggernautStorage;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostJuggernautPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private EventService eventService;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        JuggernautStorage storage = new JuggernautStorage(this);
        storage.load();

        DamageTracker damageTracker = new DamageTracker();
        BossBarService bossBarService = new BossBarService(this, pluginConfig, damageTracker);
        eventService = new EventService(this, pluginConfig, damageTracker, bossBarService, storage);
        bossBarService.attach(eventService);
        eventService.restorePvpFromDisk();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new ManageGuiListener(), this);
        pm.registerEvents(new JuggernautListener(eventService, pluginConfig, bossBarService), this);

        JuggernautCommand commandHandler = new JuggernautCommand(this, pluginConfig, eventService);
        PluginCommand command = getCommand("juggernaut");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        } else {
            getLogger().severe("Command 'juggernaut' missing from plugin.yml!");
        }

        getLogger().info("FrostJuggernaut enabled.");
    }

    @Override
    public void onDisable() {
        if (eventService != null) {
            eventService.shutdown();
        }
    }

    public void reloadPluginConfig() {
        pluginConfig.load();
    }

    public EventService getEventService() {
        return eventService;
    }
}
