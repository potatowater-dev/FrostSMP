package com.frost.plots.manager;

import com.frost.plots.FrostPlotsPlugin;
import com.frost.plots.config.PluginConfig;
import com.frost.plots.hook.SchematicHook;
import com.frost.plots.hook.WorldGuardHook;
import com.frost.plots.model.Plot;
import com.frost.plots.model.PlotPrivacy;
import com.frost.plots.model.UpgradeTier;
import com.frost.plots.storage.PlotDAO;
import com.frost.plots.util.GridUtil;
import com.frost.plots.util.MessageUtil;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlotManager {

    private final FrostPlotsPlugin plugin;
    private final PluginConfig config;
    private final PlotDAO dao;
    private final WorldGuardHook worldGuard;
    private final SchematicHook schematics;
    private final Map<Integer, Plot> plots = new ConcurrentHashMap<>();
    private final Set<Long> reservedSlots = ConcurrentHashMap.newKeySet();
    private final Object createLock = new Object();

    public PlotManager(
            FrostPlotsPlugin plugin,
            PluginConfig config,
            PlotDAO dao,
            WorldGuardHook worldGuard,
            SchematicHook schematics
    ) {
        this.plugin = plugin;
        this.config = config;
        this.dao = dao;
        this.worldGuard = worldGuard;
        this.schematics = schematics;
    }

    public void load(Collection<Plot> loaded) {
        plots.clear();
        if (loaded == null) {
            return;
        }
        for (Plot plot : loaded) {
            plots.put(plot.getId(), plot);
        }
        plugin.getLogger().info("Loaded " + plots.size() + " plot(s) into memory.");
    }

    public Collection<Plot> all() {
        return plots.values();
    }

    public Plot byId(int id) {
        return plots.get(id);
    }

    public Plot byOwner(UUID owner) {
        if (owner == null) {
            return null;
        }
        for (Plot plot : plots.values()) {
            if (plot.isOwner(owner)) {
                return plot;
            }
        }
        return null;
    }

    public Plot at(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        String world = location.getWorld().getName();
        for (Plot plot : plots.values()) {
            if (!plot.worldMatches(world)) {
                continue;
            }
            int size = config.tier(plot.getTierIndex()).size();
            if (plot.contains(location, size)) {
                return plot;
            }
        }
        return null;
    }

    public int sizeOf(Plot plot) {
        if (plot == null) {
            return 0;
        }
        return config.tier(plot.getTierIndex()).size();
    }

    public void persist(Plot plot) {
        dao.updateAsync(plot);
    }

    public void create(Player player) {
        if (player == null) {
            return;
        }
        if (byOwner(player.getUniqueId()) != null) {
            MessageUtil.send(player, config.prefixed("already-owns"));
            return;
        }
        World world = Bukkit.getWorld(config.plotWorld());
        if (world == null) {
            MessageUtil.send(player, config.prefixed("world-missing", "world", config.plotWorld()));
            return;
        }
        UpgradeTier starter = config.tier(0);
        MessageUtil.send(player, config.prefixed("creating"));

        final int[] slot;
        synchronized (createLock) {
            slot = nextFreeSlot();
            reservedSlots.add(GridUtil.pack(slot[0], slot[1]));
        }
        int centerX = slot[0] * config.gridSpacing();
        int centerZ = slot[1] * config.gridSpacing();
        long slotKey = GridUtil.pack(slot[0], slot[1]);
        int baseY = config.platformY();

        Plot draft = new Plot();
        draft.setOwner(player.getUniqueId());
        draft.setWorldName(world.getName());
        draft.setCenterX(centerX);
        draft.setCenterY(baseY);
        draft.setCenterZ(centerZ);
        draft.setTierIndex(0);
        draft.setPrivacy(PlotPrivacy.PUBLIC);

        UUID playerId = player.getUniqueId();
        dao.insertAsync(draft, saved -> {
            Player online = Bukkit.getPlayer(playerId);
            World plotWorld = Bukkit.getWorld(config.plotWorld());
            if (plotWorld == null) {
                reservedSlots.remove(slotKey);
                if (online != null) {
                    MessageUtil.send(online, config.prefixed("world-missing", "world", config.plotWorld()));
                }
                dao.deleteAsync(saved.getId(), null);
                return;
            }
            schematics.placeGrassPlatform(plotWorld, centerX, baseY, centerZ, starter.size());
            worldGuard.createOrUpdateRegion(saved, starter.size());
            plots.put(saved.getId(), saved);
            Location spawn = saved.getSpawnLocation();
            if (online != null && spawn != null) {
                online.teleport(spawn);
                MessageUtil.send(online, config.prefixed("created", "id", String.valueOf(saved.getId())));
            }
        }, error -> {
            reservedSlots.remove(slotKey);
            Player online = Bukkit.getPlayer(playerId);
            if (online != null) {
                MessageUtil.send(online, config.prefixed("create-failed"));
            }
        });
    }

    public void home(Player player) {
        Plot plot = byOwner(player.getUniqueId());
        if (plot == null) {
            MessageUtil.send(player, config.prefixed("no-plot"));
            return;
        }
        Location spawn = plot.getSpawnLocation();
        if (spawn == null) {
            MessageUtil.send(player, config.prefixed("world-missing", "world", plot.getWorldName()));
            return;
        }
        player.teleport(spawn);
        MessageUtil.send(player, config.prefixed("teleported-home"));
    }

    public void visit(Player visitor, OfflinePlayer target) {
        if (target == null || target.getUniqueId() == null) {
            MessageUtil.send(visitor, config.prefixed("player-not-found", "player", "unknown"));
            return;
        }
        Plot plot = byOwner(target.getUniqueId());
        if (plot == null) {
            MessageUtil.send(visitor, config.prefixed("visit-no-plot", "player", name(target)));
            return;
        }
        boolean admin = visitor.hasPermission("frostplots.admin");
        if (plot.getPrivacy() == PlotPrivacy.PRIVATE
                && !plot.isMemberOrOwner(visitor.getUniqueId())
                && !admin) {
            MessageUtil.send(visitor, config.prefixed("visit-private"));
            return;
        }
        Location spawn = plot.getSpawnLocation();
        if (spawn == null) {
            MessageUtil.send(visitor, config.prefixed("world-missing", "world", plot.getWorldName()));
            return;
        }
        visitor.teleport(spawn);
        MessageUtil.send(visitor, config.prefixed("teleported-visit", "owner", name(target)));
    }

    public void setSpawn(Player player) {
        Plot plot = ownedOrAdmin(player);
        if (plot == null) {
            return;
        }
        if (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("frostplots.admin")) {
            MessageUtil.send(player, config.prefixed("not-owner"));
            return;
        }
        if (!plot.contains(player.getLocation(), sizeOf(plot))) {
            MessageUtil.send(player, config.prefixed("spawn-off-plot"));
            return;
        }
        plot.setSpawn(player.getLocation());
        persist(plot);
        MessageUtil.send(player, config.prefixed("spawn-set"));
    }

    public void info(Player player) {
        Plot plot = at(player.getLocation());
        if (plot == null) {
            plot = byOwner(player.getUniqueId());
        }
        if (plot == null) {
            MessageUtil.send(player, config.prefixed("not-on-plot"));
            return;
        }
        int size = sizeOf(plot);
        MessageUtil.send(player, config.prefixed(
                "info-header",
                "id", String.valueOf(plot.getId()),
                "owner", name(Bukkit.getOfflinePlayer(plot.getOwner()))
        ));
        MessageUtil.send(player, config.prefixed(
                "info-body",
                "privacy", plot.getPrivacy().name(),
                "tier", String.valueOf(plot.getTierIndex() + 1),
                "size", String.valueOf(size)
        ));
        String members = plot.getMembers().isEmpty()
                ? "none"
                : plot.getMembers().entrySet().stream()
                .map(entry -> name(Bukkit.getOfflinePlayer(entry.getKey())) + " (" + entry.getValue().name() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
        MessageUtil.send(player, config.prefixed("info-members", "members", members));
    }

    public void list(Player player, int page) {
        List<Plot> visible = new ArrayList<>();
        for (Plot plot : plots.values()) {
            if (plot.isOwner(player.getUniqueId()) || plot.getPrivacy() == PlotPrivacy.PUBLIC) {
                visible.add(plot);
            }
        }
        visible.sort(Comparator.comparingInt(Plot::getId));
        if (visible.isEmpty()) {
            MessageUtil.send(player, config.prefixed("list-empty"));
            return;
        }
        int pageSize = config.listPageSize();
        int pages = Math.max(1, (int) Math.ceil(visible.size() / (double) pageSize));
        int safePage = Math.min(Math.max(1, page), pages);
        if (page > pages) {
            MessageUtil.send(player, config.prefixed("list-page"));
            return;
        }
        MessageUtil.send(player, config.prefixed(
                "list-header",
                "page", String.valueOf(safePage),
                "pages", String.valueOf(pages)
        ));
        int from = (safePage - 1) * pageSize;
        int to = Math.min(from + pageSize, visible.size());
        for (int i = from; i < to; i++) {
            Plot plot = visible.get(i);
            int size = sizeOf(plot);
            MessageUtil.send(player, config.prefixed(
                    "list-entry",
                    "id", String.valueOf(plot.getId()),
                    "owner", name(Bukkit.getOfflinePlayer(plot.getOwner())),
                    "privacy", plot.getPrivacy().name(),
                    "size", String.valueOf(size)
            ));
        }
    }

    public void delete(Plot plot, Player actor) {
        if (plot == null) {
            return;
        }
        World world = plot.getCenterLocation() == null ? null : plot.getCenterLocation().getWorld();
        int size = sizeOf(plot);
        if (world != null) {
            int minY = plot.getBaseY() - 1;
            int maxY = Math.min(world.getMaxHeight() - 1, plot.getBaseY() + config.regionHeight());
            schematics.clearCuboid(
                    world,
                    BlockVector3.at(plot.minBlockX(size), minY, plot.minBlockZ(size)),
                    BlockVector3.at(plot.maxBlockX(size), maxY, plot.maxBlockZ(size))
            );
        }
        Location eject = ejectLocation(world);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (plot.isPlayerInside(online, size) && eject != null) {
                online.teleport(eject);
            }
        }
        worldGuard.removeRegion(plot);
        plots.remove(plot.getId());
        int id = plot.getId();
        dao.deleteAsync(id, () -> {
            if (actor != null && actor.isOnline()) {
                MessageUtil.send(actor, config.prefixed("deleted", "id", String.valueOf(id)));
            }
        });
    }

    public void expandPlatform(Plot plot, UpgradeTier tier) {
        Location center = plot.getCenterLocation();
        if (center == null || center.getWorld() == null) {
            return;
        }
        World world = center.getWorld();
        schematics.placeGrassPlatform(world, plot.getCenterBlockX(), plot.getBaseY(), plot.getCenterBlockZ(), tier.size());
        worldGuard.createOrUpdateRegion(plot, tier.size());
    }

    public Location ejectLocation(World world) {
        if (world != null) {
            Location spawn = world.getSpawnLocation();
            if (spawn != null) {
                return spawn;
            }
        }
        World fallback = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().getFirst();
        return fallback == null ? null : fallback.getSpawnLocation();
    }

    public Plot requireOwned(Player player) {
        Plot plot = byOwner(player.getUniqueId());
        if (plot == null) {
            MessageUtil.send(player, config.prefixed("no-plot"));
        }
        return plot;
    }

    public Plot ownedOrAdmin(Player player) {
        Plot standing = at(player.getLocation());
        if (standing != null && (standing.isOwner(player.getUniqueId()) || player.hasPermission("frostplots.admin"))) {
            return standing;
        }
        return requireOwned(player);
    }

    public Plot requireOwnerForManagement(Player player) {
        Plot plot = ownedOrAdmin(player);
        if (plot == null) {
            return null;
        }
        if (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("frostplots.admin")) {
            MessageUtil.send(player, config.prefixed("not-owner"));
            return null;
        }
        return plot;
    }

    private int[] nextFreeSlot() {
        var occupied = ConcurrentHashMap.<Long>newKeySet();
        occupied.addAll(reservedSlots);
        for (Plot plot : plots.values()) {
            int gx = (int) Math.round(plot.getCenterX() / (double) config.gridSpacing());
            int gz = (int) Math.round(plot.getCenterZ() / (double) config.gridSpacing());
            occupied.add(GridUtil.pack(gx, gz));
        }
        int index = 0;
        while (true) {
            int[] offset = GridUtil.indexToOffset(index);
            if (occupied.add(GridUtil.pack(offset[0], offset[1]))) {
                return offset;
            }
            index++;
            if (index > 1_000_000) {
                throw new IllegalStateException("Could not find a free plot slot");
            }
        }
    }

    private String name(OfflinePlayer player) {
        if (player == null) {
            return "Unknown";
        }
        String name = player.getName();
        return name == null ? player.getUniqueId().toString() : name;
    }
}
