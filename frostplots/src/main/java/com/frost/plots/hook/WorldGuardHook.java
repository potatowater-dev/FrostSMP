package com.frost.plots.hook;

import com.frost.plots.FrostPlotsPlugin;
import com.frost.plots.config.PluginConfig;
import com.frost.plots.model.Plot;
import com.frost.plots.model.PlotPrivacy;
import com.frost.plots.model.PlotRole;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.Association;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class WorldGuardHook {

    private final FrostPlotsPlugin plugin;
    private final PluginConfig config;

    public WorldGuardHook(FrostPlotsPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public RegionManager manager(World world) {
        if (world == null) {
            return null;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(world));
    }

    public void createOrUpdateRegion(Plot plot, int size) {
        World world = plot.getCenterLocation() == null ? null : plot.getCenterLocation().getWorld();
        RegionManager manager = manager(world);
        if (world == null || manager == null) {
            plugin.getLogger().warning("Cannot update WorldGuard region for plot #" + plot.getId() + ": world/manager missing.");
            return;
        }
        int minX = plot.minBlockX(size);
        int maxX = plot.maxBlockX(size);
        int minZ = plot.minBlockZ(size);
        int maxZ = plot.maxBlockZ(size);
        int minY = plot.getBaseY() - 1;
        int maxY = Math.min(world.getMaxHeight() - 1, plot.getBaseY() + config.regionHeight());

        BlockVector3 min = BlockVector3.at(minX, minY, minZ);
        BlockVector3 max = BlockVector3.at(maxX, maxY, maxZ);
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(plot.regionId(), min, max);
        ProtectedRegion existing = manager.getRegion(plot.regionId());
        if (existing != null) {
            region.setFlags(existing.getFlags());
        }
        region.setPriority(100);
        applyMembers(region, plot);
        applyPrivacyFlags(region, plot.getPrivacy());
        applyStayFlags(region);
        manager.addRegion(region);
        save(manager);
    }

    public void removeRegion(Plot plot) {
        Location center = plot.getCenterLocation();
        World world = center == null ? null : center.getWorld();
        RegionManager manager = manager(world);
        if (manager == null) {
            return;
        }
        manager.removeRegion(plot.regionId());
        save(manager);
    }

    public void syncMembers(Plot plot) {
        ProtectedRegion region = region(plot);
        if (region == null) {
            createOrUpdateRegion(plot, config.tier(plot.getTierIndex()).size());
            return;
        }
        applyMembers(region, plot);
        save(manager(plot.getCenterLocation() == null ? null : plot.getCenterLocation().getWorld()));
    }

    public void applyPrivacy(Plot plot) {
        ProtectedRegion region = region(plot);
        if (region == null) {
            createOrUpdateRegion(plot, config.tier(plot.getTierIndex()).size());
            return;
        }
        applyPrivacyFlags(region, plot.getPrivacy());
        applyStayFlags(region);
        save(manager(plot.getCenterLocation() == null ? null : plot.getCenterLocation().getWorld()));
    }

    public ProtectedRegion region(Plot plot) {
        Location center = plot.getCenterLocation();
        if (center == null) {
            return null;
        }
        RegionManager manager = manager(center.getWorld());
        if (manager == null) {
            return null;
        }
        return manager.getRegion(plot.regionId());
    }

    public Association association(Player player, Plot plot) {
        if (player == null || plot == null) {
            return Association.NON_MEMBER;
        }
        ProtectedRegion region = region(plot);
        if (region == null) {
            return plot.isOwner(player.getUniqueId())
                    ? Association.OWNER
                    : (plot.isListed(player.getUniqueId()) ? Association.MEMBER : Association.NON_MEMBER);
        }
        return WorldGuardPlugin.inst().wrapPlayer(player).getAssociation(List.of(region));
    }

    public boolean canBuildByAssociation(Player player, Plot plot) {
        return hasBuildAccess(player, plot);
    }

    public boolean canInteractByAssociation(Player player, Plot plot) {
        return hasContainerAccess(player, plot);
    }

    /** Owner / member / co-owner may build. Public does not grant build to visitors. */
    public boolean hasBuildAccess(Player player, Plot plot) {
        if (player == null || plot == null) {
            return false;
        }
        if (plot.isOwner(player.getUniqueId())) {
            return true;
        }
        PlotRole role = plot.getRole(player.getUniqueId());
        return role != null && role.canBuild(plot.getPrivacy());
    }

    /** Owner / member / co-owner may open containers (shulkers, chests, etc.). */
    public boolean hasContainerAccess(Player player, Plot plot) {
        if (player == null || plot == null) {
            return false;
        }
        if (plot.isOwner(player.getUniqueId())) {
            return true;
        }
        PlotRole role = plot.getRole(player.getUniqueId());
        return role != null && role.canInteract(plot.getPrivacy());
    }

    private void applyMembers(ProtectedRegion region, Plot plot) {
        region.getOwners().clear();
        region.getMembers().clear();
        if (plot.getOwner() != null) {
            region.getOwners().addPlayer(plot.getOwner());
        }
        for (Map.Entry<UUID, PlotRole> entry : plot.getMembers().entrySet()) {
            // Only real builders are WG members — visitors stay non-members for flags.
            if (entry.getValue() == PlotRole.MEMBER || entry.getValue() == PlotRole.CO_OWNER) {
                region.getMembers().addPlayer(entry.getKey());
            }
        }
    }

    private void applyPrivacyFlags(ProtectedRegion region, PlotPrivacy privacy) {
        // Build / containers: members+owners only on both public and private plots.
        // Public only changes ENTRY (anyone can visit).
        RegionGroup builders = RegionGroup.MEMBERS;
        if (privacy == PlotPrivacy.PRIVATE) {
            region.setFlag(Flags.ENTRY, StateFlag.State.DENY);
        } else {
            region.setFlag(Flags.ENTRY, StateFlag.State.ALLOW);
        }
        region.setFlag(Flags.BUILD, StateFlag.State.DENY);
        region.setFlag(Flags.PVP, StateFlag.State.DENY);
        denyNonMembers(region, Flags.INTERACT);
        denyNonMembers(region, Flags.USE);
        denyNonMembers(region, Flags.CHEST_ACCESS);
        region.setFlag(Flags.BLOCK_PLACE, StateFlag.State.ALLOW);
        region.setFlag(Flags.BLOCK_PLACE.getRegionGroupFlag(), builders);
        region.setFlag(Flags.BLOCK_BREAK, StateFlag.State.ALLOW);
        region.setFlag(Flags.BLOCK_BREAK.getRegionGroupFlag(), builders);
        applyBuildWhitelistBypass(region);
    }

    private void denyNonMembers(ProtectedRegion region, StateFlag flag) {
        region.setFlag(flag, StateFlag.State.DENY);
        region.setFlag(flag.getRegionGroupFlag(), RegionGroup.NON_MEMBERS);
    }

    private void applyBuildWhitelistBypass(ProtectedRegion region) {
        trySetFlag(region, "allow-place", "any");
        trySetFlag(region, "allow-blocks", "any");
        trySetFlag(region, "allow-break", "any");
        trySetFlag(region, "custom-block-place", "*");
        trySetFlag(region, "custom-block-break", "*");
        trySetFlag(region, "deny-place", "");
        trySetFlag(region, "deny-blocks", "");
        trySetFlag(region, "deny-break", "");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void trySetFlag(ProtectedRegion region, String name, String input) {
        Flag flag = WorldGuard.getInstance().getFlagRegistry().get(name);
        if (flag == null) {
            return;
        }
        try {
            if (input == null || input.isBlank()) {
                region.setFlag(flag, null);
                return;
            }
            Object value = flag.parseInput(FlagContext.create().setInput(input).build());
            region.setFlag(flag, value);
        } catch (Exception ignored) {
            // Flag type from another plugin; event un-cancel still allows building.
        }
    }

    private void applyStayFlags(ProtectedRegion region) {
        region.setFlag(Flags.EXIT, StateFlag.State.DENY);
        region.setFlag(Flags.EXIT.getRegionGroupFlag(), RegionGroup.ALL);
        region.setFlag(Flags.ENDERPEARL, StateFlag.State.DENY);
        region.setFlag(Flags.ENDERPEARL.getRegionGroupFlag(), RegionGroup.ALL);
        region.setFlag(Flags.CHORUS_TELEPORT, StateFlag.State.DENY);
        region.setFlag(Flags.CHORUS_TELEPORT.getRegionGroupFlag(), RegionGroup.ALL);
    }

    private void save(RegionManager manager) {
        if (manager == null) {
            return;
        }
        try {
            manager.saveChanges();
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to save WorldGuard regions", exception);
        }
    }
}
