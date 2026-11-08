package com.frostsmp.alt;

import com.frostsmp.alt.command.AppealCommand;
import com.frostsmp.alt.command.FrostAltCommand;
import com.frostsmp.alt.config.PluginConfig;
import com.frostsmp.alt.listener.AltJoinListener;
import com.frostsmp.alt.listener.AltLoginListener;
import com.frostsmp.alt.listener.AltQuitListener;
import com.frostsmp.alt.listener.AppealRestrictionListener;
import com.frostsmp.alt.service.AltDetectionService;
import com.frostsmp.alt.service.AppealService;
import com.frostsmp.alt.service.EnforcementService;
import com.frostsmp.alt.service.OnlineIpRegistry;
import com.frostsmp.alt.service.PlaytimeService;
import com.frostsmp.alt.storage.CaseStorage;
import com.frostsmp.alt.storage.IpHistoryStorage;
import com.frostsmp.alt.storage.PlayerStateStorage;
import com.frostsmp.license.LicenseGate;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostAltPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private PlayerStateStorage playerStateStorage;
    private CaseStorage caseStorage;
    private IpHistoryStorage ipHistoryStorage;
    private OnlineIpRegistry ipRegistry;
    private PlaytimeService playtimeService;
    private EnforcementService enforcementService;
    private AltDetectionService detectionService;
    private AppealService appealService;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        playerStateStorage = new PlayerStateStorage(this);
        caseStorage = new CaseStorage(this);
        ipHistoryStorage = new IpHistoryStorage(this);
        playerStateStorage.load();
        caseStorage.load();
        ipHistoryStorage.load();

        ipRegistry = new OnlineIpRegistry();
        playtimeService = new PlaytimeService(playerStateStorage);
        enforcementService = new EnforcementService(this, pluginConfig, caseStorage, playerStateStorage, playtimeService);
        detectionService = new AltDetectionService(
                pluginConfig, ipRegistry, playtimeService, playerStateStorage, enforcementService
        );
        appealService = new AppealService(pluginConfig, caseStorage, playerStateStorage);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new AltLoginListener(pluginConfig, playerStateStorage), this);
        pm.registerEvents(new AltJoinListener(
                this, ipRegistry, ipHistoryStorage, playtimeService,
                playerStateStorage, enforcementService, detectionService
        ), this);
        pm.registerEvents(new AltQuitListener(ipRegistry, playtimeService), this);
        pm.registerEvents(new AppealRestrictionListener(pluginConfig, enforcementService), this);

        AppealCommand appealCommand = new AppealCommand(appealService);
        PluginCommand appeal = getCommand("appeal");
        if (appeal != null) {
            appeal.setExecutor(appealCommand);
            appeal.setTabCompleter(appealCommand);
        } else {
            getLogger().severe("Command 'appeal' missing from plugin.yml!");
        }

        FrostAltCommand adminCommand = new FrostAltCommand(this, caseStorage, enforcementService);
        PluginCommand frostalt = getCommand("frostalt");
        if (frostalt != null) {
            frostalt.setExecutor(adminCommand);
            frostalt.setTabCompleter(adminCommand);
        } else {
            getLogger().severe("Command 'frostalt' missing from plugin.yml!");
        }

        getLogger().info("FrostAlt enabled.");
    }

    @Override
    public void onDisable() {
        if (playerStateStorage != null) {
            playerStateStorage.save();
        }
        if (caseStorage != null) {
            caseStorage.save();
        }
        if (ipHistoryStorage != null) {
            ipHistoryStorage.save();
        }
        getLogger().info("FrostAlt disabled.");
    }

    public void reloadPluginConfig() {
        pluginConfig.load();
    }
}
