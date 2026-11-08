package com.frostsmp.bounty;

import com.frostsmp.bounty.command.BountyCommand;
import com.frostsmp.bounty.config.PluginConfig;
import com.frostsmp.bounty.listener.BountyGuiListener;
import com.frostsmp.bounty.listener.BountyKillListener;
import com.frostsmp.bounty.service.BountyService;
import com.frostsmp.bounty.storage.BountyStorage;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostBountyPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private BountyStorage bountyStorage;
    private BountyService bountyService;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        bountyStorage = new BountyStorage(this);
        bountyStorage.load();

        bountyService = new BountyService(this, pluginConfig, bountyStorage);

        BountyCommand command = new BountyCommand(this, pluginConfig, bountyService);
        PluginCommand bounty = getCommand("bounty");
        if (bounty != null) {
            bounty.setExecutor(command);
            bounty.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'bounty' missing from plugin.yml!");
        }

        getServer().getPluginManager().registerEvents(new BountyGuiListener(bountyService), this);
        getServer().getPluginManager().registerEvents(new BountyKillListener(bountyService), this);

        getLogger().info("FrostBounty enabled.");
    }

    @Override
    public void onDisable() {
        if (bountyStorage != null) {
            bountyStorage.save();
        }
    }

    public void reloadAll() {
        pluginConfig.load();
        bountyStorage.load();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public BountyService getBountyService() {
        return bountyService;
    }
}
