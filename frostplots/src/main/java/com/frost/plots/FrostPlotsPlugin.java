package com.frost.plots;

import com.frost.plots.command.PlotCommand;
import com.frost.plots.command.SafeCommandExecutor;
import com.frost.plots.config.PluginConfig;
import com.frost.plots.gui.UpgradeGUI;
import com.frost.plots.hook.ItemCostHook;
import com.frost.plots.hook.SchematicHook;
import com.frost.plots.hook.WorldGuardHook;
import com.frost.plots.listener.PlotBoundaryListener;
import com.frost.plots.listener.PlotProtectionListener;
import com.frost.plots.manager.MemberManager;
import com.frost.plots.manager.PlotManager;
import com.frost.plots.manager.PrivacyManager;
import com.frost.plots.placeholder.PlotPlaceholders;
import com.frost.plots.storage.PlotDAO;
import com.frost.plots.util.SchedulerUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostPlotsPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private PlotDAO plotDao;
    private PlotManager plotManager;

    @Override
    public void onEnable() {
        pluginConfig = new PluginConfig(this);
        pluginConfig.load();

        SchedulerUtil scheduler = new SchedulerUtil(this);
        plotDao = new PlotDAO(this, scheduler);
        plotDao.open();

        ItemCostHook itemCosts = new ItemCostHook(this);

        WorldGuardHook worldGuard = new WorldGuardHook(this, pluginConfig);
        SchematicHook schematicsHook = new SchematicHook(this);
        plotManager = new PlotManager(this, pluginConfig, plotDao, worldGuard, schematicsHook);
        plotManager.load(plotDao.loadAllBlocking());

        PrivacyManager privacyManager = new PrivacyManager(pluginConfig, plotManager, worldGuard);
        MemberManager memberManager = new MemberManager(pluginConfig, plotManager, worldGuard);
        UpgradeGUI upgradeGui = new UpgradeGUI(pluginConfig, plotManager, itemCosts);

        PlotCommand plotCommand = new PlotCommand(pluginConfig, plotManager, privacyManager, memberManager, upgradeGui);
        PluginCommand command = getCommand("plot");
        if (command == null) {
            getLogger().severe("plugin.yml is missing the /plot command.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(new SafeCommandExecutor(this, plotCommand));
        command.setTabCompleter(plotCommand);

        PlotBoundaryListener boundary = new PlotBoundaryListener(pluginConfig, plotManager);
        getServer().getPluginManager().registerEvents(new PlotProtectionListener(pluginConfig, plotManager, worldGuard), this);
        getServer().getPluginManager().registerEvents(boundary, this);
        getServer().getPluginManager().registerEvents(upgradeGui, this);
        boundary.start(this);

        for (var plot : plotManager.all()) {
            worldGuard.createOrUpdateRegion(plot, plotManager.sizeOf(plot));
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlotPlaceholders(this, pluginConfig, plotManager).register();
        } else {
            getLogger().info("PlaceholderAPI not found; %frostplots_*% placeholders disabled.");
        }

        warnSpacing();
    }

    @Override
    public void onDisable() {
        if (plotDao != null) {
            plotDao.close();
        }
    }

    public PluginConfig config() {
        return pluginConfig;
    }

    public PlotManager plots() {
        return plotManager;
    }

    private void warnSpacing() {
        int maxSize = pluginConfig.upgradeTiers().stream()
                .mapToInt(tier -> tier.size())
                .max()
                .orElse(0);
        if (pluginConfig.gridSpacing() <= maxSize) {
            getLogger().warning("grid-spacing (" + pluginConfig.gridSpacing()
                    + ") is not greater than the largest upgrade size (" + maxSize
                    + "). Plots may overlap.");
        }
    }
}
