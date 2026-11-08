package com.frostsmp.grapple;

import com.frostsmp.grapple.command.GrappleCommand;
import com.frostsmp.grapple.command.SafeCommandExecutor;
import com.frostsmp.grapple.config.GrappleConfig;
import com.frostsmp.grapple.listener.GrappleListener;
import com.frostsmp.grapple.service.GrappleService;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostGrapplePlugin extends JavaPlugin {

    private GrappleConfig grappleConfig;
    private GrappleService grappleService;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        grappleConfig = new GrappleConfig(this);
        grappleConfig.load();
        grappleService = new GrappleService(this, grappleConfig);

        getServer().getPluginManager().registerEvents(new GrappleListener(grappleConfig, grappleService), this);

        GrappleCommand command = new GrappleCommand(this, grappleConfig);
        PluginCommand grapple = getCommand("grapple");
        if (grapple != null) {
            grapple.setExecutor(new SafeCommandExecutor(this, command));
            grapple.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'grapple' missing from plugin.yml!");
        }

        getLogger().info("FrostGrapple enabled.");
    }

    @Override
    public void onDisable() {
        if (grappleService != null) {
            grappleService.clearAll();
        }
    }

    public void reloadAll() {
        grappleConfig.load();
    }
}
