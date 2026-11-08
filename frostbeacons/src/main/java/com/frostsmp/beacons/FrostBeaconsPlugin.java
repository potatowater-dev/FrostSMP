package com.frostsmp.beacons;

import com.frostsmp.beacons.command.FrostBeaconsCommand;
import com.frostsmp.beacons.config.PluginConfig;
import com.frostsmp.beacons.listener.BeaconListener;
import com.frostsmp.beacons.service.BeaconBeamService;
import com.frostsmp.beacons.service.BeaconEffectService;
import com.frostsmp.beacons.service.BeaconRegistry;
import com.frostsmp.beacons.service.PyramidService;
import com.frostsmp.beacons.service.WandService;
import com.frostsmp.beacons.storage.BeaconEffectStorage;
import com.frostsmp.beacons.storage.BeamLinkStorage;
import com.frostsmp.beacons.util.MessageUtil;
import com.frostsmp.license.LicenseGate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class FrostBeaconsPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private BeamLinkStorage beamLinks;
    private BeaconEffectStorage effectStorage;
    private BeaconRegistry beaconRegistry;
    private BeaconEffectService effectService;
    private BukkitTask tickTask;

    @Override
    public void onEnable() {
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        if (!LicenseGate.activate(this)) {
            getLogger().severe("Invalid or missing license.key in config.yml. Set it to your Frost license password and restart.");
            PluginCommand command = getCommand("frostbeacons");
            if (command != null) {
                command.setExecutor((sender, cmd, label, args) -> {
                    MessageUtil.send(sender,
                            "<red>FrostBeacons license invalid.</red> <gray>Set</gray> <white>license.key</white> <gray>in</gray> <white>plugins/FrostBeacons/config.yml</white> <gray>and restart.</gray>");
                    return true;
                });
            }
            return;
        }

        pluginConfig.load();

        beamLinks = new BeamLinkStorage(this);
        beamLinks.load();
        effectStorage = new BeaconEffectStorage(this);
        effectStorage.load();

        beaconRegistry = new BeaconRegistry();
        WandService wandService = new WandService(this, pluginConfig);
        BeaconBeamService beamService = new BeaconBeamService();
        effectService = new BeaconEffectService(
                pluginConfig,
                new PyramidService(),
                beaconRegistry,
                beamLinks,
                effectStorage,
                beamService
        );

        getServer().getPluginManager().registerEvents(
                new BeaconListener(this, pluginConfig, beaconRegistry, wandService, beamLinks, effectStorage),
                this
        );

        FrostBeaconsCommand commandHandler = new FrostBeaconsCommand(this, pluginConfig, wandService, beamLinks);
        PluginCommand command = getCommand("frostbeacons");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        } else {
            getLogger().severe("Command 'frostbeacons' missing from plugin.yml!");
        }

        for (World world : Bukkit.getWorlds()) {
            for (var chunk : world.getLoadedChunks()) {
                beaconRegistry.scanChunk(chunk);
            }
        }

        startTickTask();
        getLogger().info("FrostBeacons enabled.");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (beamLinks != null) {
            beamLinks.save();
        }
        if (effectStorage != null) {
            effectStorage.save();
        }
    }

    public void reloadAll() {
        pluginConfig.load();
        beamLinks.load();
        effectStorage.load();
        startTickTask();
    }

    private void startTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        long interval = pluginConfig.getTickIntervalTicks();
        tickTask = getServer().getScheduler().runTaskTimer(this, () -> effectService.tick(), interval, interval);
    }
}
