package com.frostsmp.pearls;

import com.frostsmp.license.LicenseGate;
import com.frostsmp.pearls.command.FrostPearlsCommand;
import com.frostsmp.pearls.config.PluginConfig;
import com.frostsmp.pearls.cooldown.PearlCooldownManager;
import com.frostsmp.pearls.listener.PearlCooldownListener;
import com.frostsmp.pearls.listener.PearlThrowListener;
import com.frostsmp.pearls.listener.WindChargeBoostListener;
import com.frostsmp.pearls.physics.PearlVelocityCalculator;
import com.frostsmp.pearls.tracker.PearlCatchTracker;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class FrostPearlsPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private PearlCatchTracker pearlCatchTracker;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        PearlVelocityCalculator calculator = new PearlVelocityCalculator(pluginConfig);
        pearlCatchTracker = new PearlCatchTracker();
        PearlCooldownManager pearlCooldownManager = new PearlCooldownManager();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PearlCooldownListener(pluginConfig, pearlCooldownManager), this);
        pm.registerEvents(new PearlThrowListener(pluginConfig, calculator, pearlCatchTracker), this);
        pm.registerEvents(new WindChargeBoostListener(pluginConfig, pearlCatchTracker), this);

        FrostPearlsCommand commandHandler = new FrostPearlsCommand(this);
        PluginCommand command = getCommand("frostpearls");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        } else {
            getLogger().severe("Command 'frostpearls' missing from plugin.yml!");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                pearlCatchTracker.tick();
            }
        }.runTaskTimer(this, 1L, 1L);

        getLogger().info("FrostPearls enabled.");
    }

    public void reloadPluginConfig() {
        pluginConfig.load();
    }
}
