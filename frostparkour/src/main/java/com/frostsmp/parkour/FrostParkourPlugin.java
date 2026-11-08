package com.frostsmp.parkour;

import com.frostsmp.license.LicenseGate;
import com.frostsmp.parkour.command.ParkourCommand;
import com.frostsmp.parkour.config.PluginConfig;
import com.frostsmp.parkour.gui.ParkourGuiService;
import com.frostsmp.parkour.listener.ParkourControlListener;
import com.frostsmp.parkour.listener.ParkourPlayerListener;
import com.frostsmp.parkour.listener.ParkourTriggerListener;
import com.frostsmp.parkour.placeholder.ParkourExpansion;
import com.frostsmp.parkour.service.CourseService;
import com.frostsmp.parkour.service.LeaderboardService;
import com.frostsmp.parkour.service.ParkourSessionService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FrostParkourPlugin extends JavaPlugin {

    private PluginConfig pluginConfig;
    private CourseService courseService;
    private LeaderboardService leaderboardService;
    private ParkourSessionService sessionService;
    private ParkourGuiService guiService;
    private ParkourExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        if (!LicenseGate.activate(this)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.pluginConfig = new PluginConfig(this);
        this.pluginConfig.load();

        this.courseService = new CourseService(this);
        this.courseService.load();

        this.leaderboardService = new LeaderboardService(this);
        this.leaderboardService.load();

        this.sessionService = new ParkourSessionService(this, pluginConfig, courseService, leaderboardService);
        this.guiService = new ParkourGuiService(this, courseService, leaderboardService);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ParkourTriggerListener(courseService, sessionService), this);
        pm.registerEvents(new ParkourControlListener(sessionService, guiService, courseService), this);
        pm.registerEvents(new ParkourPlayerListener(pluginConfig, courseService, sessionService), this);

        ParkourCommand cmd = new ParkourCommand(this, pluginConfig, courseService, leaderboardService, sessionService, guiService);
        PluginCommand parkourCmd = getCommand("parkour");
        if (parkourCmd != null) {
            parkourCmd.setExecutor(cmd);
            parkourCmd.setTabCompleter(cmd);
        } else {
            getLogger().severe("Command 'parkour' is missing from plugin.yml!");
        }

        if (pm.getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new ParkourExpansion(this, courseService, leaderboardService, sessionService);
            placeholderExpansion.register();
            getLogger().info("Registered FrostParkour PlaceholderAPI expansion.");
        }

        getLogger().info("FrostParkour enabled.");
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            try {
                placeholderExpansion.unregister();
            } catch (Exception ignored) {
            }
        }
        if (sessionService != null) {
            sessionService.shutdown();
        }
        if (courseService != null) {
            courseService.save();
        }
        if (leaderboardService != null) {
            leaderboardService.save();
        }
    }

    public void reloadPlugin() {
        pluginConfig.load();
        courseService.load();
        leaderboardService.load();
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public CourseService getCourseService() {
        return courseService;
    }

    public LeaderboardService getLeaderboardService() {
        return leaderboardService;
    }

    public ParkourSessionService getSessionService() {
        return sessionService;
    }

    public ParkourGuiService getGuiService() {
        return guiService;
    }
}
