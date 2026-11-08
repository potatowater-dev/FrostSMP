package com.frostsmp.colosseum;

import com.frostsmp.colosseum.command.ColoCommand;
import com.frostsmp.colosseum.config.PluginConfig;
import com.frostsmp.colosseum.gui.AdminGui;
import com.frostsmp.colosseum.gui.RewardGui;
import com.frostsmp.colosseum.listener.MatchListener;
import com.frostsmp.colosseum.listener.WandListener;
import com.frostsmp.colosseum.service.MatchScheduler;
import com.frostsmp.colosseum.service.MatchService;
import com.frostsmp.colosseum.service.QueueService;
import com.frostsmp.colosseum.service.RewardService;
import com.frostsmp.colosseum.service.TeamGlowService;
import com.frostsmp.colosseum.service.WandService;
import com.frostsmp.colosseum.storage.RegionStorage;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostColosseumPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private RegionStorage regionStorage;
    private RewardService rewardService;
    private WandService wandService;
    private QueueService queueService;
    private MatchService matchService;
    private MatchScheduler matchScheduler;
    private TeamGlowService glowService;
    private RewardGui rewardGui;
    private AdminGui adminGui;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.pluginConfig = new PluginConfig(this);
        this.pluginConfig.load();

        this.regionStorage = new RegionStorage(this);
        this.regionStorage.load();

        this.rewardService = new RewardService(this);
        this.rewardService.load();

        this.wandService = new WandService(this, pluginConfig);
        this.glowService = new TeamGlowService();
        this.queueService = new QueueService(pluginConfig, regionStorage);
        this.matchService = new MatchService(
                this, pluginConfig, regionStorage, queueService, glowService, rewardService
        );
        this.queueService.setMatchService(matchService);

        this.matchScheduler = new MatchScheduler(this, pluginConfig, matchService, queueService);
        this.matchService.setScheduler(matchScheduler);

        this.rewardGui = new RewardGui(pluginConfig, rewardService);
        this.adminGui = new AdminGui(
                pluginConfig, regionStorage, rewardService, matchService,
                matchScheduler, queueService, wandService, rewardGui
        );

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new WandListener(pluginConfig, regionStorage, wandService), this);
        pm.registerEvents(new MatchListener(pluginConfig, regionStorage, queueService, matchService), this);
        pm.registerEvents(rewardGui, this);
        pm.registerEvents(adminGui, this);

        ColoCommand coloCommand = new ColoCommand(
                this, pluginConfig, regionStorage, wandService, matchService,
                matchScheduler, queueService, adminGui, rewardGui
        );
        PluginCommand colo = getCommand("colo");
        if (colo != null) {
            colo.setExecutor(coloCommand);
            colo.setTabCompleter(coloCommand);
        } else {
            getLogger().severe("Command 'colo' is missing from plugin.yml!");
        }
        PluginCommand setReward = getCommand("setreward");
        if (setReward != null) {
            setReward.setExecutor(coloCommand);
            setReward.setTabCompleter(coloCommand);
        }

        matchScheduler.start();
        getLogger().info("FrostColosseum enabled.");
    }

    @Override
    public void onDisable() {
        if (matchScheduler != null) {
            matchScheduler.stop();
        }
        if (matchService != null) {
            matchService.shutdown();
        }
    }

    public void reloadPlugin() {
        pluginConfig.load();
        regionStorage.load();
        rewardService.load();
        if (matchScheduler != null) {
            matchScheduler.resetInterval();
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public RegionStorage getRegionStorage() {
        return regionStorage;
    }

    public MatchService getMatchService() {
        return matchService;
    }

    public QueueService getQueueService() {
        return queueService;
    }

    public MatchScheduler getMatchScheduler() {
        return matchScheduler;
    }
}
