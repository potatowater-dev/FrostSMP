package com.frost.plots.manager;

import com.frost.plots.config.PluginConfig;
import com.frost.plots.hook.WorldGuardHook;
import com.frost.plots.model.Plot;
import com.frost.plots.model.PlotRole;
import com.frost.plots.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class MemberManager {

    private final PluginConfig config;
    private final PlotManager plots;
    private final WorldGuardHook worldGuard;

    public MemberManager(PluginConfig config, PlotManager plots, WorldGuardHook worldGuard) {
        this.config = config;
        this.plots = plots;
        this.worldGuard = worldGuard;
    }

    public void add(Player actor, OfflinePlayer target) {
        Plot plot = requireCanInvite(actor);
        if (plot == null || target == null) {
            return;
        }
        UUID targetId = target.getUniqueId();
        if (targetId == null) {
            MessageUtil.send(actor, config.prefixed("player-not-found", "player", String.valueOf(target.getName())));
            return;
        }
        if (plot.isOwner(targetId)) {
            MessageUtil.send(actor, config.prefixed("cannot-target-owner"));
            return;
        }
        if (plot.isListed(targetId)) {
            MessageUtil.send(actor, config.prefixed("already-member", "player", name(target)));
            return;
        }
        if (plot.getMembers().size() >= config.maxMembers()) {
            MessageUtil.send(actor, config.prefixed("max-members", "max", String.valueOf(config.maxMembers())));
            return;
        }
        plot.putMember(targetId, PlotRole.VISITOR);
        worldGuard.syncMembers(plot);
        plots.persist(plot);
        MessageUtil.send(actor, config.prefixed("added", "player", name(target), "role", PlotRole.VISITOR.name()));
        Player online = Bukkit.getPlayer(targetId);
        if (online != null) {
            MessageUtil.send(online, config.prefixed(
                    "added-target",
                    "owner", name(Bukkit.getOfflinePlayer(plot.getOwner())),
                    "role", PlotRole.VISITOR.name()
            ));
        }
    }

    public void remove(Player actor, OfflinePlayer target) {
        Plot plot = requireCanInvite(actor);
        if (plot == null || target == null) {
            return;
        }
        UUID targetId = target.getUniqueId();
        if (targetId == null) {
            MessageUtil.send(actor, config.prefixed("player-not-found", "player", String.valueOf(target.getName())));
            return;
        }
        if (plot.isOwner(targetId)) {
            MessageUtil.send(actor, config.prefixed("cannot-target-owner"));
            return;
        }
        PlotRole existing = plot.getRole(targetId);
        if (existing == null) {
            MessageUtil.send(actor, config.prefixed("not-member", "player", name(target)));
            return;
        }
        if (existing == PlotRole.CO_OWNER && !plot.isOwner(actor.getUniqueId()) && !actor.hasPermission("frostplots.admin")) {
            MessageUtil.send(actor, config.prefixed("co-owner-restricted"));
            return;
        }
        plot.removeMember(targetId);
        worldGuard.syncMembers(plot);
        plots.persist(plot);
        MessageUtil.send(actor, config.prefixed("removed", "player", name(target)));
        Player online = Bukkit.getPlayer(targetId);
        if (online != null) {
            MessageUtil.send(online, config.prefixed(
                    "removed-target",
                    "owner", name(Bukkit.getOfflinePlayer(plot.getOwner()))
            ));
            ejectIfPresent(plot, online);
        }
    }

    public void promote(Player actor, OfflinePlayer target, String rawRole) {
        Plot plot = requireCanInvite(actor);
        if (plot == null || target == null) {
            return;
        }
        PlotRole role = PlotRole.fromString(rawRole);
        if (role == null) {
            MessageUtil.send(actor, config.prefixed("invalid-role"));
            return;
        }
        UUID targetId = target.getUniqueId();
        if (targetId == null) {
            MessageUtil.send(actor, config.prefixed("player-not-found", "player", String.valueOf(target.getName())));
            return;
        }
        if (plot.isOwner(targetId) || actor.getUniqueId().equals(targetId)) {
            MessageUtil.send(actor, config.prefixed(plot.isOwner(targetId) ? "cannot-target-owner" : "cannot-target-self"));
            return;
        }
        boolean actorIsOwner = plot.isOwner(actor.getUniqueId()) || actor.hasPermission("frostplots.admin");
        if (role == PlotRole.CO_OWNER && !actorIsOwner) {
            MessageUtil.send(actor, config.prefixed("co-owner-restricted"));
            return;
        }
        boolean wasListed = plot.isListed(targetId);
        if (!wasListed) {
            if (plot.getMembers().size() >= config.maxMembers()) {
                MessageUtil.send(actor, config.prefixed("max-members", "max", String.valueOf(config.maxMembers())));
                return;
            }
        }
        plot.putMember(targetId, role);
        worldGuard.syncMembers(plot);
        plots.persist(plot);
        String messageKey = wasListed ? "promoted" : "added";
        MessageUtil.send(actor, config.prefixed(messageKey, "player", name(target), "role", role.name()));
        Player online = Bukkit.getPlayer(targetId);
        if (online != null) {
            MessageUtil.send(online, config.prefixed(
                    "promoted-target",
                    "owner", name(Bukkit.getOfflinePlayer(plot.getOwner())),
                    "role", role.name()
            ));
        }
    }

    public boolean canInvite(Player player, Plot plot) {
        if (player == null || plot == null) {
            return false;
        }
        if (player.hasPermission("frostplots.admin") || plot.isOwner(player.getUniqueId())) {
            return true;
        }
        PlotRole role = plot.getRole(player.getUniqueId());
        return role != null && role.canInvite();
    }

    private Plot requireCanInvite(Player actor) {
        Plot standing = plots.at(actor.getLocation());
        if (standing != null && canInvite(actor, standing)) {
            return standing;
        }
        Plot owned = plots.byOwner(actor.getUniqueId());
        if (owned != null && canInvite(actor, owned)) {
            return owned;
        }
        if (owned == null && standing == null) {
            MessageUtil.send(actor, config.prefixed("no-plot"));
            return null;
        }
        MessageUtil.send(actor, config.prefixed("not-trusted"));
        return null;
    }

    private void ejectIfPresent(Plot plot, Player player) {
        if (!plot.isPlayerInside(player, plots.sizeOf(plot))) {
            return;
        }
        Location eject = plots.ejectLocation(plot.getCenterLocation() == null ? null : plot.getCenterLocation().getWorld());
        if (eject != null) {
            player.teleport(eject);
        }
    }

    private String name(OfflinePlayer player) {
        if (player == null) {
            return "Unknown";
        }
        String name = player.getName();
        return name == null ? "Unknown" : name;
    }
}
