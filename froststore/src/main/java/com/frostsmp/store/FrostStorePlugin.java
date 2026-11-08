package com.frostsmp.store;

import com.frostsmp.store.command.StoreCommand;
import com.frostsmp.store.service.PurchasePoller;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostStorePlugin extends JavaPlugin {

    private PurchasePoller poller;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        poller = new PurchasePoller(this);
        poller.start();

        StoreCommand command = new StoreCommand(this);
        PluginCommand froststore = getCommand("froststore");
        if (froststore != null) {
            froststore.setExecutor(command);
            froststore.setTabCompleter(command);
        }

        getLogger().info("FrostStore enabled — polling for rank purchases.");
    }

    @Override
    public void onDisable() {
        if (poller != null) {
            poller.stop();
        }
    }

    public void reloadAll() {
        reloadConfig();
        if (poller != null) {
            poller.stop();
        }
        poller = new PurchasePoller(this);
        poller.start();
    }
}
