package com.frost.plots.placeholder;

import com.frost.plots.FrostPlotsPlugin;
import com.frost.plots.config.PluginConfig;
import com.frost.plots.manager.PlotManager;
import com.frost.plots.model.Plot;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlotPlaceholders extends PlaceholderExpansion {

    private final FrostPlotsPlugin plugin;
    private final PluginConfig config;
    private final PlotManager plots;

    public PlotPlaceholders(FrostPlotsPlugin plugin, PluginConfig config, PlotManager plots) {
        this.plugin = plugin;
        this.config = config;
        this.plots = plots;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "frostplots";
    }

    @Override
    public @NotNull String getAuthor() {
        return "FrostSMP";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }
        Player player = offlinePlayer.getPlayer();
        Plot plot = player != null ? plots.at(player.getLocation()) : null;
        if (plot == null) {
            plot = plots.byOwner(offlinePlayer.getUniqueId());
        }
        if (plot == null) {
            return "";
        }
        return switch (params.toLowerCase()) {
            case "size" -> {
                int size = config.tier(plot.getTierIndex()).size();
                yield size + "x" + size;
            }
            case "owner" -> {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(plot.getOwner());
                String name = owner.getName();
                yield name == null ? plot.getOwner().toString() : name;
            }
            case "privacy" -> plot.getPrivacy().name();
            case "tier" -> String.valueOf(plot.getTierIndex() + 1);
            default -> null;
        };
    }
}
