package com.frostsmp.parkour.listener;

import com.frostsmp.parkour.config.PluginConfig;
import com.frostsmp.parkour.model.Course;
import com.frostsmp.parkour.service.CourseService;
import com.frostsmp.parkour.service.ParkourSessionService;
import com.frostsmp.parkour.session.ParkourSession;
import com.frostsmp.parkour.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

public final class ParkourPlayerListener implements Listener {

    private final PluginConfig config;
    private final CourseService courseService;
    private final ParkourSessionService sessionService;

    public ParkourPlayerListener(PluginConfig config, CourseService courseService, ParkourSessionService sessionService) {
        this.config = config;
        this.courseService = courseService;
        this.sessionService = sessionService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (sessionService.isInParkour(event.getPlayer().getUniqueId())) {
            sessionService.quitParkour(event.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (sessionService.isInParkour(player.getUniqueId())) {
            event.getDrops().clear();
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            sessionService.quitParkour(player, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && sessionService.isInParkour(player.getUniqueId())) {
            if (config.isPreventDamage()) {
                if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    event.setCancelled(true);
                    sessionService.handleFall(player);
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && sessionService.isInParkour(player.getUniqueId())) {
            if (config.isPreventHunger()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (sessionService.isInParkour(event.getPlayer().getUniqueId()) && config.isPreventBlockBreak()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (sessionService.isInParkour(event.getPlayer().getUniqueId()) && config.isPreventBlockPlace()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!sessionService.isInParkour(player.getUniqueId())) {
            return;
        }

        // Cancel parkour if flying/gliding is detected
        if (player.isFlying() || player.isGliding() || player.getAllowFlight()) {
            if (!player.hasPermission("frostsmp.parkour.admin")) {
                MessageUtil.send(player, config.msg("flight-detected"));
                sessionService.quitParkour(player, false);
                return;
            }
        }

        ParkourSession session = sessionService.getSession(player.getUniqueId());
        if (session == null) {
            return;
        }

        Course course = courseService.getCourse(session.getCourseId());
        if (course == null) {
            return;
        }

        if (config.isFallDetectionEnabled()) {
            Location to = event.getTo();
            if (to == null) {
                return;
            }

            // Custom course fall-y check
            if (course.getCustomFallY() != null && to.getY() <= course.getCustomFallY()) {
                sessionService.handleFall(player);
                return;
            }

            // Offset from last checkpoint
            Location cp = session.getLastCheckpointLocation();
            if (cp != null && to.getY() <= (cp.getY() - config.getFallYOffset())) {
                sessionService.handleFall(player);
                return;
            }

            // Void instant teleport
            if (config.isVoidInstantTeleport() && to.getY() <= -60) {
                sessionService.handleFall(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!sessionService.isInParkour(player.getUniqueId())) {
            return;
        }
        if (player.hasPermission("frostsmp.parkour.admin")) {
            return;
        }
        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);
        MessageUtil.send(player, config.msg("flight-detected"));
        sessionService.quitParkour(player, false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!sessionService.isInParkour(player.getUniqueId())) {
            return;
        }
        if (player.hasPermission("frostsmp.parkour.admin")) {
            return;
        }
        if (event.isGliding()) {
            event.setCancelled(true);
            MessageUtil.send(player, config.msg("flight-detected"));
            sessionService.quitParkour(player, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!sessionService.isInParkour(player.getUniqueId())) {
            return;
        }
        if (player.hasPermission("frostsmp.parkour.admin")) {
            return;
        }
        String msg = event.getMessage().toLowerCase();
        if (msg.startsWith("/parkour") || msg.startsWith("/frostparkour") || msg.startsWith("/pk")) {
            return;
        }
        event.setCancelled(true);
        MessageUtil.send(player, config.msg("command-blocked"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!sessionService.isInParkour(player.getUniqueId())) {
            return;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN || event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return;
        }
        event.setCancelled(true);
        MessageUtil.send(player, config.msg("teleport-blocked"));
    }
}
