package com.frost.plots.manager;

import com.frost.plots.config.PluginConfig;
import com.frost.plots.hook.WorldGuardHook;
import com.frost.plots.model.Plot;
import com.frost.plots.model.PlotPrivacy;
import com.frost.plots.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PrivacyManager {

    private final PluginConfig config;
    private final PlotManager plots;
    private final WorldGuardHook worldGuard;

    public PrivacyManager(PluginConfig config, PlotManager plots, WorldGuardHook worldGuard) {
        this.config = config;
        this.plots = plots;
        this.worldGuard = worldGuard;
    }

    public void setPublic(Player player) {
        Plot plot = plots.requireOwnerForManagement(player);
        if (plot == null) {
            return;
        }
        plot.setPrivacy(PlotPrivacy.PUBLIC);
        worldGuard.applyPrivacy(plot);
        plots.persist(plot);
        MessageUtil.send(player, config.prefixed("privacy-public"));
    }

    public void setPrivate(Player player) {
        Plot plot = plots.requireOwnerForManagement(player);
        if (plot == null) {
            return;
        }
        plot.setPrivacy(PlotPrivacy.PRIVATE);
        worldGuard.applyPrivacy(plot);
        plots.persist(plot);
        ejectNonMembers(plot);
        MessageUtil.send(player, config.prefixed("privacy-private"));
    }

    public void ejectNonMembers(Plot plot) {
        int size = plots.sizeOf(plot);
        Location eject = plots.ejectLocation(plot.getCenterLocation() == null ? null : plot.getCenterLocation().getWorld());
        if (eject == null) {
            return;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!plot.isPlayerInside(online, size)) {
                continue;
            }
            if (plot.isMemberOrOwner(online.getUniqueId()) || online.hasPermission("frostplots.admin")) {
                continue;
            }
            online.teleport(eject);
        }
    }
}
