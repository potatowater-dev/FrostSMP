package com.frostsmp.speed;

import com.frostsmp.license.LicenseGate;
import com.frostsmp.speed.command.SafeCommandExecutor;
import com.frostsmp.speed.command.SpeedCommand;
import com.frostsmp.speed.config.SpeedPotionConfig;
import com.frostsmp.speed.listener.SpeedPotionListener;
import com.frostsmp.speed.service.SpeedPotionService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostSpeedPlugin extends JavaPlugin {

    private SpeedPotionConfig speedPotionConfig;
    private SpeedPotionService speedPotionService;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        speedPotionConfig = new SpeedPotionConfig(this);
        speedPotionConfig.load();

        speedPotionService = new SpeedPotionService(this, speedPotionConfig);

        getServer().getPluginManager().registerEvents(new SpeedPotionListener(speedPotionService), this);

        SpeedCommand command = new SpeedCommand(this, speedPotionConfig);
        PluginCommand frostspeed = getCommand("frostspeed");
        if (frostspeed != null) {
            frostspeed.setExecutor(new SafeCommandExecutor(this, command));
            frostspeed.setTabCompleter(command);
        } else {
            getLogger().severe("Command 'frostspeed' missing from plugin.yml!");
        }

        getLogger().info("FrostSpeed enabled.");
    }

    @Override
    public void onDisable() {
        if (speedPotionService != null) {
            speedPotionService.clearAll();
        }
    }

    public void reloadAll() {
        speedPotionConfig.load();
    }
}
