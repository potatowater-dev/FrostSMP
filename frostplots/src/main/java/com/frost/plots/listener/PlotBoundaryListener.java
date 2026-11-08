package com.frost.plots.listener;

import com.frost.plots.config.PluginConfig;
import com.frost.plots.manager.PlotManager;
import com.frost.plots.model.Plot;
import com.frost.plots.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlotBoundaryListener implements Listener {

    private static final long MESSAGE_COOLDOWN_MS = 2500L;

    private final PluginConfig config;
    private final PlotManager plots;
    private final Map<UUID, Integer> lastPlot = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMessage = new ConcurrentHashMap<>();
    private NamespacedKey boundPlotKey;

    public PlotBoundaryListener(PluginConfig config, PlotManager plots) {
        this.config = config;
        this.plots = plots;
    }

    public void start(Plugin plugin) {
        boundPlotKey = new NamespacedKey(plugin, "bound_plot");
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::enforce, 1L, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (boundPlotKey == null) {
            return;
        }
        Item item = event.getEntity();
        Plot plot = plotAt(item.getLocation());
        if (plot == null) {
            return;
        }
        item.getPersistentDataContainer().set(boundPlotKey, PersistentDataType.INTEGER, plot.getId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || sameBlock(from, to)) {
            return;
        }
        Player player = event.getPlayer();
        if (bypass(player)) {
            remember(player, to);
            return;
        }
        Plot origin = plotAt(from);
        if (origin == null) {
            Plot previous = trackedPlot(player);
            if (previous != null && stillInPlotWorld(player, previous) && !inside(previous, to)) {
                snapMove(event, player, previous);
                return;
            }
            remember(player, to);
            return;
        }
        lastPlot.put(player.getUniqueId(), origin.getId());
        if (inside(origin, to)) {
            return;
        }
        snapMove(event, player, origin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (bypass(player)) {
            remember(player, event.getTo());
            return;
        }
        Plot origin = plotAt(event.getFrom());
        if (origin == null) {
            origin = trackedPlot(player);
        }
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        boolean commanded = cause == PlayerTeleportEvent.TeleportCause.PLUGIN
                || cause == PlayerTeleportEvent.TeleportCause.COMMAND
                || cause == PlayerTeleportEvent.TeleportCause.SPECTATE;
        if (commanded) {
            Plot destination = plotAt(event.getTo());
            if (destination != null) {
                lastPlot.put(player.getUniqueId(), destination.getId());
            } else {
                lastPlot.remove(player.getUniqueId());
            }
            return;
        }
        if (origin == null) {
            remember(player, event.getTo());
            return;
        }
        lastPlot.put(player.getUniqueId(), origin.getId());
        Location to = event.getTo();
        if (to != null && inside(origin, to)) {
            return;
        }
        event.setCancelled(true);
        notifyStay(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onVehicleMove(VehicleMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (sameBlock(from, to)) {
            return;
        }
        Plot origin = plotAt(from);
        if (origin == null || inside(origin, to)) {
            return;
        }
        boolean snapped = false;
        for (Entity passenger : event.getVehicle().getPassengers()) {
            if (passenger instanceof Player player && !bypass(player)) {
                snapped = true;
                lastPlot.put(player.getUniqueId(), origin.getId());
                notifyStay(player);
            }
        }
        if (snapped) {
            event.getVehicle().teleport(from, PlayerTeleportEvent.TeleportCause.PLUGIN);
            event.getVehicle().setVelocity(new Vector());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVoid(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        Plot plot = plotAt(player.getLocation());
        if (plot == null) {
            plot = trackedPlot(player);
            if (plot == null) {
                plot = plots.byOwner(player.getUniqueId());
            }
        }
        if (plot == null) {
            return;
        }
        Location spawn = plot.getSpawnLocation();
        if (spawn == null) {
            return;
        }
        event.setCancelled(true);
        player.setFallDistance(0f);
        player.setVelocity(new Vector());
        player.teleport(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastPlot.remove(id);
        lastMessage.remove(id);
    }

    public void enforce() {
        enforcePlayers();
        enforceItems();
    }

    private void enforcePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (bypass(player)) {
                remember(player, player.getLocation());
                continue;
            }
            Plot here = plotAt(player.getLocation());
            if (here != null) {
                lastPlot.put(player.getUniqueId(), here.getId());
                continue;
            }
            Plot previous = trackedPlot(player);
            if (previous == null || !stillInPlotWorld(player, previous)) {
                continue;
            }
            Location spawn = previous.getSpawnLocation();
            if (spawn == null) {
                continue;
            }
            player.setVelocity(new Vector());
            player.setFallDistance(0f);
            player.teleport(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
            notifyStay(player);
        }
    }

    private void enforceItems() {
        if (boundPlotKey == null) {
            return;
        }
        World world = Bukkit.getWorld(config.plotWorld());
        if (world == null) {
            return;
        }
        for (Item item : world.getEntitiesByClass(Item.class)) {
            Plot here = plotAt(item.getLocation());
            if (here != null) {
                item.getPersistentDataContainer().set(boundPlotKey, PersistentDataType.INTEGER, here.getId());
                continue;
            }
            Integer boundId = item.getPersistentDataContainer().get(boundPlotKey, PersistentDataType.INTEGER);
            if (boundId == null) {
                continue;
            }
            Plot plot = plots.byId(boundId);
            if (plot == null || !plot.worldMatches(world.getName())) {
                item.getPersistentDataContainer().remove(boundPlotKey);
                continue;
            }
            snapItem(item, plot);
        }
    }

    private void snapItem(Item item, Plot plot) {
        int size = plots.sizeOf(plot);
        int minX = plot.minBlockX(size);
        int maxX = plot.maxBlockX(size);
        int minZ = plot.minBlockZ(size);
        int maxZ = plot.maxBlockZ(size);
        int minY = plot.getBaseY();
        int maxY = plot.getBaseY() + config.regionHeight();

        Location loc = item.getLocation();
        double x = clamp(loc.getX(), minX + 0.3, maxX + 0.7);
        double z = clamp(loc.getZ(), minZ + 0.3, maxZ + 0.7);
        double y = loc.getY();
        if (y < minY) {
            y = minY + 1.0;
        } else if (y > maxY) {
            y = maxY - 0.5;
        }
        item.setVelocity(new Vector());
        item.teleport(new Location(loc.getWorld(), x, y, z));
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    private void snapMove(PlayerMoveEvent event, Player player, Plot plot) {
        Location to = event.getTo();
        Location from = event.getFrom();
        player.setVelocity(new Vector());
        player.setFallDistance(0f);
        if ((to != null && to.getBlockY() < plot.getBaseY()) || !inside(plot, from)) {
            Location spawn = plot.getSpawnLocation();
            if (spawn != null) {
                event.setTo(spawn);
                player.teleport(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
            notifyStay(player);
            return;
        }
        Location stay = from.clone();
        if (to != null) {
            stay.setYaw(to.getYaw());
            stay.setPitch(to.getPitch());
        }
        event.setTo(stay);
        notifyStay(player);
    }

    private void remember(Player player, Location location) {
        Plot plot = plotAt(location);
        if (plot != null) {
            lastPlot.put(player.getUniqueId(), plot.getId());
        }
    }

    private Plot trackedPlot(Player player) {
        Integer id = lastPlot.get(player.getUniqueId());
        return id == null ? null : plots.byId(id);
    }

    private boolean stillInPlotWorld(Player player, Plot plot) {
        return player.getWorld() != null && plot.worldMatches(player.getWorld().getName());
    }

    private Plot plotAt(Location location) {
        return plots.at(location);
    }

    private boolean inside(Plot plot, Location location) {
        int size = plots.sizeOf(plot);
        int minY = plot.getBaseY() - 1;
        int maxY = plot.getBaseY() + config.regionHeight();
        return plot.containsVolume(location, size, minY, maxY);
    }

    private boolean bypass(Player player) {
        return config.allowBoundaryBypass() && player.hasPermission("frostplots.bypass-boundary");
    }

    private boolean sameBlock(Location from, Location to) {
        return from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ();
    }

    private void notifyStay(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastMessage.get(player.getUniqueId());
        if (last != null && now - last < MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastMessage.put(player.getUniqueId(), now);
        MessageUtil.send(player, config.prefixed("cannot-leave"));
    }
}
