package com.frostsmp.ffa;

import com.frostsmp.ffa.arena.ArenaService;
import com.frostsmp.ffa.arena.MembershipCache;
import com.frostsmp.ffa.arena.MembershipListener;
import com.frostsmp.ffa.command.FfaCommand;
import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.kit.KitListener;
import com.frostsmp.ffa.kit.KitService;
import com.frostsmp.ffa.kill.KillCreditListener;
import com.frostsmp.ffa.mutator.MutatorManager;
import com.frostsmp.ffa.placeholder.FfaExpansion;
import com.frostsmp.ffa.schedule.ScheduleService;
import com.frostsmp.ffa.storage.FfaStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostFfaPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private ArenaService arenaService;
    private MutatorManager mutatorManager;
    private ScheduleService scheduleService;

    @Override
    public void onEnable() {
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        FfaStorage storage = new FfaStorage(this);
        storage.load();

        arenaService = new ArenaService(this);
        MembershipCache membership = new MembershipCache(arenaService);
        KitService kits = new KitService(pluginConfig, arenaService);
        mutatorManager = new MutatorManager(this, pluginConfig, arenaService, membership, storage);
        scheduleService = new ScheduleService(this, pluginConfig, mutatorManager, storage);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new MembershipListener(membership), this);
        pm.registerEvents(new KitListener(this, pluginConfig, kits), this);
        pm.registerEvents(new KillCreditListener(pluginConfig, membership), this);

        membership.scanOnline();
        scheduleService.start();

        if (pm.getPlugin("PlaceholderAPI") != null) {
            new FfaExpansion(this, mutatorManager, scheduleService).register();
        }

        FfaCommand command = new FfaCommand(pluginConfig, kits, mutatorManager, scheduleService, this::reloadAll);
        PluginCommand ffa = getCommand("ffa");
        if (ffa != null) {
            ffa.setExecutor(command);
            ffa.setTabCompleter(command);
        }

        if (arenaService.region() == null) {
            getLogger().warning("WorldGuard region '" + arenaService.regionId()
                    + "' not found in world '" + arenaService.worldName()
                    + "'. Set arena.world / arena.region in config.yml.");
        }
        getLogger().info("FrostFFA enabled (MIT).");
    }

    @Override
    public void onDisable() {
        if (scheduleService != null) {
            scheduleService.shutdown();
        }
        if (mutatorManager != null) {
            mutatorManager.shutdown();
        }
    }

    private void reloadAll() {
        if (mutatorManager != null && mutatorManager.isActive()) {
            mutatorManager.stop();
        }
        pluginConfig.load();
        arenaService.reload();
        scheduleService.reload();
    }
}
